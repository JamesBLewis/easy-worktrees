package com.github.jameslewis.easyworktrees.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider
import javax.swing.JComponent

class WorktreeContentProvider(private val project: Project) : ChangesViewContentProvider {

    private var panel: WorktreePanel? = null

    override fun initContent(): JComponent {
        val p = WorktreePanel(project)
        panel = p
        return p
    }

    override fun disposeContent() {
        panel = null
    }
}
