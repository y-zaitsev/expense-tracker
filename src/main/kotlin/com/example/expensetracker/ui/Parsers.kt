package com.example.expensetracker.ui

import com.example.expensetracker.domain.MAX_NOTE_LENGTH
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

internal fun parseDate(raw: String): LocalDate =
    try {
        LocalDate.parse(raw)
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException("Use the format YYYY-MM-DD, e.g. 2026-10-27.")
    }

internal fun parseMonth(raw: String): YearMonth =
    try {
        YearMonth.parse(raw)
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException("Use the format YYYY-MM, e.g. 2026-10.")
    }

internal fun parseId(raw: String): Long =
    raw.toLongOrNull()?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("Enter a valid ID (a positive number).")

internal fun parseNote(raw: String): String {
    require(raw.length <= MAX_NOTE_LENGTH) { "Note is limited to $MAX_NOTE_LENGTH characters." }
    return raw
}