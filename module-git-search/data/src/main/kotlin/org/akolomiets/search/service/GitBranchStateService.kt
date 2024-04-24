package org.akolomiets.search.service

import org.akolomiets.search.entity.GitBranchState

/**
 * Basic service to access stored information about branches' states.
 *
 * @author akolomiets
 * @since 1.0.0
 */
interface GitBranchStateService {

    /**
     * Updates given branch states, i.e. saves new HEADS.
     *
     * @param gitBranchStates list of new branch states
     */
    fun update(gitBranchStates: List<org.akolomiets.search.entity.GitBranchState>)

    /**
     * Returns local states of branches' HEADs for given repository.
     *
     * @param repositoryUri the full repository SSH URI
     * @return list of branches for this repository
     */
    fun findByRepositoryUri(repositoryUri: String): List<org.akolomiets.search.entity.GitBranchState>?
}