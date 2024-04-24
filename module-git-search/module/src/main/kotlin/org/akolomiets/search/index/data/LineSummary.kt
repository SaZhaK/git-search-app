package org.akolomiets.search.index.data

import java.io.Serializable

/**
 * @author akolomiets
 * @since 1.0.0
 */
data class LineSummary(
    val path: String,
    val content: String,
    val number: Int
) : Serializable