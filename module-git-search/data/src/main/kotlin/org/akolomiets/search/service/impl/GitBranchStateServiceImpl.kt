package org.akolomiets.search.service.impl

import org.akolomiets.search.dao.GitBranchStateDao
import org.akolomiets.search.entity.GitBranchState
import org.akolomiets.search.service.GitBranchStateService
import org.springframework.stereotype.Service

/**
 * @author akolomiets
 * @since 1.0.0
 */
@Service("search.gitBranchStateService")
class GitBranchStateServiceImpl constructor(
    private val gitBranchStateDao: org.akolomiets.search.dao.GitBranchStateDao
): org.akolomiets.search.service.GitBranchStateService {

    override fun update(gitBranchStates: List<org.akolomiets.search.entity.GitBranchState>) {
        gitBranchStateDao.saveAll(gitBranchStates)
    }

    override fun findByRepositoryUri(repositoryUri: String): List<org.akolomiets.search.entity.GitBranchState>? {
        return gitBranchStateDao.findByRepositoryUri(repositoryUri)
    }
}