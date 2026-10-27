package com.example.expensetracker.ui.gui

import com.example.expensetracker.domain.BudgetLevel
import com.example.expensetracker.domain.BudgetStatus
import com.example.expensetracker.domain.CategoryTotal
import com.example.expensetracker.domain.Expense
import com.example.expensetracker.domain.ExpenseDraft
import com.example.expensetracker.domain.Money
import com.example.expensetracker.domain.StorageException
import com.example.expensetracker.domain.total
import com.example.expensetracker.export.CsvExporter
import com.example.expensetracker.service.ExpenseService
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JTable
import javax.swing.JToolBar
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.RowSorter
import javax.swing.ScrollPaneConstants
import javax.swing.SortOrder
import javax.swing.SwingConstants
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

private val MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

private val COLOR_SHARE = Color(66, 133, 244)
private val COLOR_OK = Color(67, 160, 71)
private val COLOR_WARNING = Color(251, 140, 0)
private val COLOR_EXCEEDED = Color(211, 47, 47)

/** Read-only table model: ID | Date | Category | Amount | Note. */
private class ExpenseTableModel : AbstractTableModel() {
    var rows: List<Expense> = emptyList()
        private set

    private val names = arrayOf("ID", "Date", "Category", "Amount", "Note")

    fun setRows(newRows: List<Expense>) {
        rows = newRows
        fireTableDataChanged()
    }

    override fun getRowCount() = rows.size
    override fun getColumnCount() = names.size
    override fun getColumnName(column: Int) = names[column]

    override fun getColumnClass(column: Int): Class<*> = when (column) {
        0, 3 -> Long::class.javaObjectType
        else -> String::class.java
    }

    override fun getValueAt(row: Int, column: Int): Any {
        val e = rows[row]
        return when (column) {
            0 -> e.id
            1 -> e.date.toString()
            2 -> e.category.name
            3 -> e.amount.cents
            else -> e.note
        }
    }
}

/** Table cell with horizontal padding; [format] turns the raw value into the text shown. */
private class PaddedRenderer(
    private val alignment: Int,
    private val format: (Any?) -> String = { it?.toString() ?: "" },
) : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int,
    ): Component {
        val c = super.getTableCellRendererComponent(table, format(value), isSelected, false, row, column)
        horizontalAlignment = alignment
        border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
        return c
    }
}

private fun centsText(value: Any?): String = if (value is Long) Money(value).toString() else ""

