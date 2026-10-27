package com.example.expensetracker

import com.example.expensetracker.data.Database
import com.example.expensetracker.data.SqliteBudgetRepository
import com.example.expensetracker.data.SqliteExpenseRepository
import com.example.expensetracker.domain.Budget
import com.example.expensetracker.domain.BudgetLevel
import com.example.expensetracker.domain.Category
import com.example.expensetracker.domain.ExpenseDraft
import com.example.expensetracker.domain.Money
import com.example.expensetracker.service.ExpenseService
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ExpenseTrackerTest {

    private lateinit var dbFile: Path
    private lateinit var service: ExpenseService

    @BeforeTest
    fun setUp() {
        dbFile = Files.createTempFile("expenses-test", ".db")
        val db = Database.at(dbFile)
        service = ExpenseService(SqliteExpenseRepository(db), SqliteBudgetRepository(db))
    }

    @AfterTest
    fun tearDown() {
        Files.deleteIfExists(dbFile)
    }

    private fun draft(category: String, cents: Long, date: String) =
        ExpenseDraft(Money(cents), Category.of(category), LocalDate.parse(date))

    @Test
    fun `money parsing rejects nan, inf, zero, negatives and too many decimals`() {
        listOf("nan", "inf", "0", "-5", "abc", "1.234", "").forEach {
            assertFailsWith<IllegalArgumentException>(it) { Money.parsePositive(it) }
        }
    }

    @Test
    fun `money parsing accepts decimal comma and point`() {
        assertEquals(Money(1250), Money.parsePositive("12,50"))
        assertEquals(Money(1250), Money.parsePositive("12.5"))
        assertEquals("12.50", Money(1250).plain())
    }

    @Test
    fun `category is normalized`() {
        assertEquals("Food", Category.of("  fOOD ").name)
    }

    @Test
    fun `budget alert escalates from none to warning to exceeded`() {
        service.setBudget(Budget(Category.of("Food"), Money(10_000)))

        assertNull(service.addExpense(draft("Food", 7_900, "2026-10-05")).alert)
        assertEquals(BudgetLevel.WARNING, service.addExpense(draft("Food", 200, "2026-10-06")).alert?.level)
        assertEquals(BudgetLevel.EXCEEDED, service.addExpense(draft("Food", 2_000, "2026-10-07")).alert?.level)
    }

    @Test
    fun `summary only counts the requested month and sorts by total`() {
        service.addExpense(draft("Food", 1_000, "2026-10-01"))
        service.addExpense(draft("Food", 500, "2026-10-15"))
        service.addExpense(draft("Fun", 2_500, "2026-10-20"))
        service.addExpense(draft("Food", 999, "2026-11-01"))

        val summary = service.summarize(YearMonth.of(2026, 10))

        assertEquals(Money(4_000), summary.total)
        assertEquals(Category.of("Fun"), summary.byCategory.first().category)
        assertEquals(Money(1_500), summary.byCategory.last().total)
    }

    @Test
    fun `update and delete work`() {
        val saved = service.addExpense(draft("Food", 1_000, "2026-10-01")).expense
        service.updateExpense(saved.copy(amount = Money(2_000)))
        assertEquals(Money(2_000), service.findExpense(saved.id)?.amount)

        assertEquals(true, service.deleteExpense(saved.id))
        assertNull(service.findExpense(saved.id))
    }
}