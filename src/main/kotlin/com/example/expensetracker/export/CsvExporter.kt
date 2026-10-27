package com.example.expensetracker.export

import com.example.expensetracker.domain.Expense
import java.nio.file.Files
import java.nio.file.Path

/**
 * Writes expenses as CSV. Defaults suit European Excel: ';' delimiter, decimal comma, UTF-8 BOM.
 * Pass ',' as delimiter for the international format (decimal point).
 */
class CsvExporter(private val delimiter: Char = ';') {

    fun export(expenses: List<Expense>, target: Path) {
        target.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        Files.newBufferedWriter(target, Charsets.UTF_8).use { out ->
            out.write("\uFEFF") // BOM so Excel detects UTF-8 (needed for the € and umlauts)
            out.write(row("id", "date", "category", "amount", "note"))
            for (e in expenses) {
                out.write(
                    row(
                        e.id.toString(),
                        e.date.toString(),
                        defuse(e.category.name),
                        amountText(e.amount.plain()),
                        defuse(e.note),
                    )
                )
            }
        }
    }

    private fun amountText(plain: String) = if (delimiter == ';') plain.replace('.', ',') else plain

    private fun row(vararg fields: String): String =
        fields.joinToString(delimiter.toString()) { escape(it) } + "\r\n"

    private fun escape(field: String): String {
        val needsQuotes = field.any { it == delimiter || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + field.replace("\"", "\"\"") + "\"" else field
    }

    /** Prevents spreadsheet formula injection (a note like "=HYPERLINK(...)"). */
    private fun defuse(text: String): String =
        if (text.isNotEmpty() && text[0] in "=+-@\t\r") "'$text" else text
}
