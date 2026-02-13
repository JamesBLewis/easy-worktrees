package com.github.jameslewis.easyworktrees.services

import com.github.jameslewis.easyworktrees.model.WorktreeInfo
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import git4idea.config.GitExecutableManager
import git4idea.repo.GitRepositoryManager
import java.io.File

@Service(Service.Level.PROJECT)
class WorktreeService(private val project: Project) {

    companion object {
        private val LOG = Logger.getInstance(WorktreeService::class.java)

        fun getInstance(project: Project): WorktreeService =
            project.getService(WorktreeService::class.java)
    }

    fun getGitRoot(): String? {
        val repos = GitRepositoryManager.getInstance(project).repositories
        if (repos.isEmpty()) return null
        return repos.first().root.path
    }

    fun listWorktrees(): List<WorktreeInfo> {
        val gitRoot = getGitRoot() ?: return emptyList()
        val gitExecutable = GitExecutableManager.getInstance().getPathToGit(project)

        val commandLine = GeneralCommandLine(gitExecutable, "worktree", "list", "--porcelain")
            .withWorkDirectory(gitRoot)

        return try {
            val handler = CapturingProcessHandler(commandLine)
            val output = handler.runProcess(10_000)

            if (output.exitCode != 0) {
                LOG.warn("git worktree list failed (exit ${output.exitCode}): ${output.stderr}")
                return emptyList()
            }

            parsePorcelainOutput(output.stdoutLines)
        } catch (e: Exception) {
            LOG.warn("Failed to run git worktree list", e)
            emptyList()
        }
    }

    fun findMainBranchWorktree(): WorktreeInfo? =
        listWorktrees().firstOrNull { it.isOnMainBranch }

    fun isCurrentWorktree(worktree: WorktreeInfo): Boolean {
        val projectPath = project.basePath ?: return false
        return File(projectPath).canonicalPath == File(worktree.path).canonicalPath
    }

    internal fun parsePorcelainOutput(lines: List<String>): List<WorktreeInfo> {
        val worktrees = mutableListOf<WorktreeInfo>()
        var currentPath: String? = null
        var currentHash = ""
        var currentBranch: String? = null
        var isMainWorktree = false
        var isBare = false
        var isDetached = false
        var isLocked = false
        var lockReason: String? = null
        var isPrunable = false
        var isFirst = true

        fun commitEntry() {
            val path = currentPath ?: return
            worktrees.add(
                WorktreeInfo(
                    path = path,
                    commitHash = currentHash,
                    branch = currentBranch,
                    isMainWorktree = isMainWorktree,
                    isBare = isBare,
                    isDetached = isDetached,
                    isLocked = isLocked,
                    lockReason = lockReason,
                    isPrunable = isPrunable,
                )
            )
            currentPath = null
            currentHash = ""
            currentBranch = null
            isMainWorktree = false
            isBare = false
            isDetached = false
            isLocked = false
            lockReason = null
            isPrunable = false
        }

        for (line in lines) {
            when {
                line.startsWith("worktree ") -> {
                    if (currentPath != null) commitEntry()
                    currentPath = line.removePrefix("worktree ")
                    if (isFirst) {
                        isMainWorktree = true
                        isFirst = false
                    }
                }
                line.startsWith("HEAD ") -> currentHash = line.removePrefix("HEAD ")
                line.startsWith("branch ") -> currentBranch = line.removePrefix("branch refs/heads/")
                line == "bare" -> isBare = true
                line == "detached" -> isDetached = true
                line == "locked" -> isLocked = true
                line.startsWith("locked ") -> {
                    isLocked = true
                    lockReason = line.removePrefix("locked ")
                }
                line == "prunable" -> isPrunable = true
                line.startsWith("prunable ") -> isPrunable = true
                line.isBlank() -> commitEntry()
            }
        }
        commitEntry()

        return worktrees
    }
}
