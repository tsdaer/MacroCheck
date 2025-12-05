package core.macrocheck.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import core.macrocheck.config.MacroCheckConfig
import core.macrocheck.i18n.MacroCheckBundle
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.BoxLayout
import javax.swing.table.AbstractTableModel
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.DefaultCellEditor
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

/**
 * 宏检查配置界面
 */
class MacroCheckConfigurable(private val project: Project) : Configurable {
    private lateinit var panel: JPanel
    private lateinit var enableWhitelistCheckBox: JCheckBox
    private lateinit var whitelistTable: JBTable
    private lateinit var tableModel: WhitelistTableModel

    override fun createComponent(): JComponent {
        panel = JPanel(BorderLayout())

        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)

        // 启用白名单复选框
        enableWhitelistCheckBox = JCheckBox(MacroCheckBundle.message("config.whitelist.enable"))
        contentPanel.add(enableWhitelistCheckBox)

        // 白名单标签
        val whitelistLabel = JLabel(MacroCheckBundle.message("config.whitelist.label"))
        contentPanel.add(whitelistLabel)

        // 创建表格模型
        tableModel = WhitelistTableModel()

        // 创建表格
        whitelistTable = JBTable(tableModel)
        whitelistTable.emptyText.text = MacroCheckBundle.message("config.whitelist.tableEmptyText")

        // 设置列宽 - 宏名称在第一列，启用状态在第二列
        whitelistTable.columnModel.getColumn(0).preferredWidth = 200 // 宏名称列（第一列）
        whitelistTable.columnModel.getColumn(1).preferredWidth = 60   // 启用列（第二列）

        // 设置启用状态列居中对齐
        val enabledColumn = whitelistTable.columnModel.getColumn(1)
        enabledColumn.cellRenderer = CenterAlignedCheckBoxRenderer()
        enabledColumn.cellEditor = CenterAlignedCheckBoxEditor()

        // 设置宏名称列左对齐
        val nameColumn = whitelistTable.columnModel.getColumn(0)
        nameColumn.cellRenderer = LeftAlignedRenderer()

        // 使用 ToolbarDecorator 添加工具栏（增加/删除按钮）
        val tablePanel = ToolbarDecorator.createDecorator(whitelistTable)
            .setAddAction {
                // 添加新行（默认启用）
                tableModel.addMacro("NEW_MACRO", true)
                whitelistTable.editCellAt(tableModel.rowCount - 1, 0) // 编辑宏名称列（第一列）
            }
            .setRemoveAction {
                // 删除选中行
                val selectedRow = whitelistTable.selectedRow
                if (selectedRow >= 0) {
                    tableModel.removeMacro(selectedRow)
                }
            }
            .disableUpDownActions() // 禁用上下移动按钮（可选）
            .createPanel()

