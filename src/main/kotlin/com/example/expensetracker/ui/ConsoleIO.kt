package com.example.expensetracker.ui

import java.io.BufferedReader
import java.io.InputStreamReader

/** Thrown when stdin is closed (Ctrl+D / Ctrl+Z). */
class EndOfInputException : RuntimeException()

class ConsoleIO(
    private val reader: BufferedReader = BufferedReader(InputStreamReader(System.`in`, Charsets.UTF_8)),
) {
    fun readLine(prompt: String): String {
        print(prompt)
        System.out.flush()
        return reader.readLine()?.trim() ?: throw EndOfInputException()
    }

    /**
     * Prompts until [parse] succeeds. If [default] is given, pressing Enter returns it.
     * [parse] signals bad input by throwing IllegalArgumentException.
     */
    fun <T> ask(prompt: String, default: (() -> T)? = null, parse: (String) -> T): T {
        while (true) {
            val raw = readLine(prompt)
            if (raw.isEmpty() && default != null) return default()
            try {
                return parse(raw)
            } catch (e: IllegalArgumentException) {
                println("  ! ${e.message}")
            }
        }
    }

    fun confirm(prompt: String): Boolean = ask("$prompt (y/n): ") {
        when (it.lowercase()) {
            "y", "yes" -> true
            "n", "no" -> false
            else -> throw IllegalArgumentException("Please answer y or n.")
        }
    }
}