package com.example.expensetracker.data

import com.example.expensetracker.domain.Budget
import com.example.expensetracker.domain.BudgetRepository
import com.example.expensetracker.domain.Category
import com.example.expensetracker.domain.Money
import java.sql.ResultSet

class SqliteBudgetRepository(private val db: Database) : BudgetRepository {

    override fun upsert(budget: Budget) {
        db.withConnection { conn ->
            conn.prepareStatement(
                """
                INSERT INTO budgets(category, limit_cents) VALUES (?, ?)
                ON CONFLICT(category) DO UPDATE SET limit_cents = excluded.limit_cents
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, budget.category.name)
                ps.setLong(2, budget.monthlyLimit.cents)
                ps.executeUpdate()
            }
        }
    }

    override fun remove(category: Category): Boolean = db.withConnection { conn ->
        conn.prepareStatement("DELETE FROM budgets WHERE category = ?").use { ps ->
            ps.setString(1, category.name)
            ps.executeUpdate() > 0
        }
    }

    override fun find(category: Category): Budget? = db.withConnection { conn ->
        conn.prepareStatement("SELECT * FROM budgets WHERE category = ?").use { ps ->
            ps.setString(1, category.name)
            ps.executeQuery().use { rs -> if (rs.next()) rs.toBudget() else null }
        }
    }

    override fun findAll(): List<Budget> = db.withConnection { conn ->
        conn.prepareStatement("SELECT * FROM budgets ORDER BY category").use { ps ->
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(rs.toBudget())
                }
            }
        }
    }

    private fun ResultSet.toBudget() = Budget(
        category = Category.of(getString("category")),
        monthlyLimit = Money(getLong("limit_cents")),
    )
}
