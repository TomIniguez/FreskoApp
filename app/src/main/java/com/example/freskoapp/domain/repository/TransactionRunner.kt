package com.example.freskoapp.domain.repository

// Runs repository calls atomically, so a product change and its event are written (or rolled back) together.
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}
