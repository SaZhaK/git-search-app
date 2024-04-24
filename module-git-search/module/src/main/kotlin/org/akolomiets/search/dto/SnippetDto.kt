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
class SnippetDto @JsonCreator constructor(
    @param:JsonProperty("snippet") val snippet: List<org.akolomiets.search.dto.LineDto>? = null,
    @param:JsonProperty("authorEmail") val authorEmail: String? = null,
    @param:JsonProperty("date") val date: String? = null,
    @param:JsonProperty("branch") val branch: String? = null,
    @param:JsonProperty("link") val link: String? = null
)