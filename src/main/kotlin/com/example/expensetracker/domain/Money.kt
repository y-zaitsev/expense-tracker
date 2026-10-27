package com.example.expensetracker.domain

import java.math.BigDecimal

/** Amount of money stored as integer cents, so no floating-point rounding errors. */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(Math.addExact(cents, other.cents))
    operator fun minus(other: Money) = Money(Math.subtractExact(cents, other.cents))
    operator fun unaryMinus() = Money(-cents)

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    /** Integer percentage of [whole] (0 if [whole] is not positive). */
    fun percentOf(whole: Money): Int =
        if (whole.cents <= 0L) 0 else (cents * 100 / whole.cents).toInt()

    /** e.g. "12.50" (no currency symbol). */
    fun plain(): String = BigDecimal.valueOf(cents, 2).toPlainString()

    override fun toString(): String = "${plain()} €"

    companion object {
        val ZERO = Money(0)

        // Digits with an optional 1-2 digit fraction. Rejects "nan", "inf", "1e5", etc.
        private val PATTERN = Regex("""\d{1,9}([.]\d{1,2})?""")

        /** Parses user input such as "12.50" or "12,50". Must be > 0. */
        fun parsePositive(raw: String): Money {
            val text = raw.trim().replace(',', '.')
            require(PATTERN.matches(text)) {
                "Enter an amount like 12.50 (at most 9 digits and 2 decimals)."
            }
            val cents = BigDecimal(text).movePointRight(2).longValueExact()
            require(cents > 0) { "Amount must be greater than zero." }
            return Money(cents)
        }
    }
}

fun Iterable<Money>.total(): Money = fold(Money.ZERO) { acc, m -> acc + m }
