package com.example.expensetracker.service

import com.example.expensetracker.domain.Budget
import com.example.expensetracker.domain.BudgetLevel
import com.example.expensetracker.domain.BudgetRepository
import com.example.expensetracker.domain.BudgetStatus
import com.example.expensetracker.domain.Category
import com.example.expensetracker.domain.CategoryTotal
import com.example.expensetracker.domain.Expense
import com.example.expensetracker.domain.ExpenseDraft
import com.example.expensetracker.domain.ExpenseRepository
import com.example.expensetracker.domain.MonthlySummary
import com.example.expensetracker.domain.SaveResult
import com.example.expensetracker.domain.total
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/** All business rules live here; the UI and the database are interchangeable around it. */
class ExpenseService(
    private val expenses: ExpenseRepository,
    private val budgets: BudgetRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun today(): LocalDate = LocalDate.now(clock)
    fun currentMonth(): YearMonth = YearMonth.from(today())

    fun addExpense(draft: ExpenseDraft): SaveResult {
        val saved = expenses.add(draft)
        return SaveResult(saved, alertFor(saved.category, YearMonth.from(saved.date)))
    }

    /** Returns null when the expense does not exist. */
    fun updateExpense(expense: Expense): SaveResult? {
        if (!expenses.update(expense)) return null
        return SaveResult(expense, alertFor(expense.category, YearMonth.from(expense.date)))
    }

    fun deleteExpense(id: Long): Boolean = expenses.delete(id)

    fun findExpense(id: Long): Expense? = expenses.findById(id)

    /** [month] = null means "all expenses". */
    fun listExpenses(month: YearMonth?): List<Expense> =
        if (month == null) expenses.findAll() else expenses.findByMonth(month)

    fun summarize(month: YearMonth): MonthlySummary {
        val monthExpenses = expenses.findByMonth(month)
        val byCategory = monthExpenses
            .groupBy { it.category }
            .map { (category, items) -> CategoryTotal(category, items.map { it.amount }.total()) }
            .sortedByDescending { it.total }
        val statuses = budgets.findAll().map { budget ->
            BudgetStatus(budget.category, budget.monthlyLimit, spentIn(monthExpenses, budget.category))
        }
        return MonthlySummary(month, monthExpenses.map { it.amount }.total(), byCategory, statuses)
    }

    /** Every category used by an expense or a budget, sorted by name (for input suggestions). */
    fun knownCategories(): List<Category> =
        (expenses.findAll().map { it.category } + budgets.findAll().map { it.category })
            .distinct()
            .sortedBy { it.name }

    fun setBudget(budget: Budget) = budgets.upsert(budget)

    fun removeBudget(category: Category): Boolean = budgets.remove(category)

    private fun spentIn(list: List<Expense>, category: Category) =
        list.filter { it.category == category }.map { it.amount }.total()

    private fun alertFor(category: Category, month: YearMonth): BudgetStatus? {
        val budget = budgets.find(category) ?: return null
        val spent = spentIn(expenses.findByMonth(month), category)
        return BudgetStatus(category, budget.monthlyLimit, spent).takeIf { it.level != BudgetLevel.OK }
    }
}