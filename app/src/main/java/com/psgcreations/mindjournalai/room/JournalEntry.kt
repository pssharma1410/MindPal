package com.psgcreations.mindjournalai.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.psgcreations.mindjournalai.util.Mood

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val mood: String = Mood.NEUTRAL.emoji,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)