package com.psgcreations.mindjournalai.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psgcreations.mindjournalai.room.JournalEntry
import com.psgcreations.mindjournalai.repository.JournalRepository
import com.psgcreations.mindjournalai.repository.AuthRepository // IMPORT THIS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repo: JournalRepository,
    private val authRepo: AuthRepository // INJECT THIS
) : ViewModel() {

    // Expose entries as StateFlow for Compose consumption
    val entries: StateFlow<List<JournalEntry>> =
        repo.getAllEntries()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- NEW: Sync FCM Token ---
    fun checkFcmToken() {
        viewModelScope.launch {
            // This ensures the token is synced to Firestore even if the user
            // was already logged in (auto-login)
            authRepo.retrieveAndSaveFcmToken()
        }
    }
    // ---------------------------

    fun addEntry(title: String, content: String) {
        viewModelScope.launch {
            val entry = JournalEntry(title = title.trim(), content = content.trim())
            repo.insert(entry)
        }
    }

    fun updateEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repo.update(entry.copy(content = entry.content, title = entry.title))
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repo.delete(entry)
        }
    }

    suspend fun getEntryById(id: Int): JournalEntry? = repo.getById(id)

    fun getIndexForId(id: Int): Int {
        return entries.value.indexOfFirst { it.id == id }
    }

    /**
     * Returns Pair(prevIndex, nextIndex) relative to given index.
     * If previous/next doesn't exist, that side will be -1.
     */
    fun getAdjacentIndices(index: Int): Pair<Int, Int> {
        if (index < 0 || index >= entries.value.size) return Pair(-1, -1)
        val prev = if (index - 1 >= 0) index - 1 else -1
        val next = if (index + 1 < entries.value.size) index + 1 else -1
        return Pair(prev, next)
    }

    /**
     * Get entry at index or null.
     */
    suspend fun getEntryAtIndex(index: Int): JournalEntry? {
        val list = entries.value
        return if (index in list.indices) list[index] else null
    }
}