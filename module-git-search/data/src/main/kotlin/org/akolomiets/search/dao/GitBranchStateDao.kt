package org.akolomiets.search.dao

import org.akolomiets.search.entity.GitBranchState
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Basic DAO for getting stored information about branches' states.
 *
 * @author akolomiets
 * @since 1.0.0
 */
interface GitBranchStateDao : JpaRepository<org.akolomiets.search.entity.GitBranchState, Long> {

    /**
     * Finds local states of branches' HEADs for given repository.
     *
     * @param repositoryUri the full repository SSH URI
     * @return list of branches for this repository
     */
    fun findByRepositoryUri(repositoryUri: String): List<org.akolomiets.search.entity.GitBranchState>?
}