package com.github.jameslewis.easyworktrees.ui

import com.github.jameslewis.easyworktrees.model.WorktreeInfo
import javax.swing.SortOrder
import javax.swing.table.AbstractTableModel

class WorktreeTableModel : AbstractTableModel() {

    companion object {
        val COLUMNS = arrayOf("Branch", "Path", "Commit", "Status")
        const val COL_BRANCH = 0
        const val COL_PATH = 1
        const val COL_COMMIT = 2
        const val COL_STATUS = 3
        val SORTABLE_COLUMNS = setOf(COL_BRANCH, COL_PATH)
    }

    private var allWorktrees: List<WorktreeInfo> = emptyList()
    private var filteredWorktrees: List<WorktreeInfo> = emptyList()
    private var filterText: String = ""
    private var sortColumn: Int? = null
    private var sortOrder: SortOrder = SortOrder.UNSORTED

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
        applyFilterAndSort()
    }

    fun setFilter(text: String) {
        filterText = text.trim()
        applyFilterAndSort()
    }

    fun getSortColumn(): Int? = sortColumn
    fun getSortOrder(): SortOrder = sortOrder

    /**
     * Toggles sorting for the given column.
     * Cycles: unsorted -> ascending -> descending -> unsorted.
     * If a different column is clicked, starts at ascending.
     */
    fun toggleSort(column: Int) {
        if (column !in SORTABLE_COLUMNS) return

        if (sortColumn == column) {
            sortOrder = when (sortOrder) {
                SortOrder.ASCENDING -> SortOrder.DESCENDING
                SortOrder.DESCENDING -> SortOrder.UNSORTED
                else -> SortOrder.ASCENDING
            }
            if (sortOrder == SortOrder.UNSORTED) sortColumn = null
        } else {
            sortColumn = column
            sortOrder = SortOrder.ASCENDING
        }
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        var result = if (filterText.isEmpty()) {
            allWorktrees
        } else {
            val lower = filterText.lowercase()
            allWorktrees.filter { wt ->
                (wt.branch?.lowercase()?.contains(lower) == true) ||
                    wt.path.lowercase().contains(lower) ||
                    wt.directoryName.lowercase().contains(lower)
            }
        }

        val col = sortColumn
        if (col != null && sortOrder != SortOrder.UNSORTED) {
            val selector: (WorktreeInfo) -> String = when (col) {
                COL_BRANCH -> { wt -> wt.displayBranch.lowercase() }
                COL_PATH -> { wt -> wt.directoryName.lowercase() }
                else -> { _ -> "" }
            }
            result = if (sortOrder == SortOrder.ASCENDING) {
                result.sortedBy(selector)
            } else {
                result.sortedByDescending(selector)
            }
        }

        filteredWorktrees = result
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
