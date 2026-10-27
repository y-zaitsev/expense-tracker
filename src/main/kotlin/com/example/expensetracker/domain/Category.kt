package com.example.expensetracker.domain

/** A normalized category name: "  fOOD " and "food" both become "Food". */
@JvmInline
value class Category(val name: String) {

    init {
        require(name.isNotBlank()) { "Category cannot be empty." }
        require(name.length <= 30) { "Category is limited to 30 characters." }
    }

    override fun toString(): String = name

    companion object {
        private val WHITESPACE = Regex("\\s+")

        fun of(raw: String): Category {
            val cleaned = raw.trim()
                .replace(WHITESPACE, " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() }
            return Category(cleaned)
        }
    }
}