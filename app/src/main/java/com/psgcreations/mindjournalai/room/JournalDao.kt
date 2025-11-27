package com.psgcreations.mindjournalai.data

import androidx.room.*
import com.psgcreations.mindjournalai.room.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntry): Long

    @Update
    suspend fun update(entry: JournalEntry)

    @Delete
    suspend fun delete(entry: JournalEntry)

    // 🔥 MODIFIED: Sort by createdAt DESC to order by creation date
    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    fun getAll(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: Int): JournalEntry?
}