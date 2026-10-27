package com.example.expensetracker.ui.gui

import com.example.expensetracker.domain.Budget
import com.example.expensetracker.domain.Category
import com.example.expensetracker.domain.Money
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.GridBagLayout
import java.awt.event.KeyEvent
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

/** Modal form for setting a monthly budget for one category. */
internal class BudgetDialog(owner: Frame, categories: List<String>) : JDialog(owner, "Set monthly budget", true) {

    private val categoryBox = JComboBox<String>(DefaultComboBoxModel(categories.toTypedArray())).apply {
        isEditable = true
        selectedItem = ""
    }
    private val limitField = JTextField(14)
    private val errorLabel = JLabel(" ").apply { foreground = Color(0xB7, 0x1C, 0x1C) }

    var result: Budget? = null
        private set

    init {
        val form = JPanel(GridBagLayout()).apply { border = BorderFactory.createEmptyBorder(16, 18, 4, 18) }
        form.addFormRow(0, "Category", categoryBox)
        form.addFormRow(1, "Monthly limit (€)", limitField)

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
        defaultCloseOperation = DISPOSE_ON_CLOSE
        pack()
        setLocationRelativeTo(owner)
    }

    private fun submit() {
        try {
            val category = Category.of((categoryBox.editor.item ?: "").toString())
            val limit = Money.parsePositive(limitField.text)
            result = Budget(category, limit)
            dispose()
        } catch (e: IllegalArgumentException) {
            errorLabel.text = e.message ?: "Invalid input."
            pack()
        }
    }

    companion object {
        fun ask(owner: Frame, categories: List<String>): Budget? {
            val dialog = BudgetDialog(owner, categories)
            dialog.isVisible = true
            return dialog.result
        }
    }
}
