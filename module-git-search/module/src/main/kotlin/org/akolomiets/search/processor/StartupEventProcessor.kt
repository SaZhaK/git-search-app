package org.akolomiets.search.processor

import org.akolomiets.search.index.service.IndexService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

/**
 * Event processor for performing actions on application startup.
 *
 * @author akolomiets
 * @since 1.0.0
 */
@Service("search.startupEventProcessor")
class StartupEventProcessor constructor(
    private val indexService: IndexService
) {

    /**
     * Updates index on application startup.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun updateIndexOnApplicationStartup() {
        indexService.updateIndex()
    }
}