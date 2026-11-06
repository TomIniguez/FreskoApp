package com.example.freskoapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// v1 -> v2: logical status for RF04. Existing rows stay active.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
    }
}

// v2 -> v3: creation timestamp (epoch millis). Existing rows get 0 = unknown, the UI hides it.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
    }
}

// v3 -> v4: local pilot event log. New table only; existing rows untouched. SQL copied from schemas/4.json.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `product_event` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`productId` INTEGER, `type` TEXT NOT NULL, `units` INTEGER, `daysToExpiry` INTEGER, `method` TEXT, " +
                "`durationMs` INTEGER, `timestamp` INTEGER NOT NULL)"
        )
    }
}
