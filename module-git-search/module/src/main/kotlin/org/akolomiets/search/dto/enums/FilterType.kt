package org.akolomiets.search.dto.enums

/**
 * Types of search filters.
 *
 * @author akolomiets
 * @since 1.0.0
 */
enum class FilterType {

    /**
     * File extension filter.
     */
    EXTENSION,

    /**
     * Java/Kotlin package filter
     */
    PACKAGE,

    /**
     * File mask filter.
     */
    FILE_MASK,

    /**
     * Directory name filter.
     */
    DIRECTORY,

    /**
     * Repository name filter.
     */
    REPOSITORY,

    /**
     * Repository type filter (development / implementation).
     */
    REPOSITORY_TYPE
}