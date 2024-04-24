package org.akolomiets.search.index.service.impl

import org.akolomiets.search.dto.LineDto
import org.akolomiets.search.dto.SearchRequestDto
import org.akolomiets.search.dto.SnippetDto
import org.akolomiets.search.dto.enums.FilterType
import org.akolomiets.search.index.Index
import org.akolomiets.search.index.Index.Companion.AUTHOR_EMAIL
import org.akolomiets.search.index.Index.Companion.BRANCH
import org.akolomiets.search.index.Index.Companion.BRANCHES
import org.akolomiets.search.index.Index.Companion.CONTENT
import org.akolomiets.search.index.Index.Companion.DATE
import org.akolomiets.search.index.Index.Companion.LINE_NUMBER
import org.akolomiets.search.index.Index.Companion.PACKAGE
import org.akolomiets.search.index.Index.Companion.PATH
import org.akolomiets.search.index.Index.Companion.REPOSITORY
import org.akolomiets.search.index.Index.Companion.TYPE
import org.akolomiets.search.index.IndexProvider
import org.akolomiets.search.index.analyzer.CodeAnalyzer
import org.akolomiets.search.index.service.SearchService
import org.akolomiets.search.index.service.SearchService.Companion.SEARCH_RESULTS_LIMIT
import org.akolomiets.search.index.service.SearchService.Companion.SNIPPET_N
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.springframework.stereotype.Component
import kotlin.math.max
import kotlin.math.min

/**
 * @author akolomiets
 * @since 1.0.0
 */
