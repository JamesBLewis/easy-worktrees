package com.github.jameslewis.easyworktrees.ui

import com.github.jameslewis.easyworktrees.model.WorktreeInfo
import com.github.jameslewis.easyworktrees.services.WorktreeService
import com.intellij.ide.impl.ProjectUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.SearchTextField
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.nio.file.Path
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer

class WorktreePanel(private val project: Project) : SimpleToolWindowPanel(true, true) {

    private val tableModel = WorktreeTableModel()
    private val table = JBTable(tableModel)
    private val searchField = SearchTextField(false)
    private val service = WorktreeService.getInstance(project)

    init {
        setupToolbar()
        setupSearchField()
        setupTable()
        setupLayout()
        refreshWorktrees()
    }

    private fun setupToolbar() {
        val actionManager = ActionManager.getInstance()
        val group = DefaultActionGroup().apply {
            actionManager.getAction("EasyWorktrees.SwitchToMain")?.let { add(it) }
            addSeparator()
            actionManager.getAction("EasyWorktrees.Refresh")?.let { add(it) }
        }
        val toolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, group, true)
        toolbar.targetComponent = this
        setToolbar(toolbar.component)
    }

    private fun setupSearchField() {
        searchField.textEditor.emptyText.text = "Search worktrees..."
        searchField.textEditor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onFilterChanged()
            override fun removeUpdate(e: DocumentEvent) = onFilterChanged()
            override fun changedUpdate(e: DocumentEvent) = onFilterChanged()
        })
    }

    private fun onFilterChanged() {
        tableModel.setFilter(searchField.text)
        highlightCurrentWorktree()
    }

    private fun setupTable() {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        table.rowHeight = 24

        table.columnModel.getColumn(WorktreeTableModel.COL_BRANCH).preferredWidth = 200
        table.columnModel.getColumn(WorktreeTableModel.COL_PATH).preferredWidth = 250
        table.columnModel.getColumn(WorktreeTableModel.COL_COMMIT).preferredWidth = 100
        table.columnModel.getColumn(WorktreeTableModel.COL_STATUS).preferredWidth = 150

        val renderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable, value: Any?, isSelected: Boolean,
                hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                val wt = tableModel.getWorktreeAt(row)
                if (wt != null && service.isCurrentWorktree(wt)) {
                    comp.font = comp.font.deriveFont(Font.BOLD)
                }
                return comp
            }
        }
        for (i in 0 until table.columnCount) {
            table.columnModel.getColumn(i).cellRenderer = renderer
        }

        // Sort on column header click
        table.tableHeader.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val col = table.columnAtPoint(e.point)
                if (col >= 0) {
                    tableModel.toggleSort(col)
                    updateHeaderSortIndicators()
                    highlightCurrentWorktree()
                }
            }
        })
        // Set cursor to hand on sortable column headers
        table.tableHeader.defaultRenderer = SortableHeaderRenderer(table.tableHeader.defaultRenderer, tableModel)

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val row = table.rowAtPoint(e.point)
                    if (row >= 0) {
                        tableModel.getWorktreeAt(row)?.let { switchToWorktree(it) }
                    }
                }
            }

            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) showContextMenu(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) showContextMenu(e)
            }
        })
    }

    private fun setupLayout() {
        val content = JPanel(BorderLayout()).apply {
            add(searchField, BorderLayout.NORTH)
            add(JScrollPane(table), BorderLayout.CENTER)
        }
        setContent(content)
    }

    private fun showContextMenu(e: MouseEvent) {
        val row = table.rowAtPoint(e.point)
        if (row < 0) return
        table.setRowSelectionInterval(row, row)
        val wt = tableModel.getWorktreeAt(row) ?: return

        JPopupMenu().apply {
            add(JMenuItem("Switch to Worktree").apply {
                addActionListener { switchToWorktree(wt) }
            })
            add(JMenuItem("Open in New Window").apply {
                addActionListener { openInNewWindow(wt) }
            })
            addSeparator()
            add(JMenuItem("Copy Path").apply {
                addActionListener {
                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(StringSelection(wt.path), null)
                }
            })
        }.show(table, e.x, e.y)
    }

    /**
     * Default switch behavior: replaces the current window with the selected worktree.
     * If already in the target worktree, shows a notification.
     * If the target is already open in another window, brings that window to focus.
     */
    fun switchToWorktree(worktree: WorktreeInfo) {
        if (service.isCurrentWorktree(worktree)) {
            notify("Already here", "You are already in ${worktree.displayBranch}", NotificationType.INFORMATION)
            return
        }

        // Check if already open in another window — bring it to focus
        val existing = findOpenProject(worktree)
        if (existing != null) {
            val frame = WindowManager.getInstance().getFrame(existing)
            frame?.toFront()
            frame?.requestFocus()
            return
        }

        // Open in the current window (replaces current project)
        ProjectUtil.openOrImport(Path.of(worktree.path), project, false)
    }

    /**
     * Opens the worktree in a new IntelliJ window, keeping the current window open.
     */
    private fun openInNewWindow(worktree: WorktreeInfo) {
        if (service.isCurrentWorktree(worktree)) {
            notify("Already here", "You are already in ${worktree.displayBranch}", NotificationType.INFORMATION)
            return
        }

        val existing = findOpenProject(worktree)
        if (existing != null) {
            val frame = WindowManager.getInstance().getFrame(existing)
            frame?.toFront()
            frame?.requestFocus()
            return
        }

        ProjectUtil.openOrImport(Path.of(worktree.path), null, true)
    }

    private fun findOpenProject(worktree: WorktreeInfo): Project? {
        val targetCanonical = File(worktree.path).canonicalPath
        return ProjectManager.getInstance().openProjects.firstOrNull { p ->
            val basePath = p.basePath ?: return@firstOrNull false
            File(basePath).canonicalPath == targetCanonical
        }
    }

    private fun notify(title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Easy Worktrees")
            .createNotification(title, content, type)
            .notify(project)
    }

    fun refreshWorktrees() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val worktrees = service.listWorktrees()
            ApplicationManager.getApplication().invokeLater {
                tableModel.setWorktrees(worktrees)
                highlightCurrentWorktree()
            }
        }
    }

    private fun updateHeaderSortIndicators() {
        table.tableHeader.repaint()
    }

    private fun highlightCurrentWorktree() {
        val projectPath = project.basePath ?: return
        val canonical = File(projectPath).canonicalPath
        val row = tableModel.findRowByPath(canonical)
        if (row >= 0) {
            table.setRowSelectionInterval(row, row)
            table.scrollRectToVisible(table.getCellRect(row, 0, true))
        }
    }
}
