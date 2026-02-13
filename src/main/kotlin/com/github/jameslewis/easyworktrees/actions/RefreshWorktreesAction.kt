package com.github.jameslewis.easyworktrees.actions

import com.github.jameslewis.easyworktrees.ui.WorktreePanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager
import com.intellij.openapi.wm.ToolWindowManager

class RefreshWorktreesAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindowId = ChangesViewContentManager.TOOLWINDOW_ID
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolWindowId) ?: return
        val contentManager = toolWindow.contentManager
        for (content in contentManager.contents) {
            val panel = content.component as? WorktreePanel
            if (panel != null) {
                panel.refreshWorktrees()
                return
            }
        }
    }
}
