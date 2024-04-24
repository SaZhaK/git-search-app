package org.akolomiets.search.index

import org.akolomiets.search.index.data.FileDetails
import org.akolomiets.search.index.data.LineSummary
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.FieldType
import org.apache.lucene.index.IndexOptions
import org.springframework.stereotype.Service

/**
 * Class for creating Apache Lucene [Document] instances.
 *
 * @author akolomiets
 * @since 1.0.0
 */
@Service("search.documentProvider")
class DocumentProvider {

    private val tokenizedFieldType = prepareFieldType(true)
    private val notTokenizedFieldType = prepareFieldType(false)

    /**
     * Creates document containing information about separate lines.
     *
     * @param lineSummary information about line
     * @param branches comma-separated list of branches,
     * on which the same file contains this line in an unaltered way
     *
     * @return created document
     */
    fun lineInfoToDocument(lineSummary: LineSummary, branches: String): Document {
        val document = Document()
        document.add(Field(Index.CONTENT, lineSummary.content, tokenizedFieldType))
        document.add(Field(Index.PATH, lineSummary.path, notTokenizedFieldType))
        document.add(Field(Index.LINE_NUMBER, lineSummary.number.toString(), notTokenizedFieldType))
        document.add(Field(Index.BRANCHES, branches, tokenizedFieldType))
        return document
    }

    /**
     * Creates document containing additional information about files.
     *
     * @param fileDetails information about file
     *
     * @return created document
     */
    fun fileInfoToDocument(fileDetails: FileDetails): Document {
        val document = Document()
        document.add(Field(Index.AUTHOR_EMAIL, fileDetails.authorEmail, notTokenizedFieldType))
        document.add(Field(Index.DATE, fileDetails.date, notTokenizedFieldType))
        document.add(Field(Index.PACKAGE, fileDetails.javaPackage, notTokenizedFieldType))
        document.add(Field(Index.PATH, fileDetails.path, notTokenizedFieldType))
        document.add(Field(Index.TYPE, fileDetails.type, notTokenizedFieldType))
        document.add(Field(Index.BRANCH, fileDetails.branch, notTokenizedFieldType))
        document.add(Field(Index.REPOSITORY, fileDetails.repository, notTokenizedFieldType))
        return document
    }

    private fun prepareFieldType(tokenized: Boolean): FieldType {
        val tokenizedFieldType = FieldType()
        tokenizedFieldType.setTokenized(tokenized)
        tokenizedFieldType.setStored(true)
        tokenizedFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)
        tokenizedFieldType.freeze()
        return tokenizedFieldType
    }
}