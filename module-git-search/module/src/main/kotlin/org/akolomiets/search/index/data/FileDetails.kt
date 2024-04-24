package org.akolomiets.search.index.data

import java.io.Serializable

/**
 * @author akolomiets
 * @since 1.0.0
 */
data class FileDetails(
    val authorEmail: String,
    val date: String,
    val javaPackage: String,
    val path: String,
    val type: String,
    val branch: String,
    val repository: String,
) : Serializable