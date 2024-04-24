package org.akolomiets.search.git.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.akolomiets.search.config.ApplicationProperties
import org.akolomiets.search.dto.RepositoryInfoDto
import org.akolomiets.search.entity.GitBranchState
import org.akolomiets.search.git.GitService
import org.akolomiets.search.service.impl.GitBranchStateServiceImpl
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.util.FS
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Paths


/**
 * @author akolomiets
 * @since 1.0.0
 */
@Service("search.gitService")
class GitServiceImpl constructor(
    applicationProperties: org.akolomiets.search.config.ApplicationProperties,
    private val gitBranchStateService: org.akolomiets.search.service.impl.GitBranchStateServiceImpl
) : GitService {
    private val configDir = applicationProperties.configDir
    private val repositories = applicationProperties.repositories

    override fun fetchBranches(repositoryUri: String?): Map<String, String> {
        val refs = Git.lsRemoteRepository()
            .setHeads(true)
            .setRemote(repositoryUri)
            .call()

        return refs.associate { it.name.substring(it.name.lastIndexOf("/") + 1, it.name.length) to it.objectId.name }
    }

    override fun cloneRepository(repositoryUri: String, localDir: String): Git {
        val workingDir = Paths.get(localDir).toFile()

        LOGGER.info("Cloning $repositoryUri")

        val git = Git.cloneRepository()
            .setDirectory(workingDir)
            .setURI(repositoryUri)
            .call()

        LOGGER.info("Cloned $repositoryUri")

        return git
    }

    override fun setRepositoryHeads(branchesToHeads: Map<String, String>, repositoryUri: String) {
        val localBranches = gitBranchStateService.findByRepositoryUri(repositoryUri).orEmpty().associateBy { it.branch }
        val updatedGitBranchStates = branchesToHeads.map { (branchName, remoteHead) ->
            val localBranch = localBranches[branchName]
            val localBranchId = localBranch?.id
            org.akolomiets.search.entity.GitBranchState().apply {
                id = localBranchId
                this.repositoryUri = repositoryUri
                branch = branchName
                head = remoteHead
            }
        }
        gitBranchStateService.update(updatedGitBranchStates)
    }

    override fun repositoryHasUpdates(repositoryUri: String): Boolean {
        val remoteBranchesToHead = fetchBranches(repositoryUri)
        val localBranches = gitBranchStateService.findByRepositoryUri(repositoryUri).orEmpty()

        if (localBranches.isEmpty()) return true // new repository

        for (localBranch in localBranches) {
            val remoteHead = remoteBranchesToHead[localBranch.branch]
            if (remoteHead != localBranch.head) return true
        }
        return false
    }

    override fun getBranchesForUpdate(repositoryUri: String, remoteBranchesToHeads: Map<String, String>): List<String> {
        LOGGER.info("Fetching branches for $repositoryUri")
        val localBranchesToHeads =
            gitBranchStateService.findByRepositoryUri(repositoryUri).orEmpty().associate { it.branch to it.head }
        return remoteBranchesToHeads.filter { (name, remoteHead) ->
            val localHead = localBranchesToHeads[name]
            localHead != remoteHead
        }.map { it.key }
    }

    override fun getRepositories(): List<org.akolomiets.search.dto.RepositoryInfoDto> {
        return try {
            val repositoriesFile = File("$configDir/$repositories")
            val typeRef = object : TypeReference<List<org.akolomiets.search.dto.RepositoryInfoDto>>() {}
            ObjectMapper().readValue(repositoriesFile, typeRef)
        } catch (e: IOException) {
            emptyList()
        }
    }

    override fun getLastCommit(git: Git, fileRelativePath: String): RevCommit? {
        return git
            .log()
            .add(git.repository.resolve(Constants.HEAD))
            .addPath(fileRelativePath)
            .setMaxCount(1)
            .call()
            .iterator()
            .next()
    }

    override fun checkoutBranch(git: Git, branch: String) {
        val fullBranchName = "origin/$branch"
        git.checkout()
            .setName(fullBranchName)
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .setStartPoint(fullBranchName)
            .call()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GitServiceImpl::class.java)
    }
}