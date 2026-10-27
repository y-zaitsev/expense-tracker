package com.example.expensetracker.ui.gui

import com.example.expensetracker.domain.Category
import com.example.expensetracker.domain.Expense
import com.example.expensetracker.domain.ExpenseDraft
import com.example.expensetracker.domain.Money
import com.example.expensetracker.ui.parseDate
import com.example.expensetracker.ui.parseNote
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.GridBagLayout
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.time.LocalDate
import javax.swing.BorderFactory
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.KeyStroke

/** Validated values from the "Add / Edit expense" form. */
internal data class ExpenseInput(val amount: Money, val category: Category, val date: LocalDate, val note: String)

/** Modal form for adding a new expense or editing an existing one. */
internal class ExpenseDialog(
    owner: Frame,
    title: String,
    categories: List<String>,
    initial: Expense?,
    defaultDate: LocalDate,
) : JDialog(owner, title, true) {

    private val amountField = JTextField(initial?.amount?.plain() ?: "", 14)
    private val categoryBox = JComboBox<String>(DefaultComboBoxModel(categories.toTypedArray())).apply {
        isEditable = true
        selectedItem = initial?.category?.name ?: ""
    }
    private val dateField = JTextField((initial?.date ?: defaultDate).toString(), 14)
    private val noteField = JTextField(initial?.note ?: "", 24)
    private val errorLabel = JLabel(" ").apply { foreground = Color(0xB7, 0x1C, 0x1C) }

    var result: ExpenseInput? = null
        private set

    init {
        val form = JPanel(GridBagLayout()).apply { border = BorderFactory.createEmptyBorder(16, 18, 4, 18) }
        form.addFormRow(0, "Amount (€)", amountField)
        form.addFormRow(1, "Category", categoryBox)
        form.addFormRow(2, "Date (YYYY-MM-DD)", dateField)
        form.addFormRow(3, "Note (optional)", noteField)

        val ok = JButton("Save").apply { addActionListener { submit() } }
        val cancel = JButton("Cancel").apply { addActionListener { dispose() } }
        val south = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(0, 18, 12, 18)
            add(errorLabel, BorderLayout.NORTH)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8)).apply { add(ok); add(cancel) }, BorderLayout.CENTER)
        }

        contentPane.add(form, BorderLayout.CENTER)
        contentPane.add(south, BorderLayout.SOUTH)
        rootPane.defaultButton = ok
        rootPane.registerKeyboardAction(
            { dispose() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW,
        )
        addWindowListener(object : WindowAdapter() {
            override fun windowOpened(e: WindowEvent?) {
                amountField.requestFocusInWindow()
                amountField.selectAll()
            }
        })
        defaultCloseOperation = DISPOSE_ON_CLOSE
        pack()
        setLocationRelativeTo(owner)
    }

    private fun submit() {
        try {
            val amount = Money.parsePositive(amountField.text)
            val category = Category.of((categoryBox.editor.item ?: "").toString())
            val date = parseDate(dateField.text.trim())
            val note = parseNote(noteField.text.trim())
            ExpenseDraft(amount, category, date, note) // runs the domain validation once more
            result = ExpenseInput(amount, category, date, note)
            dispose()
        } catch (e: IllegalArgumentException) {
            errorLabel.text = e.message ?: "Invalid input."
            pack()
        }
    }

    companion object {
        fun ask(
            owner: Frame,
            title: String,
            categories: List<String>,
            initial: Expense?,
            defaultDate: LocalDate,
        ): ExpenseInput? {
            val dialog = ExpenseDialog(owner, title, categories, initial, defaultDate)
            dialog.isVisible = true // blocks until closed (modal)
            return dialog.result
        }
    }
}
