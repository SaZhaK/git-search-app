package org.akolomiets.search.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.akolomiets.search.dto.enums.FilterType

/**
 * @author akolomiets
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Filter(
    @param:JsonProperty("type") val type: org.akolomiets.search.dto.enums.FilterType,
    @param:JsonProperty("value") val value: String
)