package org.akolomiets.search.index.scheduler

import org.akolomiets.search.index.service.IndexService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Class for executing scheduled index update.
 *
 * @author akolomiets
 * @since 1.0.0
 */
@Component("search.indexUpdateScheduler")
class IndexUpdateScheduler constructor(
    private val indexService: IndexService
) {

    /**
     * Updates index after a specified time interval.
     */
    @Scheduled(initialDelay = UPDATE_RATE, fixedRate = UPDATE_RATE)
    fun updateIndex() {
        indexService.updateIndex()
    }

    companion object {
        /**
         * Index update interval.
         */
        const val UPDATE_RATE: Long = 1000 * 60 * 30 // 30 minutes
    }
}