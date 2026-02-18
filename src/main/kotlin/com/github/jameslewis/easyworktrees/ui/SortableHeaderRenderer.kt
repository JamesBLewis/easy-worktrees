package com.github.jameslewis.easyworktrees.ui

import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SortOrder
import javax.swing.UIManager
import javax.swing.table.TableCellRenderer

class SortableHeaderRenderer(
    private val delegate: TableCellRenderer,
    private val model: WorktreeTableModel
) : TableCellRenderer {

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean,
        hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val comp = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (comp is JLabel) {
            val modelColumn = table.convertColumnIndexToModel(column)
            comp.icon = when {
                model.getSortColumn() == modelColumn && model.getSortOrder() == SortOrder.ASCENDING ->
                    UIManager.getIcon("Table.ascendingSortIcon")
                model.getSortColumn() == modelColumn && model.getSortOrder() == SortOrder.DESCENDING ->
                    UIManager.getIcon("Table.descendingSortIcon")
                else -> null
            }
            comp.horizontalTextPosition = JLabel.LEADING
        }
        return comp
    }
}
