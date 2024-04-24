package org.akolomiets.search.endpoint

import org.akolomiets.search.index.service.IndexService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for processing HTTP REST requests to perform operations on index.
 *
 * @author akolomiets
 * @since 1.0.0
 */
@RestController
@CrossOrigin(origins = ["\${app.dev.frontend.local}"])
class FetchController constructor(
    private val indexService: IndexService
) {

    @RequestMapping(value = ["/api/fetch"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetch(): ResponseEntity<Nothing> {
        indexService.updateIndex()
        return ResponseEntity<Nothing>(HttpStatus.OK)
    }

    @RequestMapping(value = ["/api/indexInfo"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun indexInfo(): org.akolomiets.search.dto.IndexInfoDto {
        val indexMetaInfo =  indexService.getIndexInfo()
        return mapIndexMetaInfoToDto(indexMetaInfo)
    }

    private fun mapIndexMetaInfoToDto(indexMetaInfoVo: org.akolomiets.search.dto.IndexInfoDto): org.akolomiets.search.dto.IndexInfoDto {
        return org.akolomiets.search.dto.IndexInfoDto(indexMetaInfoVo.lastUpdateTime)
    }
}