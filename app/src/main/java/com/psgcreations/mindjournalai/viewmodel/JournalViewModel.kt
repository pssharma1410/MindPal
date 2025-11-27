package com.psgcreations.mindjournalai.ui.journal

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psgcreations.mindjournalai.repository.AuthRepository
import com.psgcreations.mindjournalai.repository.JournalRepository
import com.psgcreations.mindjournalai.room.JournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.psgcreations.mindjournalai.util.Mood // Assuming this import exists

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repo: JournalRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    // All entries as StateFlow for Compose
    val entries: StateFlow<List<JournalEntry>> =
        repo.getAllEntries()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- FCM TOKEN SYNC ---
    fun checkFcmToken() {
        viewModelScope.launch {
            authRepo.retrieveAndSaveFcmToken()
        }
    }

    // --- CRUD OPERATIONS ---
    fun addEntry(title: String, content: String, mood: String) {
        viewModelScope.launch {
            val entry = JournalEntry(
                title = title.trim(),
                content = content.trim(),
                mood = mood
                // createdAt and updatedAt are set by the default values in the data class
            )
            repo.insert(entry)
        }
    }

    // 🔥 MODIFIED: Explicitly set updatedAt to current time
    fun updateEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repo.update(
                entry.copy(
                    content = entry.content,
                    title = entry.title,
                    // Note: createdAt remains the original value
                    updatedAt = System.currentTimeMillis() // 🔥 Update modification time
                )
            )
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

    fun getAdjacentIndices(index: Int): Pair<Int, Int> {
        if (index < 0 || index >= entries.value.size) return Pair(-1, -1)
        val prev = if (index - 1 >= 0) index - 1 else -1
        val next = if (index + 1 < entries.value.size) index + 1 else -1
        return Pair(prev, next)
    }

    suspend fun getEntryAtIndex(index: Int): JournalEntry? {
        val list = entries.value
        return if (index in list.indices) list[index] else null
    }

    // --- MULTI-SELECTION STATE ---
    private val _selectedIds = mutableStateListOf<Int>()
    val selectedIds: List<Int> get() = _selectedIds

    var isSelectionMode by mutableStateOf(false)
        private set

    fun startSelection(id: Int) {
        Log.d("viewModel123", "startSelection: ")
        if (!isSelectionMode) {
            isSelectionMode = true
        }
        if (!_selectedIds.contains(id)) {
            _selectedIds.add(id)
        }
    }

    fun toggleSelection(id: Int) {
        if (_selectedIds.contains(id)) {
            _selectedIds.remove(id)
            if (_selectedIds.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            _selectedIds.add(id)
            if (!isSelectionMode) {
                isSelectionMode = true
            }
        }
    }

    fun clearSelection() {
        _selectedIds.clear()
        isSelectionMode = false
    }
}