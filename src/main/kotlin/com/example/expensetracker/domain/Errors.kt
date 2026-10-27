package com.example.expensetracker.domain

/** Thrown when the storage layer fails (disk, corrupted DB, ...). */
class StorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)