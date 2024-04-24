package org.akolomiets.search.index.service

import org.akolomiets.search.dto.LineDto
import org.akolomiets.search.dto.SearchRequestDto
import org.akolomiets.search.dto.SnippetDto

/**
 * Service for performing user-specified search in Apache Lucene index.
 *
 * @author akolomiets
 * @since 1.0.0
 */
interface SearchService {

    /**
     * Performs search for exact matching substrings **case-insensitive** for the given search request.
     * Applies filter values provided in request.
     * The amount of found results is limited by [SEARCH_RESULTS_LIMIT] value.
     *
     * For example: search 'Test' will return lines that contain '@Test', 'TestService', 'test'
     *
     * @param searchRequest search request containing user-specified search and filter values
     *
     * @return list of matching lines
     */
    fun exactSearch(searchRequest: org.akolomiets.search.dto.SearchRequestDto): List<org.akolomiets.search.dto.LineDto>

    /**
     * Performs fuzzy search based on bigrams for the given search request.
     * Applies filter values provided in request.
     * The amount of found results is limited by [SEARCH_RESULTS_LIMIT] value.
     *
     * For example: search 'TestService' will return lines that contain 'TestService', 'TextService', 'TestSuperService'
     *
     * @param searchRequest search request containing user-specified search and filter values
     *
     * @return list of matching lines
     */
    fun fuzzySearch(searchRequest: org.akolomiets.search.dto.SearchRequestDto): List<org.akolomiets.search.dto.LineDto>

    /**
     * Performs search of a code snippet that frames given line.
     *
     * Returns [SNIPPET_N] lines before the given line, the line and [SNIPPET_N] lines after the given line.
     * If the line is the first one in a file returns the given line and (2 * [SNIPPET_N]) lines after it.
     * The same logic applies if the line is the last one in a file.
     * If a file has less than (2 * [SNIPPET_N] + 1) lines returns all of them.
     *
     * @param filePath path to file resolved from repository
     * @param lineNumber the number of central line
     * @param branch name of branch that contains this file
     *
     * @return list of lines that form the code snippet
     */
    fun searchSnippet(filePath: String, lineNumber: Int, branch: String): org.akolomiets.search.dto.SnippetDto

    companion object {
        /**
         * The limit number of search results per request
         */
        const val SEARCH_RESULTS_LIMIT = 100

        /**
         * Number of lines before and after the selected line in code snippet (if possible)
         */
        const val SNIPPET_N = 3
    }
}