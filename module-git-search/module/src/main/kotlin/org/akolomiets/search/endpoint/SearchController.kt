package org.akolomiets.search.endpoint

import org.akolomiets.search.dto.Filter
import org.akolomiets.search.dto.SearchRequestDto
import org.akolomiets.search.dto.SearchResponseDto
import org.akolomiets.search.dto.SnippetDto
import org.akolomiets.search.dto.enums.FilterType
import org.akolomiets.search.index.service.SearchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for processing HTTP REST requests to perform search operations.
 *
 * @author akolomiets
 * @since 1.0.0
 */
@RestController
@CrossOrigin(origins = ["\${app.dev.frontend.local}"])
class SearchController @Autowired constructor(
    private val searchService: SearchService
) {

    @RequestMapping(value = ["/api/search"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun search(
        @RequestParam("searchString") searchString: String,
        @RequestParam("extensionFilter", required = false) extensionFilter: String?,
        @RequestParam("packageFilter", required = false) packageFilter: String?,
        @RequestParam("fileMaskFilter", required = false) fileMaskFilter: String?,
        @RequestParam("directoryFilter", required = false) directoryFilter: String?,
        @RequestParam("repositoryFilter", required = false) repositoryFilter: String?,
        @RequestParam("repositoryTypeFilter", required = false) repositoryTypeFilter: String?,
        @RequestParam("branchFilter", required = false) branchFilter: String?
    ): org.akolomiets.search.dto.SearchResponseDto {
        val filters = mutableListOf<org.akolomiets.search.dto.Filter>()
        extensionFilter?.let { filters.add(
            org.akolomiets.search.dto.Filter(
                org.akolomiets.search.dto.enums.FilterType.EXTENSION,
                it
            )
        ) }
        packageFilter?.let { filters.add(
            org.akolomiets.search.dto.Filter(
                org.akolomiets.search.dto.enums.FilterType.PACKAGE,
                it
            )
        ) }
        fileMaskFilter?.let { filters.add(
            org.akolomiets.search.dto.Filter(
                org.akolomiets.search.dto.enums.FilterType.FILE_MASK,
                it
            )
        ) }
        directoryFilter?.let { filters.add(
            org.akolomiets.search.dto.Filter(
                org.akolomiets.search.dto.enums.FilterType.DIRECTORY,
                it
            )
        ) }
        repositoryFilter?.let { filters.add(
            org.akolomiets.search.dto.Filter(
                org.akolomiets.search.dto.enums.FilterType.REPOSITORY,
                it
            )
        ) }
        repositoryTypeFilter?.let { filters.add(
            org.akolomiets.search.dto.Filter(
                org.akolomiets.search.dto.enums.FilterType.REPOSITORY_TYPE,
                it
            )
        ) }

        val searchRequest = org.akolomiets.search.dto.SearchRequestDto(searchString, branchFilter, filters)
        val exactSearch = searchService.exactSearch(searchRequest)
        return if (exactSearch.isEmpty()) {
            val fuzzySearch = searchService.fuzzySearch(searchRequest)
            org.akolomiets.search.dto.SearchResponseDto(emptyList(), fuzzySearch)
        } else {
            org.akolomiets.search.dto.SearchResponseDto(exactSearch, emptyList())
        }
    }

    @RequestMapping(value = ["/api/search/snippet"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun snippet(
        @RequestParam("linePath") linePath: String,
        @RequestParam("lineNumber") lineNumber: Int,
        @RequestParam("branch") branch: String
    ): org.akolomiets.search.dto.SnippetDto {
        return searchService.searchSnippet(linePath, lineNumber, branch)
    }
}