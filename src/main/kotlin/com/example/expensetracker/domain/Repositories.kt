package com.example.expensetracker.domain

import java.time.YearMonth

interface ExpenseRepository {
    fun add(draft: ExpenseDraft): Expense
    fun update(expense: Expense): Boolean
    fun delete(id: Long): Boolean
    fun findById(id: Long): Expense?
    fun findAll(): List<Expense>
    fun findByMonth(month: YearMonth): List<Expense>
}

interface BudgetRepository {
    fun upsert(budget: Budget)
    fun remove(category: Category): Boolean
    fun find(category: Category): Budget?
    fun findAll(): List<Budget>
}