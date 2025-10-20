package com.psgcreations.mindjournalai.repository

import com.psgcreations.mindjournalai.data.JournalDao
import com.psgcreations.mindjournalai.room.JournalEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val dao: JournalDao
) {
    fun getAllEntries(): Flow<List<JournalEntry>> = dao.getAll()
    suspend fun insert(entry: JournalEntry) = dao.insert(entry)
    suspend fun update(entry: JournalEntry) = dao.update(entry)
    suspend fun delete(entry: JournalEntry) = dao.delete(entry)
    suspend fun getById(id: Int): JournalEntry? = dao.getById(id)
}
