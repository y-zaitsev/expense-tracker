package com.example.expensetracker.ui.gui

import com.example.expensetracker.export.CsvExporter
import com.example.expensetracker.service.ExpenseService
import javax.swing.SwingUtilities
import javax.swing.UIManager

/** Opens the main window on the Swing event thread, using the native look of the operating system. */
fun launchGui(service: ExpenseService, exporter: CsvExporter) {
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            // Keep the default look-and-feel; the app works the same.
        }
        MainWindow(service, exporter).isVisible = true
    }
}
