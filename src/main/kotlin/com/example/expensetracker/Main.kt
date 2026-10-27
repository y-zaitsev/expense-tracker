package com.example.expensetracker

import com.example.expensetracker.data.Database
import com.example.expensetracker.data.SqliteBudgetRepository
import com.example.expensetracker.data.SqliteExpenseRepository
import com.example.expensetracker.domain.StorageException
import com.example.expensetracker.export.CsvExporter
import com.example.expensetracker.service.ExpenseService
import com.example.expensetracker.ui.ConsoleIO
import com.example.expensetracker.ui.ExpenseTrackerCli
import com.example.expensetracker.ui.gui.launchGui
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/**
 * Starts the windowed app by default.
 *   --cli          use the text menu instead of the window
 *   <path>         use a different database file
 */
fun main(args: Array<String>) {
    val cliMode = "--cli" in args
    val dbPath = args.firstOrNull { !it.startsWith("--") }?.let { Path.of(it) }
        ?: Path.of(System.getProperty("user.home"), ".expense-tracker", "expenses.db")

    if (cliMode) {
        // Force UTF-8 output so "€" renders correctly on Windows too.
        System.setOut(PrintStream(FileOutputStream(FileDescriptor.out), true, Charsets.UTF_8))
        System.setErr(PrintStream(FileOutputStream(FileDescriptor.err), true, Charsets.UTF_8))
    }

    try {
        Files.createDirectories(dbPath.toAbsolutePath().parent)

        // Composition root: the only place where concrete classes are wired together.
        val db = Database.at(dbPath)
        val service = ExpenseService(SqliteExpenseRepository(db), SqliteBudgetRepository(db))
        val exporter = CsvExporter()

        if (cliMode) ExpenseTrackerCli(service, ConsoleIO(), exporter).run() else launchGui(service, exporter)
    } catch (e: StorageException) {
        fatal("Could not open the database: ${e.message}", cliMode)
    } catch (e: IOException) {
        fatal("Could not create the data folder: ${e.message}", cliMode)
    }
}

private fun fatal(message: String, cliMode: Boolean): Nothing {
    if (cliMode) System.err.println("Fatal: $message")
    else JOptionPane.showMessageDialog(null, message, "Expense Tracker", JOptionPane.ERROR_MESSAGE)
    exitProcess(1)
}
