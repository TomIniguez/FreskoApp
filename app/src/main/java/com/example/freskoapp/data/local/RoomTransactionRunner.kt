package com.example.freskoapp.data.local

import androidx.room.withTransaction
import com.example.freskoapp.domain.repository.TransactionRunner

class RoomTransactionRunner(private val db: FreskoDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction(block)
}
