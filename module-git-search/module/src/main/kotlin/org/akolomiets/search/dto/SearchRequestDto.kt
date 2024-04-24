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
class SearchRequestDto @JsonCreator constructor(
    @param:JsonProperty("searchString") val searchString: String,
    @param:JsonProperty("branch") val branch: String? = null,
    @param:JsonProperty("filters") val filters: List<org.akolomiets.search.dto.Filter> = emptyList()
)