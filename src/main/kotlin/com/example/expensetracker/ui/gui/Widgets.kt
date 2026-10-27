package com.example.expensetracker.ui.gui

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.Insets
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/** A flat horizontal bar. Painted by hand so the colours look the same in every look-and-feel. */
internal class BarComponent(fraction: Double, private val fill: Color) : JComponent() {
    private val fraction = fraction.coerceIn(0.0, 1.0)

    init {
        preferredSize = Dimension(120, 12)
        minimumSize = Dimension(40, 12)
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = Color(128, 128, 128, 60)
            g2.fillRoundRect(0, 0, width, height, height, height)
            val filled = (width * fraction).toInt()
            if (filled > 0) {
                g2.color = fill
                g2.fillRoundRect(0, 0, maxOf(filled, height).coerceAtMost(width), height, height, height)
            }
        } finally {
            g2.dispose()
        }
    }
}

/** Adds a "label  [field]" row to a panel that uses GridBagLayout. */
internal fun JPanel.addFormRow(row: Int, label: String, field: JComponent) {
    add(
        JLabel(label),
        GridBagConstraints().apply {
            gridx = 0; gridy = row
            anchor = GridBagConstraints.LINE_START
            insets = Insets(5, 0, 5, 12)
        },
    )
    add(
        field,
        GridBagConstraints().apply {
            gridx = 1; gridy = row
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = Insets(5, 0, 5, 0)
        },
    )
}
