package com.psgcreations.mindjournalai.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.psgcreations.mindjournalai.data.JournalDao
import com.psgcreations.mindjournalai.util.Mood // Used for default value

@Database(entities = [JournalEntry::class], version = 2, exportSchema = false)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}

/**
 * Migration from version 1 to 2.
 * This handles the following schema changes:
 * 1. Renames the old 'timestamp' column (used for creation/update time in v1) to 'updatedAt'.
 * 2. Adds the new 'mood' column with a non-null default value ('😐').
 * 3. Adds the new 'createdAt' column.
 * 4. Updates 'createdAt' to match the old timestamp value for all existing entries,
 * thus preserving the original creation date.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // 1. RENAME COLUMN: Rename the existing 'timestamp' column to 'updatedAt'.
        // This is possible because the column type (INTEGER/Long) remains the same.
        database.execSQL("ALTER TABLE journal_entries RENAME COLUMN timestamp TO updatedAt")

        // 2. ADD COLUMN: Add 'mood' column. Must be NOT NULL with a DEFAULT value.
        database.execSQL("ALTER TABLE journal_entries ADD COLUMN mood TEXT NOT NULL DEFAULT '😐'")

        // 3. ADD COLUMN: Add 'createdAt' column. Must be NOT NULL with a temporary DEFAULT value (0).
        // The value will be corrected in the next step.
        database.execSQL("ALTER TABLE journal_entries ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")

        // 4. UPDATE DATA: Set the new 'createdAt' column for existing rows
        // using the data stored in 'updatedAt' (which holds the original timestamp from v1).
        database.execSQL("UPDATE journal_entries SET createdAt = updatedAt WHERE createdAt = 0")
    }
}
// Note: This MIGRATION_1_2 object must be added to your Room database builder
// (e.g., in your application or DI module) using .addMigrations(MIGRATION_1_2).