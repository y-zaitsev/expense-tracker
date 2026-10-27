package com.example.expensetracker.domain

import java.time.LocalDate
import java.time.YearMonth

const val MAX_NOTE_LENGTH = 100

/** An expense that has not been stored yet (no id). */
data class ExpenseDraft(
    val amount: Money,
    val category: Category,
    val date: LocalDate,
    val note: String = "",
) {
    init {
        require(amount.cents > 0) { "Amount must be greater than zero." }
        require(note.length <= MAX_NOTE_LENGTH) { "Note is limited to $MAX_NOTE_LENGTH characters." }
    }
}

/** A stored expense. */
data class Expense(
    val id: Long,
    val amount: Money,
    val category: Category,
    val date: LocalDate,
    val note: String = "",
) {
    init {
        require(id > 0) { "Invalid id." }
        require(amount.cents > 0) { "Amount must be greater than zero." }
        require(note.length <= MAX_NOTE_LENGTH) { "Note is limited to $MAX_NOTE_LENGTH characters." }
    }
}

data class Budget(val category: Category, val monthlyLimit: Money) {
    init {
        require(monthlyLimit.cents > 0) { "Budget must be greater than zero." }
    }
}

enum class BudgetLevel { OK, WARNING, EXCEEDED }

/** How much of a category's monthly budget has been used. */
data class BudgetStatus(val category: Category, val limit: Money, val spent: Money) {
    val remaining: Money get() = limit - spent
    val percentUsed: Int get() = spent.percentOf(limit)
    val level: BudgetLevel
        get() = when {
            spent > limit -> BudgetLevel.EXCEEDED
            percentUsed >= WARNING_THRESHOLD_PERCENT -> BudgetLevel.WARNING
            else -> BudgetLevel.OK
        }

    companion object {
        const val WARNING_THRESHOLD_PERCENT = 80
    }
}

data class CategoryTotal(val category: Category, val total: Money)

data class MonthlySummary(
    val month: YearMonth,
    val total: Money,
    val byCategory: List<CategoryTotal>,
    val budgets: List<BudgetStatus>,
)

/** Result of adding or editing an expense; [alert] is set when a budget is near or over its limit. */
data class SaveResult(val expense: Expense, val alert: BudgetStatus?)