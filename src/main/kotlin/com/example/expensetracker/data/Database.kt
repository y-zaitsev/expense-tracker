package com.example.expensetracker.data

import com.example.expensetracker.domain.StorageException
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/** Opens a short-lived connection per operation (simple and safe for a single-user app). */
class Database(private val url: String) {

    init {
        migrate()
    }

    fun <T> withConnection(block: (Connection) -> T): T =
        try {
            DriverManager.getConnection(url).use(block)
        } catch (e: SQLException) {
            throw StorageException("Database error: ${e.message}", e)
        }

    private fun migrate() {
        withConnection { conn ->
            conn.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS expenses (
                        id           INTEGER PRIMARY KEY AUTOINCREMENT,
                        amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
                        category     TEXT    NOT NULL,
                        date         TEXT    NOT NULL,
                        note         TEXT    NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(date)")
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS budgets (
                        category    TEXT PRIMARY KEY,
                        limit_cents INTEGER NOT NULL CHECK (limit_cents > 0)
                    )
                    """.trimIndent()
                )
            }
        }
    }

    companion object {
        fun at(path: Path): Database =
            Database("jdbc:sqlite:" + path.toAbsolutePath().toString().replace('\\', '/'))
    }
}
