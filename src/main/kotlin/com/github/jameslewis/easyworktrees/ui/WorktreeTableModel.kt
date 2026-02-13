package com.github.jameslewis.easyworktrees.ui

import com.github.jameslewis.easyworktrees.model.WorktreeInfo
import javax.swing.table.AbstractTableModel

class WorktreeTableModel : AbstractTableModel() {

    companion object {
        val COLUMNS = arrayOf("Branch", "Path", "Commit", "Status")
        const val COL_BRANCH = 0
        const val COL_PATH = 1
        const val COL_COMMIT = 2
        const val COL_STATUS = 3
    }

    private var allWorktrees: List<WorktreeInfo> = emptyList()
    private var filteredWorktrees: List<WorktreeInfo> = emptyList()
    private var filterText: String = ""

    override fun getRowCount(): Int = filteredWorktrees.size
    override fun getColumnCount(): Int = COLUMNS.size
    override fun getColumnName(column: Int): String = COLUMNS[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val wt = filteredWorktrees[rowIndex]
        return when (columnIndex) {
            COL_BRANCH -> wt.displayBranch
            COL_PATH -> wt.directoryName
            COL_COMMIT -> wt.shortHash
            COL_STATUS -> buildStatus(wt)
            else -> ""
        }
    }

    fun getWorktreeAt(rowIndex: Int): WorktreeInfo? =
        filteredWorktrees.getOrNull(rowIndex)

    fun findRowByPath(path: String): Int =
        filteredWorktrees.indexOfFirst { it.path == path }

    fun setWorktrees(worktrees: List<WorktreeInfo>) {
        allWorktrees = worktrees
        applyFilter()
    }

    fun setFilter(text: String) {
        filterText = text.trim()
        applyFilter()
    }

    private fun applyFilter() {
        filteredWorktrees = if (filterText.isEmpty()) {
            allWorktrees
        } else {
            val lower = filterText.lowercase()
            allWorktrees.filter { wt ->
                (wt.branch?.lowercase()?.contains(lower) == true) ||
                    wt.path.lowercase().contains(lower) ||
                    wt.directoryName.lowercase().contains(lower)
            }
        }
        fireTableDataChanged()
    }

    private fun buildStatus(wt: WorktreeInfo): String {
        val parts = mutableListOf<String>()
        if (wt.isMainWorktree) parts.add("main worktree")
        if (wt.isLocked) parts.add(if (wt.lockReason != null) "locked: ${wt.lockReason}" else "locked")
        if (wt.isPrunable) parts.add("prunable")
        return parts.joinToString(", ")
    }
}
