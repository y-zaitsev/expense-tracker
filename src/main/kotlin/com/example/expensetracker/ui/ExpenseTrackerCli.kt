package com.example.expensetracker.ui

import com.example.expensetracker.domain.Budget
import com.example.expensetracker.domain.BudgetLevel
import com.example.expensetracker.domain.BudgetStatus
import com.example.expensetracker.domain.Category
import com.example.expensetracker.domain.ExpenseDraft
import com.example.expensetracker.domain.Money
import com.example.expensetracker.domain.StorageException
import com.example.expensetracker.domain.total
import com.example.expensetracker.domain.Expense
import com.example.expensetracker.export.CsvExporter
import com.example.expensetracker.service.ExpenseService
import java.io.IOException
import java.nio.file.Path
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val BAR_WIDTH = 20
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

private fun fmt(pattern: String, vararg args: Any?): String = String.format(Locale.ROOT, pattern, *args)

private fun fit(text: String, width: Int): String =
    if (text.length <= width) text else text.take(width - 1) + "…"

private fun bar(percent: Int): String {
    val filled = percent.coerceIn(0, 100) * BAR_WIDTH / 100
    return "#".repeat(filled) + ".".repeat(BAR_WIDTH - filled)
}

class ExpenseTrackerCli(
    private val service: ExpenseService,
    private val io: ConsoleIO,
    private val exporter: CsvExporter,
) {
    fun run() {
        println("Welcome to Expense Tracker!")
        try {
            while (true) {
                printMenu()
                val choice = io.readLine("Choose an option: ")
                val keepGoing = try {
                    dispatch(choice)
                } catch (e: StorageException) {
                    println("  ! ${e.message}")
                    true
                }
                if (!keepGoing) break
            }
        } catch (e: EndOfInputException) {
            println()
        }
        println("Goodbye!")
    }

    private fun printMenu() {
        println(
            """
            
            === Expense Tracker ===
            1. Add expense
            2. List expenses
            3. Edit expense
            4. Delete expense
            5. Monthly summary
            6. Set monthly budget
            7. Remove budget
            8. Export to CSV
            9. Exit
            """.trimIndent()
        )
    }

    private fun dispatch(choice: String): Boolean {
        when (choice) {
            "1" -> addExpense()
            "2" -> listExpenses()
            "3" -> editExpense()
            "4" -> deleteExpense()
            "5" -> showSummary()
            "6" -> setBudget()
            "7" -> removeBudget()
            "8" -> exportCsv()
            "9" -> return false
            else -> println("Invalid option, please enter a number from 1 to 9.")
        }
        return true
    }

    // ---------- Actions ----------

    private fun addExpense() {
        val amount = io.ask("Amount (€): ") { Money.parsePositive(it) }
        val category = io.ask("Category (e.g. Food): ") { Category.of(it) }
        val date = io.ask("Date (YYYY-MM-DD, Enter = today): ", default = { service.today() }, parse = ::parseDate)
        val note = io.ask("Note (optional): ", default = { "" }) { parseNote(it) }

        val result = service.addExpense(ExpenseDraft(amount, category, date, note))
        println("Saved expense #${result.expense.id}.")
        result.alert?.let(::printAlert)
    }

    private fun listExpenses() {
        val month = askMonthFilter()
        val list = service.listExpenses(month)
        if (list.isEmpty()) {
            println("No expenses found.")
            return
        }
        println()
        printTable(list)
        println("Total: ${list.map { it.amount }.total()}")
    }

    private fun editExpense() {
        val id = io.ask("Expense ID to edit: ", parse = ::parseId)
        val current = service.findExpense(id)
        if (current == null) {
            println("  ! No expense with ID $id.")
            return
        }
        println("Press Enter to keep the current value.")
        val amount = io.ask("Amount [${current.amount.plain()}]: ", default = { current.amount }) {
            Money.parsePositive(it)
        }
        val category = io.ask("Category [${current.category}]: ", default = { current.category }) {
            Category.of(it)
        }
        val date = io.ask("Date [${current.date}]: ", default = { current.date }, parse = ::parseDate)
        val note = io.ask("Note [${current.note}] (- to clear): ", default = { current.note }) {
            if (it == "-") "" else parseNote(it)
        }

        val result = service.updateExpense(current.copy(amount = amount, category = category, date = date, note = note))
        if (result == null) {
            println("  ! The expense no longer exists.")
        } else {
            println("Expense #$id updated.")
            result.alert?.let(::printAlert)
        }
    }

    private fun deleteExpense() {
        val id = io.ask("Expense ID to delete: ", parse = ::parseId)
        val current = service.findExpense(id)
        if (current == null) {
            println("  ! No expense with ID $id.")
            return
        }
        println("  #${current.id}  ${current.date}  ${current.category}  ${current.amount}  ${current.note}")
        if (io.confirm("Delete this expense?")) {
            service.deleteExpense(id)
            println("Deleted.")
        } else {
            println("Cancelled.")
        }
    }

    private fun showSummary() {
        val month = io.ask("Month (YYYY-MM, Enter = current): ", default = { service.currentMonth() }, parse = ::parseMonth)
        val summary = service.summarize(month)

        println("\n=== ${summary.month.format(MONTH_FORMAT)} ===")
        if (summary.byCategory.isEmpty()) {
            println("No expenses recorded this month.")
        } else {
            println("Total: ${summary.total}\n")
            summary.byCategory.forEach { ct ->
                val share = ct.total.percentOf(summary.total)
                println(fmt("  %-16s %12s %4d%%  %s", fit(ct.category.name, 16), ct.total, share, bar(share)))
            }
        }

        if (summary.budgets.isNotEmpty()) {
            println("\nBudgets:")
            summary.budgets.forEach { b ->
                println(
                    fmt(
                        "  %-16s %12s of %-12s %4d%%  %s %s",
                        fit(b.category.name, 16), b.spent, b.limit, b.percentUsed, bar(b.percentUsed), levelTag(b.level),
                    )
                )
            }
        }
    }

    private fun setBudget() {
        val category = io.ask("Category: ") { Category.of(it) }
        val limit = io.ask("Monthly limit (€): ") { Money.parsePositive(it) }
        service.setBudget(Budget(category, limit))
        println("Budget for $category set to $limit per month.")
    }

    private fun removeBudget() {
        val category = io.ask("Category: ") { Category.of(it) }
        println(if (service.removeBudget(category)) "Budget removed." else "  ! No budget for $category.")
    }

    private fun exportCsv() {
        val month = askMonthFilter()
        val list = service.listExpenses(month)
        if (list.isEmpty()) {
            println("Nothing to export.")
            return
        }
        val target = io.ask("File path (Enter = expenses-export.csv): ", default = { Path.of("expenses-export.csv") }) {
            Path.of(it)
        }
        try {
            exporter.export(list, target)
            println("Exported ${list.size} expenses to ${target.toAbsolutePath()}")
        } catch (e: IOException) {
            println("  ! Could not write file: ${e.message}")
        }
    }

    // ---------- Helpers ----------

    private fun askMonthFilter(): YearMonth? =
        io.ask<YearMonth?>(
            "Month (YYYY-MM, Enter = current, 'all' = everything): ",
            default = { service.currentMonth() },
        ) { if (it.equals("all", ignoreCase = true)) null else parseMonth(it) }

    private fun printTable(list: List<Expense>) {
        println(fmt("%-5s %-10s %-16s %12s  %s", "ID", "Date", "Category", "Amount", "Note"))
        println("-".repeat(72))
        list.forEach { e ->
            println(fmt("%-5d %-10s %-16s %12s  %s", e.id, e.date, fit(e.category.name, 16), e.amount, fit(e.note, 30)))
        }
    }

    private fun printAlert(s: BudgetStatus) {
        when (s.level) {
            BudgetLevel.EXCEEDED ->
                println("  !! Budget exceeded for ${s.category}: ${s.spent} spent of ${s.limit} (over by ${-s.remaining}).")
            BudgetLevel.WARNING ->
                println("  !  ${s.category} budget at ${s.percentUsed}%: ${s.remaining} left of ${s.limit}.")
            BudgetLevel.OK -> Unit
        }
    }

    private fun levelTag(level: BudgetLevel) = when (level) {
        BudgetLevel.OK -> ""
        BudgetLevel.WARNING -> "<- nearing limit"
        BudgetLevel.EXCEEDED -> "<- OVER BUDGET"
    }
}