@Component("search.searchService")
class SearchServiceImpl constructor(
    private val indexProvider: IndexProvider
) : SearchService {

    override fun exactSearch(searchRequest: org.akolomiets.search.dto.SearchRequestDto): List<org.akolomiets.search.dto.LineDto> {
        val searchString = QueryParser.escape(searchRequest.searchString.lowercase())
        val queryString = "${CONTENT}:\"${searchString}\""

        val searchQuery = QueryParser(CONTENT, CodeAnalyzer()).parse(queryString)

        return executeQueryWithFilters(searchRequest, searchQuery)
    }

    override fun fuzzySearch(searchRequest: org.akolomiets.search.dto.SearchRequestDto): List<org.akolomiets.search.dto.LineDto> {
        val searchString = searchRequest.searchString
        val nGrams = searchString.chunked(2)

        val queryString =
            nGrams.joinToString(prefix = "$CONTENT:(", separator = " AND ", postfix = ")") { word ->
                ComplexPhraseQueryParser.escape(word).lowercase()
            }
        val searchQuery = ComplexPhraseQueryParser(CONTENT, CodeAnalyzer()).parse(queryString)

        return executeQueryWithFilters(searchRequest, searchQuery)
    }

    override fun searchSnippet(filePath: String, lineNumber: Int, branch: String): org.akolomiets.search.dto.SnippetDto {
        val accessibleIndex = indexProvider.getAccessibleIndex()
        val accessibleLineIndex = accessibleIndex.lineIndex
        val accessibleFileIndex = accessibleIndex.fileIndex

        val filePathTerm = Term(PATH, filePath)
        val filePathQuery = TermQuery(filePathTerm)

        val branchTerm = Term(BRANCHES, branch)
        val branchQuery = TermQuery(branchTerm)

        val bufferSize = 2 * SNIPPET_N
        val lineQueryBuilder = BooleanQuery.Builder()
        for (i in (lineNumber - bufferSize)..(lineNumber + bufferSize)) {
            val lineTerm = Term(LINE_NUMBER, i.toString())
            val lineQuery = TermQuery(lineTerm)
            lineQueryBuilder.add(lineQuery, BooleanClause.Occur.SHOULD)
        }
        val lineQuery = lineQueryBuilder.build()

        val snippetQuery = BooleanQuery.Builder()
            .add(filePathQuery, BooleanClause.Occur.MUST)
            .add(branchQuery, BooleanClause.Occur.MUST)
            .add(lineQuery, BooleanClause.Occur.MUST)
            .build()

        val searchLimit = 2 * (2 * SNIPPET_N) + 1
        val documents = executeQuery(accessibleLineIndex, snippetQuery, searchLimit)
            .sortedBy { it.getField(LINE_NUMBER).stringValue().toInt() }

        var selectedRowIdx = 0
        documents.forEachIndexed { index, document ->
            if (document.getField(LINE_NUMBER).stringValue().toInt() == lineNumber) {
                selectedRowIdx = index
            }
        }
        val totalLinesBefore = selectedRowIdx
        val totalLinesAfter = documents.size - 1 - selectedRowIdx

        val additionalLinesBefore = SNIPPET_N - min(totalLinesAfter, SNIPPET_N)
        val additionalLinesAfter = SNIPPET_N - min(totalLinesBefore, SNIPPET_N)

        val startIdx = max(selectedRowIdx - SNIPPET_N - additionalLinesBefore, 0)
        val endIdx = min(selectedRowIdx + SNIPPET_N + additionalLinesAfter, documents.size - 1)

        val snippetLines = documents.subList(startIdx, endIdx + 1).map { documentToLine(it) }

        val metaInfoQuery = BooleanQuery.Builder()
            .add(filePathQuery, BooleanClause.Occur.MUST)
            .add(TermQuery(Term(BRANCH, branch)), BooleanClause.Occur.MUST)
            .build()
        val metaInfoDocuments = executeQuery(accessibleFileIndex, metaInfoQuery, 1)
        val metaInfo = metaInfoDocuments.lastOrNull()
        val authorEmail = metaInfo?.getField(AUTHOR_EMAIL)?.stringValue() ?: UNKNOWN_INFO
        val date = metaInfo?.getField(DATE)?.stringValue() ?: UNKNOWN_INFO
        val repository = metaInfo?.getField(REPOSITORY)?.stringValue() ?: UNKNOWN_INFO
        val path = metaInfo?.getField(PATH)?.stringValue() ?: UNKNOWN_INFO
//        val link = "https://$git/$repository/-/blob/$branch/${path.removePrefix("$repository/")}#L$lineNumber"
        val link = "link"

        return org.akolomiets.search.dto.SnippetDto(snippetLines, authorEmail, date, branch, link)
    }

    private fun executeQueryWithFilters(searchRequest: org.akolomiets.search.dto.SearchRequestDto, searchQuery: Query): List<org.akolomiets.search.dto.LineDto> {
        val accessibleIndex = indexProvider.getAccessibleIndex()

        val queryWithRegularFilters = applyRegularFilters(accessibleIndex, searchQuery, searchRequest)
        val query =
            searchRequest.branch?.let { applyBranchFilter(queryWithRegularFilters, it) } ?: queryWithRegularFilters

        val documents = executeQuery(accessibleIndex.lineIndex, query, SEARCH_RESULTS_LIMIT)
        return documents.map { documentToLine(it) }
    }

    private fun applyRegularFilters(index: Index, searchQuery: Query, searchRequest: org.akolomiets.search.dto.SearchRequestDto): Query {
        val fileIndex = index.fileIndex
        val noFiltersPresent = searchRequest.filters.isEmpty()
        return if (noFiltersPresent) {
            searchQuery
        } else {
            val filtersQueryBuilder = BooleanQuery.Builder()
            for (filter in searchRequest.filters) {
                val query = applyRegularFilter(filter.type, filter.value)
                filtersQueryBuilder.add(query, BooleanClause.Occur.MUST)
            }
            val filtersQuery = filtersQueryBuilder.build()

            val filteredDocuments = executeQuery(fileIndex, filtersQuery, Integer.MAX_VALUE)
            val allowedPaths = filteredDocuments.map { it.getField(PATH).stringValue() }

            val allowedPathsQueryBuilder = BooleanQuery.Builder()
            for (allowedPath in allowedPaths) {
                val pathTerm = Term(PATH, allowedPath)
                val pathQuery = TermQuery(pathTerm)
                allowedPathsQueryBuilder.add(pathQuery, BooleanClause.Occur.SHOULD)
            }
            val allowedPathsQuery = allowedPathsQueryBuilder.build()

            val queryWithFiltersBuilder = BooleanQuery.Builder()
            queryWithFiltersBuilder.add(searchQuery, BooleanClause.Occur.MUST)
            queryWithFiltersBuilder.add(allowedPathsQuery, BooleanClause.Occur.MUST)
            queryWithFiltersBuilder.build()
        }
    }

    private fun applyRegularFilter(filterType: org.akolomiets.search.dto.enums.FilterType, filterValue: String): Query {
        return when (filterType) {
            org.akolomiets.search.dto.enums.FilterType.EXTENSION -> {
                val extensionTerm = Term(PATH, "*.${filterValue.lowercase()}")
                WildcardQuery(extensionTerm)
            }
            org.akolomiets.search.dto.enums.FilterType.PACKAGE -> {
                val packageTerm = Term(PACKAGE, filterValue.lowercase())
                WildcardQuery(packageTerm)
            }
            org.akolomiets.search.dto.enums.FilterType.FILE_MASK -> {
                val fileMaskTerm = Term(PATH, "*${filterValue}.*")
                WildcardQuery(fileMaskTerm)
            }
            org.akolomiets.search.dto.enums.FilterType.DIRECTORY -> {
                val directoryTerm = Term(PATH, "*${filterValue.lowercase()}/*")
                WildcardQuery(directoryTerm)
            }
            org.akolomiets.search.dto.enums.FilterType.REPOSITORY -> {
                val repositoryTerm = Term(REPOSITORY, "*${filterValue.lowercase()}*")
                WildcardQuery(repositoryTerm)
            }
            org.akolomiets.search.dto.enums.FilterType.REPOSITORY_TYPE -> {
                val repositoryTypeTerm = Term(TYPE, filterValue.lowercase())
                TermQuery(repositoryTypeTerm)
            }
        }
    }

    private fun applyBranchFilter(searchQuery: Query, branch: String): Query {
        val queryBuilder = BooleanQuery.Builder()
        val branchTerm = Term(BRANCHES, branch)
        queryBuilder.add(TermQuery(branchTerm), BooleanClause.Occur.MUST)
        queryBuilder.add(searchQuery, BooleanClause.Occur.MUST)
        return queryBuilder.build()
    }

    private fun executeQuery(index: Directory, query: Query, resultsLimit: Int): List<Document> {
        val indexReader = DirectoryReader.open(index)
        val searcher = IndexSearcher(indexReader)

        val topDocs = searcher.search(query, resultsLimit)

        return topDocs.scoreDocs
            .map { searcher.doc(it.doc) }
            .toList()
    }

    private fun documentToLine(document: Document): org.akolomiets.search.dto.LineDto {
        val content = document.getField(CONTENT).stringValue()
        val path = document.getField(PATH).stringValue()
        val lineNumber = document.getField(LINE_NUMBER).stringValue().toInt()
        val branches = document.getField(BRANCHES).stringValue()
        val branch = selectPriorityBranch(branches)
        return org.akolomiets.search.dto.LineDto(content, path, lineNumber, branch)
    }

    // TODO test
    private fun selectPriorityBranch(branches: String): String {
        val branchesAsList = branches.split(" ").toList()
        return if (branchesAsList.contains("master")) {
            "master"
        } else if (branchesAsList.contains("*-release")) {
            val releaseBranches = branchesAsList.filter { it.contains("-release") }.sorted()
            releaseBranches.first()
        } else if (branchesAsList.contains("*-patch")) {
            val patchBranches = branchesAsList.filter { it.contains("-patch") }.sorted()
            patchBranches.first()
        } else {
            // TODO dev etc.?
            val sortedIssueBranches = branchesAsList.sorted()
            sortedIssueBranches.first()
        }
    }

    companion object {
        const val UNKNOWN_INFO = "UNKNOWN"
    }
}