class MainWindow(
    private val service: ExpenseService,
    private val exporter: CsvExporter,
) : JFrame("Expense Tracker") {

    /** The month being shown, or null for "all time". */
    private var month: YearMonth? = service.currentMonth()

    private val tableModel = ExpenseTableModel()
    private val table = JTable(tableModel)
    private val monthLabel = JLabel("", SwingConstants.CENTER)
    private val allTimeBox = JCheckBox("All time")
    private val statusLabel = JLabel(" ")
    private val summaryContent = JPanel()

    private val shortcut = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx

    private val addAction = action("Add expense…", ctrl(KeyEvent.VK_N)) { addExpense() }
    private val editAction = action("Edit expense…", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)) { editSelected() }
    private val deleteAction = action("Delete expense", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)) { deleteSelected() }
    private val exportAction = action("Export to CSV…", ctrl(KeyEvent.VK_E)) { exportCsv() }
    private val setBudgetAction = action("Set monthly budget…", ctrl(KeyEvent.VK_B)) { setBudget() }
    private val removeBudgetAction = action("Remove budget…", null) { removeBudget() }
    private val prevMonthAction = action("Previous month", alt(KeyEvent.VK_LEFT)) { shiftMonth(-1) }
    private val nextMonthAction = action("Next month", alt(KeyEvent.VK_RIGHT)) { shiftMonth(1) }
    private val thisMonthAction = action("This month", alt(KeyEvent.VK_HOME)) { setMonth(service.currentMonth()) }
    private val allTimeAction = action("Show all time", null) { setMonth(null) }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        jMenuBar = buildMenuBar()
        contentPane.layout = BorderLayout()
        contentPane.add(buildTopBar(), BorderLayout.NORTH)
        contentPane.add(buildTablePane(), BorderLayout.CENTER)
        contentPane.add(buildSummaryPane(), BorderLayout.EAST)
        contentPane.add(buildStatusBar(), BorderLayout.SOUTH)

        minimumSize = Dimension(820, 460)
        setSize(1060, 660)
        setLocationRelativeTo(null)

        updateSelectionState()
        refresh()
        table.requestFocusInWindow()
    }

    // ---------- UI construction ----------

    private fun ctrl(key: Int) = KeyStroke.getKeyStroke(key, shortcut)
    private fun alt(key: Int) = KeyStroke.getKeyStroke(key, KeyEvent.ALT_DOWN_MASK)

    private fun action(name: String, key: KeyStroke?, block: () -> Unit): AbstractAction =
        object : AbstractAction(name) {
            init {
                if (key != null) putValue(Action.ACCELERATOR_KEY, key)
            }

            override fun actionPerformed(e: ActionEvent?) = block()
        }

    private fun buildMenuBar(): JMenuBar = JMenuBar().apply {
        add(JMenu("File").apply {
            mnemonic = KeyEvent.VK_F
            add(JMenuItem(exportAction))
            addSeparator()
            add(JMenuItem(action("Exit", null) { dispose(); System.exit(0) }))
        })
        add(JMenu("Expenses").apply {
            mnemonic = KeyEvent.VK_E
            add(JMenuItem(addAction))
            add(JMenuItem(editAction))
            add(JMenuItem(deleteAction))
        })
        add(JMenu("Budgets").apply {
            mnemonic = KeyEvent.VK_B
            add(JMenuItem(setBudgetAction))
            add(JMenuItem(removeBudgetAction))
        })
        add(JMenu("View").apply {
            mnemonic = KeyEvent.VK_V
            add(JMenuItem(prevMonthAction))
            add(JMenuItem(nextMonthAction))
            add(JMenuItem(thisMonthAction))
            addSeparator()
            add(JMenuItem(allTimeAction))
        })
        add(JMenu("Help").apply {
            mnemonic = KeyEvent.VK_H
            add(JMenuItem(action("About", null) { showAbout() }))
        })
    }

    private fun buildTopBar(): JPanel {
        val tools = JToolBar().apply {
            isFloatable = false
            isRollover = true
            add(JButton(addAction))
            add(JButton(editAction))
            add(JButton(deleteAction))
        }

        val prev = JButton("<").apply { toolTipText = "Previous month (Alt+Left)"; addActionListener { shiftMonth(-1) } }
        val next = JButton(">").apply { toolTipText = "Next month (Alt+Right)"; addActionListener { shiftMonth(1) } }
        val today = JButton("This month").apply { addActionListener { setMonth(service.currentMonth()) } }
        monthLabel.font = monthLabel.font.deriveFont(Font.BOLD, 16f)
        monthLabel.preferredSize = Dimension(170, 28)
        allTimeBox.addActionListener { setMonth(if (allTimeBox.isSelected) null else service.currentMonth()) }

        val nav = JPanel(FlowLayout(FlowLayout.RIGHT, 6, 4)).apply {
            add(prev); add(monthLabel); add(next); add(today); add(allTimeBox)
        }
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color(128, 128, 128, 90))
            add(tools, BorderLayout.WEST)
            add(nav, BorderLayout.EAST)
        }
    }

    private fun buildTablePane(): JScrollPane {
        table.apply {
            autoCreateRowSorter = true
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            rowHeight = 26
            fillsViewportHeight = true
            showHorizontalLines = true
            showVerticalLines = false
            intercellSpacing = Dimension(0, 1)
            columnModel.getColumn(0).preferredWidth = 50
            columnModel.getColumn(1).preferredWidth = 100
            columnModel.getColumn(2).preferredWidth = 150
            columnModel.getColumn(3).preferredWidth = 110
            columnModel.getColumn(0).cellRenderer = PaddedRenderer(SwingConstants.RIGHT)
            columnModel.getColumn(1).cellRenderer = PaddedRenderer(SwingConstants.LEFT)
            columnModel.getColumn(2).cellRenderer = PaddedRenderer(SwingConstants.LEFT)
            columnModel.getColumn(3).cellRenderer = PaddedRenderer(SwingConstants.RIGHT, ::centsText)
            columnModel.getColumn(4).cellRenderer = PaddedRenderer(SwingConstants.LEFT)
            columnModel.getColumn(4).preferredWidth = 260
            rowSorter.sortKeys = listOf(
                RowSorter.SortKey(1, SortOrder.DESCENDING),
                RowSorter.SortKey(0, SortOrder.DESCENDING),
            )
            selectionModel.addListSelectionListener { updateSelectionState() }
        }

        val popup = JPopupMenu().apply {
            add(JMenuItem(editAction))
            add(JMenuItem(deleteAction))
        }
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && e.button == MouseEvent.BUTTON1 && table.rowAtPoint(e.point) >= 0) {
                    editSelected()
                }
            }

            override fun mousePressed(e: MouseEvent) = maybePopup(e)
            override fun mouseReleased(e: MouseEvent) = maybePopup(e)

            private fun maybePopup(e: MouseEvent) {
                if (!e.isPopupTrigger) return
                val row = table.rowAtPoint(e.point)
                if (row >= 0) {
                    table.setRowSelectionInterval(row, row)
                    popup.show(e.component, e.x, e.y)
                }
            }
        })

        return JScrollPane(table)
    }

    private fun buildSummaryPane(): JScrollPane {
        summaryContent.layout = BoxLayout(summaryContent, BoxLayout.Y_AXIS)
        summaryContent.border = BorderFactory.createEmptyBorder(14, 16, 14, 16)
        val holder = JPanel(BorderLayout()).apply { add(summaryContent, BorderLayout.NORTH) }
        return JScrollPane(holder).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            preferredSize = Dimension(360, 100)
            border = BorderFactory.createMatteBorder(0, 1, 0, 0, Color(128, 128, 128, 90))
        }
    }

    private fun buildStatusBar(): JPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color(128, 128, 128, 90)),
            BorderFactory.createEmptyBorder(4, 10, 4, 10),
        )
        add(statusLabel, BorderLayout.WEST)
    }

    // ---------- Actions ----------

    private fun addExpense() {
        val input = ExpenseDialog.ask(
            this, "Add expense", categoryNames(), initial = null, defaultDate = service.today(),
        ) ?: return
        guarded {
            val result = service.addExpense(
                ExpenseDraft(input.amount, input.category, input.date, input.note),
            )
            showMonthOf(result.expense)
            refresh(selectId = result.expense.id)
            setStatus("Saved expense #${result.expense.id}.")
            result.alert?.let { showBudgetAlert(it) }
        }
    }

    private fun editSelected() {
        val current = selectedExpense() ?: return
        val input = ExpenseDialog.ask(
            this, "Edit expense #${current.id}", categoryNames(), initial = current, defaultDate = service.today(),
        ) ?: return
        guarded {
            val result = service.updateExpense(
                current.copy(amount = input.amount, category = input.category, date = input.date, note = input.note),
            )
            if (result == null) {
                showError("This expense no longer exists.")
                refresh()
                return@guarded
            }
            showMonthOf(result.expense)
            refresh(selectId = current.id)
            setStatus("Updated expense #${current.id}.")
            result.alert?.let { showBudgetAlert(it) }
        }
    }

    private fun deleteSelected() {
        val current = selectedExpense() ?: return
        val answer = JOptionPane.showConfirmDialog(
            this,
            "Delete this expense?\n\n#${current.id}   ${current.date}   ${current.category}   ${current.amount}",
            "Delete expense",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
        )
        if (answer != JOptionPane.YES_OPTION) return
        guarded {
            service.deleteExpense(current.id)
            refresh()
            setStatus("Deleted expense #${current.id}.")
        }
    }

    private fun setBudget() {
        val budget = BudgetDialog.ask(this, categoryNames()) ?: return
        guarded {
            service.setBudget(budget)
            refresh()
            setStatus("Budget for ${budget.category} set to ${budget.monthlyLimit} per month.")
        }
    }

    private fun removeBudget() {
        guarded {
            val existing = service.summarize(service.currentMonth()).budgets.map { it.category }
            if (existing.isEmpty()) {
                JOptionPane.showMessageDialog(this, "There are no budgets to remove.", "Remove budget", JOptionPane.INFORMATION_MESSAGE)
                return@guarded
            }
            val choice = JOptionPane.showInputDialog(
                this,
                "Remove the monthly budget for:",
                "Remove budget",
                JOptionPane.QUESTION_MESSAGE,
                null,
                existing.map { it.name }.toTypedArray(),
                existing.first().name,
            ) as? String ?: return@guarded
            val category = existing.first { it.name == choice }
            service.removeBudget(category)
            refresh()
            setStatus("Removed the budget for $category.")
        }
    }

    private fun exportCsv() {
        val list = tableModel.rows
        if (list.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no expenses in this view to export.", "Export", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val suggested = "expenses-" + (month?.toString() ?: "all") + ".csv"
        val chooser = JFileChooser().apply {
            dialogTitle = "Export to CSV"
            fileFilter = FileNameExtensionFilter("CSV files (*.csv)", "csv")
            selectedFile = File(currentDirectory, suggested)
        }
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return

        var file = chooser.selectedFile
        if (!file.name.lowercase().endsWith(".csv")) file = File(file.path + ".csv")
        if (file.exists()) {
            val overwrite = JOptionPane.showConfirmDialog(
                this, "${file.name} already exists. Replace it?", "Export", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
            )
            if (overwrite != JOptionPane.YES_OPTION) return
        }
        try {
            exporter.export(list, file.toPath())
            setStatus("Exported ${list.size} expenses to ${file.absolutePath}")
        } catch (e: IOException) {
            showError("Could not write the file:\n${e.message}")
        }
    }

    private fun showAbout() {
        JOptionPane.showMessageDialog(
            this,
            "Expense Tracker 1.0\n\nAmounts are stored as exact cents in a local SQLite database:\n" +
                System.getProperty("user.home") + File.separator + ".expense-tracker" + File.separator + "expenses.db",
            "About",
            JOptionPane.INFORMATION_MESSAGE,
        )
    }

    // ---------- Month navigation ----------

    private fun setMonth(newMonth: YearMonth?) {
        month = newMonth
        refresh()
    }

    private fun shiftMonth(delta: Long) {
        val current = month ?: return
        setMonth(current.plusMonths(delta))
    }

    /** After saving, jump to the expense's month if it would not be visible in the current view. */
    private fun showMonthOf(expense: Expense) {
        val shown = month ?: return
        val target = YearMonth.from(expense.date)
        if (shown != target) month = target
    }

    // ---------- Refreshing the screen ----------

    private fun refresh(selectId: Long? = null) {
        guarded {
            val list = service.listExpenses(month)
            tableModel.setRows(list)

            val m = month
            monthLabel.text = m?.format(MONTH_TITLE) ?: "All time"
            allTimeBox.isSelected = m == null
            prevMonthAction.isEnabled = m != null
            nextMonthAction.isEnabled = m != null

            updateSummary(list, m)
            selectId?.let(::selectById)
            updateSelectionState()
        }
    }

    private fun selectById(id: Long) {
        val modelIndex = tableModel.rows.indexOfFirst { it.id == id }
        if (modelIndex < 0) return
        val viewIndex = table.convertRowIndexToView(modelIndex)
        if (viewIndex < 0) return
        table.setRowSelectionInterval(viewIndex, viewIndex)
        table.scrollRectToVisible(table.getCellRect(viewIndex, 0, true))
    }

    private fun selectedExpense(): Expense? {
        val viewRow = table.selectedRow
        if (viewRow < 0) return null
        return tableModel.rows.getOrNull(table.convertRowIndexToModel(viewRow))
    }

    private fun updateSelectionState() {
        val hasSelection = table.selectedRow >= 0
        editAction.isEnabled = hasSelection
        deleteAction.isEnabled = hasSelection
    }

    private fun updateSummary(list: List<Expense>, shownMonth: YearMonth?) {
        summaryContent.removeAll()

        val total = list.map { it.amount }.total()
        summaryContent.add(left(JLabel(if (shownMonth == null) "Total, all time" else "Total spent")))
        summaryContent.add(left(JLabel(total.toString()).apply { font = font.deriveFont(Font.BOLD, 26f) }))
        summaryContent.add(left(JLabel("${list.size} expense" + if (list.size == 1) "" else "s")))

        summaryContent.add(sectionHeader("By category"))
        if (list.isEmpty()) {
            summaryContent.add(left(JLabel("Nothing recorded.")))
        } else {
            val byCategory = list
                .groupBy { it.category }
                .map { (category, items) -> CategoryTotal(category, items.map { it.amount }.total()) }
                .sortedByDescending { it.total }
            byCategory.forEach { ct ->
                val share = ct.total.percentOf(total)
                summaryContent.add(barRow(ct.category.name, "${ct.total}  ·  $share%", share / 100.0, COLOR_SHARE, null))
            }
        }

        summaryContent.add(sectionHeader("Monthly budgets"))
        if (shownMonth == null) {
            summaryContent.add(left(JLabel("Pick a month to see budgets.")))
        } else {
            val budgets = service.summarize(shownMonth).budgets
            if (budgets.isEmpty()) {
                summaryContent.add(left(JLabel("None set (Budgets menu).")))
            }
            budgets.forEach { b ->
                val color = levelColor(b.level)
                val text = when (b.level) {
                    BudgetLevel.EXCEEDED -> "${b.spent} of ${b.limit}  ·  over by ${-b.remaining}"
                    else -> "${b.spent} of ${b.limit}  ·  ${b.percentUsed}%"
                }
                summaryContent.add(
                    barRow(b.category.name, text, b.percentUsed / 100.0, color, if (b.level == BudgetLevel.OK) null else color),
                )
            }
        }

        summaryContent.revalidate()
        summaryContent.repaint()
    }

    private fun levelColor(level: BudgetLevel): Color = when (level) {
        BudgetLevel.OK -> COLOR_OK
        BudgetLevel.WARNING -> COLOR_WARNING
        BudgetLevel.EXCEEDED -> COLOR_EXCEEDED
    }

    private fun left(c: JComponent): JComponent = c.apply { alignmentX = Component.LEFT_ALIGNMENT }

    private fun sectionHeader(title: String): JComponent = JPanel(BorderLayout(0, 6)).apply {
        alignmentX = Component.LEFT_ALIGNMENT
        border = BorderFactory.createEmptyBorder(18, 0, 6, 0)
        add(JLabel(title).apply { font = font.deriveFont(Font.BOLD, 13f) }, BorderLayout.NORTH)
        add(JSeparator(), BorderLayout.SOUTH)
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
    }

    /** "Name ........ value" on top, a bar underneath. */
    private fun barRow(name: String, value: String, fraction: Double, barColor: Color, valueColor: Color?): JComponent {
        val title = JPanel(BorderLayout(8, 0)).apply {
            isOpaque = false
            add(JLabel(name).apply { toolTipText = name }, BorderLayout.CENTER)
            add(JLabel(value).apply { valueColor?.let { foreground = it.darker() } }, BorderLayout.EAST)
        }
        return JPanel(BorderLayout(0, 3)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createEmptyBorder(4, 0, 4, 0)
            add(title, BorderLayout.NORTH)
            add(BarComponent(fraction, barColor), BorderLayout.CENTER)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
    }

    // ---------- Helpers ----------

    private fun categoryNames(): List<String> =
        try {
            service.knownCategories().map { it.name }
        } catch (e: StorageException) {
            emptyList()
        }

    private fun setStatus(text: String) {
        statusLabel.text = text
    }

    private fun showError(message: String?) {
        JOptionPane.showMessageDialog(this, message ?: "Unknown error.", "Error", JOptionPane.ERROR_MESSAGE)
    }

    private fun showBudgetAlert(s: BudgetStatus) {
        val text = when (s.level) {
            BudgetLevel.EXCEEDED -> "Budget exceeded for ${s.category}.\n\nSpent ${s.spent} of ${s.limit} (over by ${-s.remaining})."
            else -> "${s.category} has used ${s.percentUsed}% of its monthly budget.\n\n${s.remaining} left of ${s.limit}."
        }
        JOptionPane.showMessageDialog(this, text, "Budget alert", JOptionPane.WARNING_MESSAGE)
    }

    /** Runs [block]; a database failure becomes an error dialog instead of crashing the window. */
    private fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (e: StorageException) {
            showError(e.message)
        }
    }
}
