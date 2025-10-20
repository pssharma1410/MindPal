package com.psgcreations.mindjournalai.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.psgcreations.mindjournalai.data.JournalDao

@Database(entities = [JournalEntry::class], version = 1, exportSchema = false)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}
