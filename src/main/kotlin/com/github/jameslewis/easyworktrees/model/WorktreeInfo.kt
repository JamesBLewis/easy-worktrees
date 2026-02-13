package com.github.jameslewis.easyworktrees.model

data class WorktreeInfo(
    val path: String,
    val commitHash: String,
    val branch: String?,
    val isMainWorktree: Boolean = false,
    val isBare: Boolean = false,
    val isDetached: Boolean = false,
    val isLocked: Boolean = false,
    val lockReason: String? = null,
    val isPrunable: Boolean = false,
) {
    val shortHash: String
        get() = commitHash.take(8)

    val displayBranch: String
        get() = when {
            isBare -> "(bare)"
            isDetached -> "(detached HEAD)"
            branch != null -> branch
            else -> "(unknown)"
        }

    val isOnMainBranch: Boolean
        get() = branch in MAIN_BRANCH_NAMES

    val directoryName: String
        get() = path.substringAfterLast('/')

    companion object {
        val MAIN_BRANCH_NAMES = setOf("main", "master", "trunk")
    }
}
