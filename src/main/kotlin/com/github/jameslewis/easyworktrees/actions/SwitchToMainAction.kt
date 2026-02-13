package com.github.jameslewis.easyworktrees.actions

import com.github.jameslewis.easyworktrees.services.WorktreeService
import com.intellij.ide.impl.ProjectUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import java.io.File
import java.nio.file.Path

class SwitchToMainAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null &&
            WorktreeService.getInstance(project).getGitRoot() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = WorktreeService.getInstance(project)

        ApplicationManager.getApplication().executeOnPooledThread {
            val mainWorktree = service.findMainBranchWorktree()

            ApplicationManager.getApplication().invokeLater {
                if (mainWorktree == null) {
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("Easy Worktrees")
                        .createNotification(
                            "No main worktree found",
                            "Could not find a worktree with main, master, or trunk checked out.",
                            NotificationType.WARNING
                        )
                        .notify(project)
                    return@invokeLater
                }

                if (service.isCurrentWorktree(mainWorktree)) {
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("Easy Worktrees")
                        .createNotification(
                            "Already on ${mainWorktree.branch}",
                            "This worktree already has ${mainWorktree.branch} checked out.",
                            NotificationType.INFORMATION
                        )
                        .notify(project)
                    return@invokeLater
                }

                // Check if main worktree is already open in another window
                val targetCanonical = File(mainWorktree.path).canonicalPath
                val existing = ProjectManager.getInstance().openProjects.firstOrNull { p ->
                    val basePath = p.basePath ?: return@firstOrNull false
                    File(basePath).canonicalPath == targetCanonical
                }

                if (existing != null) {
                    val frame = WindowManager.getInstance().getFrame(existing)
                    frame?.toFront()
                    frame?.requestFocus()
                    return@invokeLater
                }

                // Switch current window to the main worktree
                ProjectUtil.openOrImport(Path.of(mainWorktree.path), project, false)
            }
        }
    }
}