        contentPanel.add(tablePanel)
        panel.add(contentPanel, BorderLayout.CENTER)
        return panel
    }

    // 修改 isModified() 方法
    override fun isModified(): Boolean {
        val config = MacroCheckConfig.getInstance(project)

        // 检查白名单启用状态是否改变
        if (enableWhitelistCheckBox.isSelected != config.isEnableWhitelist()) {
            return true
        }

        // 检查白名单内容是否改变（包括启用状态）
        val currentMacros = tableModel.getAllMacros().toSet()
        val savedMacros = config.getAllMacros().toSet()

        return currentMacros != savedMacros
    }

    // 修改 reset() 方法
    override fun reset() {
        val config = MacroCheckConfig.getInstance(project)
        enableWhitelistCheckBox.isSelected = config.isEnableWhitelist()

        // 从配置加载宏（包括启用状态）
        val macros = config.getAllMacros()
        tableModel.setMacros(macros)
    }

    // 修改 apply() 方法
    override fun apply() {
        val config = MacroCheckConfig.getInstance(project)
        config.setEnableWhitelist(enableWhitelistCheckBox.isSelected)
        config.setAllMacros(tableModel.getAllMacros())
    }

    override fun getDisplayName(): String {
        return MacroCheckBundle.message("config.display.name")
    }

    /**
     * 白名单表格模型
     */
    private inner class WhitelistTableModel : AbstractTableModel() {
        private val columnNames = arrayOf(
            MacroCheckBundle.message("config.whitelist.tableColumnName"),    // 第一列：宏名称
            MacroCheckBundle.message("config.whitelist.tableColumnEnabled")  // 第二列：启用状态
        )
        private var macros = mutableListOf<Pair<String, Boolean>>() // (宏名称, 启用状态)

        override fun getRowCount(): Int = macros.size

        override fun getColumnCount(): Int = columnNames.size

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> String::class.java   // 第一列是字符串（宏名称）
                1 -> Boolean::class.java  // 第二列是勾选框（启用状态）
                else -> Any::class.java
            }
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            return when (columnIndex) {
                0 -> macros[rowIndex].first   // 宏名称
                1 -> macros[rowIndex].second  // 启用状态
                else -> ""
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return true // 两列都可编辑
        }

        override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
            if (rowIndex < 0 || rowIndex >= macros.size) return

            when (columnIndex) {
                0 -> {
                    // 更新宏名称
                    val newName = value?.toString()?.trim()
                    if (newName.isNullOrEmpty()) {
                        // 如果名称为空，删除该行
                        removeMacro(rowIndex)
                    } else {
                        // 检查是否与其他行重复（除了当前行）
                        val isDuplicate = macros.withIndex().any { (index, pair) ->
                            index != rowIndex && pair.first.equals(newName, ignoreCase = true)
                        }

                        if (isDuplicate) {
                            // 显示错误或拒绝修改
                            return
                        }

                        val enabled = macros[rowIndex].second
                        macros[rowIndex] = newName to enabled
                        fireTableCellUpdated(rowIndex, columnIndex)
                    }
                }
                1 -> {
                    // 更新启用状态
                    val enabled = value as? Boolean ?: false
                    val macroName = macros[rowIndex].first
                    macros[rowIndex] = macroName to enabled
                    fireTableCellUpdated(rowIndex, columnIndex)
                }
            }
        }

        fun addMacro(macro: String, enabled: Boolean = true) {
            macros.add(macro to enabled)
            fireTableRowsInserted(macros.size - 1, macros.size - 1)
        }

        fun removeMacro(rowIndex: Int) {
            if (rowIndex in 0 until macros.size) {
                macros.removeAt(rowIndex)
                fireTableRowsDeleted(rowIndex, rowIndex)
            }
        }

        /**
         * 获取所有启用的宏名称
         */
        fun getEnabledMacros(): List<String> {
            return macros.filter { it.second }.map { it.first }
        }

        /**
         * 获取所有宏（包括启用和禁用状态）
         */
        fun getAllMacros(): List<Pair<String, Boolean>> {
            return macros.toList()
        }

        /**
         * 设置宏列表
         */
        fun setMacros(newMacros: List<Pair<String, Boolean>>) {
            macros.clear()
            macros.addAll(newMacros.filter { it.first.isNotBlank() })
            fireTableDataChanged()
        }
    }

    /**
     * 勾选框渲染器
     */
    private class CenterAlignedCheckBoxRenderer : DefaultTableCellRenderer() {
        private val checkBox = JCheckBox()

        init {
            checkBox.horizontalAlignment = SwingConstants.CENTER
            checkBox.isOpaque = true
        }

        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            if (value is Boolean) {
                checkBox.isSelected = value

                // 设置选中状态的背景色
                if (isSelected) {
                    checkBox.background = table?.selectionBackground
                    checkBox.foreground = table?.selectionForeground
                } else {
                    checkBox.background = table?.background
                    checkBox.foreground = table?.foreground
                }

                return checkBox
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        }
    }

    /**
     * 勾选框编辑器
     */
    private class CenterAlignedCheckBoxEditor : DefaultCellEditor(JCheckBox()) {
        init {
            (editorComponent as JCheckBox).horizontalAlignment = SwingConstants.CENTER
        }

        override fun getTableCellEditorComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int
        ): Component {
            val checkBox = super.getTableCellEditorComponent(table, value, isSelected, row, column) as JCheckBox
            checkBox.isSelected = value as? Boolean ?: false
            checkBox.horizontalAlignment = SwingConstants.CENTER
            return checkBox
        }
    }

    /**
     * 左对齐的文本渲染器
     */
    private class LeftAlignedRenderer : DefaultTableCellRenderer() {
        init {
            horizontalAlignment = SwingConstants.LEFT
        }

        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            horizontalAlignment = SwingConstants.LEFT
            return component
        }
    }
}