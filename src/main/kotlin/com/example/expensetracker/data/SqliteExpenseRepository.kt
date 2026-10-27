package com.example.expensetracker.data

import com.example.expensetracker.domain.Category
import com.example.expensetracker.domain.Expense
import com.example.expensetracker.domain.ExpenseDraft
import com.example.expensetracker.domain.ExpenseRepository
import com.example.expensetracker.domain.Money
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.time.LocalDate
import java.time.YearMonth

class SqliteExpenseRepository(private val db: Database) : ExpenseRepository {

    override fun add(draft: ExpenseDraft): Expense = db.withConnection { conn ->
        conn.prepareStatement(
            "INSERT INTO expenses(amount_cents, category, date, note) VALUES (?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, draft.amount.cents)
            ps.setString(2, draft.category.name)
            ps.setString(3, draft.date.toString())
            ps.setString(4, draft.note)
            ps.executeUpdate()
            ps.generatedKeys.use { keys ->
                check(keys.next()) { "No generated id returned" }
                Expense(keys.getLong(1), draft.amount, draft.category, draft.date, draft.note)
            }
        }
    }

    override fun update(expense: Expense): Boolean = db.withConnection { conn ->
        conn.prepareStatement(
            "UPDATE expenses SET amount_cents = ?, category = ?, date = ?, note = ? WHERE id = ?"
        ).use { ps ->
            ps.setLong(1, expense.amount.cents)
            ps.setString(2, expense.category.name)
            ps.setString(3, expense.date.toString())
            ps.setString(4, expense.note)
            ps.setLong(5, expense.id)
            ps.executeUpdate() > 0
        }
    }

    override fun delete(id: Long): Boolean = db.withConnection { conn ->
        conn.prepareStatement("DELETE FROM expenses WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeUpdate() > 0
        }
    }

    override fun findById(id: Long): Expense? =
        query("SELECT * FROM expenses WHERE id = ?") { it.setLong(1, id) }.firstOrNull()

    override fun findAll(): List<Expense> =
        query("SELECT * FROM expenses ORDER BY date, id")

    override fun findByMonth(month: YearMonth): List<Expense> =
        query("SELECT * FROM expenses WHERE date >= ? AND date <= ? ORDER BY date, id") {
            it.setString(1, month.atDay(1).toString())
            it.setString(2, month.atEndOfMonth().toString())
        }

    private fun query(sql: String, bind: (PreparedStatement) -> Unit = {}): List<Expense> =
        db.withConnection { conn ->
            conn.prepareStatement(sql).use { ps ->
                bind(ps)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(rs.toExpense())
                    }
                }
            }
        }

    private fun ResultSet.toExpense() = Expense(
        id = getLong("id"),
        amount = Money(getLong("amount_cents")),
        category = Category.of(getString("category")),
        date = LocalDate.parse(getString("date")),
        note = getString("note") ?: "",
    )
}