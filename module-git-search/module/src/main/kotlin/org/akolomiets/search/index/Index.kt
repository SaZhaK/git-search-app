package org.akolomiets.search.index

import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.nio.file.Path

/**
 * Class for representation of Apache Lucene full-text index.
 * Consists of line and file index.
 *
 * Line index contains information about separate lines from all files.
 * Indexed documents contain next fields:
 * - full path to line resolved from containing repository
 * - line content
 * - line number
 * - comma-separated list of branches, on which the same file contains this line in an unaltered way
 * This index is used for finding user-specified search.
 *
 * File index contains additional information about files.
 * Indexed documents contain:
 * - email of author of last changes to this document
 * - date of last changes of this document
 * - java package ('NO_PACKAGE' for non-java/kotlin files)
 * - repository type (development/implementation)
 * - Git branch
 * - repository name
 * This index is used for addition of filters to user-specified search.
 *
 * @author akolomiets
 * @since 1.0.0
 */
class Index(
    indexDir: String
) {
    val lineIndex: Directory
    val fileIndex: Directory

    init {
        lineIndex = FSDirectory.open(Path.of("$indexDir/$LINES_DIR"))
        fileIndex = FSDirectory.open(Path.of("$indexDir/$FILES_DIR"))
    }

    fun close() {
        lineIndex.close()
        fileIndex.close()
    }

    companion object {
        const val LINES_DIR = "lines"
        const val FILES_DIR = "files"

        const val PATH = "path"

        const val CONTENT = "content"
        const val LINE_NUMBER = "lineNumber"
        const val BRANCHES = "branches"

        const val AUTHOR_EMAIL = "authorEmail"
        const val DATE = "date"
        const val PACKAGE = "package"
        const val TYPE = "type"
        const val BRANCH = "branch"
        const val REPOSITORY = "repository"
    }
}