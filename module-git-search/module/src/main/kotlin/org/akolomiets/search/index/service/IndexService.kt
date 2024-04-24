package org.akolomiets.search.index.service

import org.akolomiets.search.dto.IndexInfoDto

/**
 * Service for updating Apache Lucene index.
 *
 * @author akolomiets
 * @since 1.0.0
 */
interface IndexService {

    /**
     * Returns information about this index.
     *
     * @return index meta information
     */
    fun getIndexInfo(): org.akolomiets.search.dto.IndexInfoDto

    /**
     * Updates Apache Lucene index.
     *
     * 1. Scans all repositories to find all branches that need to be updated.
     * 2. Allocates a new thread for each repository that contains at least one branch that has to be updated.
     * If amount of branches in one repository is considered to be too big
     * splits them into batches and allocates a new thread for each batch.
     * 3. Clones a local copy of repository for each thread and performs a full-scan of all lines in all files.
     * 4. All gathered data is then placed in separate persistable caches for each repository.
     * 5. Waits for completion of all threads and then writes all caches to index at once
     * avoiding on-write blocks of Apache Lucene index.
     */
    fun updateIndex()
}