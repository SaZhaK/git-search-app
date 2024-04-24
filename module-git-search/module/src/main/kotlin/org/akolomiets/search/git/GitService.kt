package org.akolomiets.search.git

import org.akolomiets.search.dto.RepositoryInfoDto
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

/**
 * Service for providing utility methods to perform dynamic operations with Git.
 *
 * @author akolomiets
 * @since 1.0.0
 */
interface GitService {

    /**
     * Returns list of all branches in given repository associated with their HEAD parameters.
     * Does **NOT** clone the repository to local memory.
     *
     * @param repositoryUri the repository SSH URI
     *
     * @return branches associated with HEADs
     */
    fun fetchBranches(repositoryUri: String?): Map<String, String>

    /**
     * Clones a repository to local memory.
     *
     * @param repositoryUri the repository SSH URI
     * @param localDir name of local directory to store cloned repository
     *
     * @return reference to cloned repository
     */
    fun cloneRepository(repositoryUri: String, localDir: String): Git

    /**
     * Updates states of processed branches in local Database.
     *
     * @param branchesToHeads branches associated with HEADs
     * @param repositoryUri the repository SSH URI
     */
    fun setRepositoryHeads(branchesToHeads: Map<String, String>, repositoryUri: String)

    /**
     * Checks if given repository has any branches that have to be updated.
     *
     * @param repositoryUri the repository SSH URI
     *
     * @return true if at least on branch has updates, false otherwise
     */
    fun repositoryHasUpdates(repositoryUri: String): Boolean

    /**
     * Returns list of branches' names that has to be updated.
     *
     * @param repositoryUri the repository SSH URI
     * @param remoteBranchesToHeads branches associated with actual current HEADs
     */
    fun getBranchesForUpdate(repositoryUri: String, remoteBranchesToHeads: Map<String, String>): List<String>

    /**
     * Returns list of all repositories that are processed by this application
     * or an empty list if no 'repositories.json' file was provided.
     *
     * @return list of repositories
     */
    fun getRepositories(): List<org.akolomiets.search.dto.RepositoryInfoDto>

    /**
     * Returns information about last commit to a given file.
     *
     * @param git reference to a locally cloned repository
     * @param fileRelativePath path to a file in a locally clones repository. Relative from repository directory.
     *
     * @return commit information
     */
    fun getLastCommit(git: Git, fileRelativePath: String): RevCommit?

    /**
     * Checkouts to a given branch.
     *
     * @param git reference to a locally cloned repository
     * @param branch branch name
     */
    fun checkoutBranch(git: Git, branch: String)
}