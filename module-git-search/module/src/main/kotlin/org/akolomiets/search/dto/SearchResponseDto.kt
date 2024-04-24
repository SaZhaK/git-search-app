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
class SearchResponseDto @JsonCreator constructor(
        @param:JsonProperty("exactSearch") val exactSearch: List<org.akolomiets.search.dto.LineDto>? = null,
        @param:JsonProperty("fuzzySearch") val fuzzySearch: List<org.akolomiets.search.dto.LineDto>? = null
)