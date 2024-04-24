package org.akolomiets.search.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author akolomiets
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class LineDto @JsonCreator constructor(
    @param:JsonProperty("content") val content: String? = null,
    @param:JsonProperty("path") val path: String? = null,
    @param:JsonProperty("number") val number: Int? = null,
    @param:JsonProperty("branch") val branch: String? = null
)