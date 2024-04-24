package org.akolomiets.search.entity

import org.akolomiets.search.entity.GitBranchState.Companion.TABLE_NAME
import javax.persistence.*

/**
 * Local information about indexed branch.
 *
 * @author akolomiets
 * @since 1.0.0
 */
@Entity(name = TABLE_NAME)
@Table(name = TABLE_NAME)
class GitBranchState {

    /**
     * The entity ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "${TABLE_NAME}_seq")
    var id: Long? = null

    /**
     * The URI of repository, which branch belongs to
     */
    @get:Column(name = "repositoryUri")
    var repositoryUri: String? = null

    /**
     * Branch name
     */
    @get:Column(name = "branch")
    var branch: String? = null

    /**
     * Last known HEAD parameter for this branch
     */
    @get:Column(name = "head")
    var head: String? = null

    companion object {
        const val TABLE_NAME = "git_branch_state"
    }
}