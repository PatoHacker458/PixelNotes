package com.midknight.pixelnotes.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.midknight.pixelnotes.data.Note
import com.midknight.pixelnotes.data.NoteDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val dao: NoteDao) : ViewModel() {

    val notes = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveNote(note: Note) {
        viewModelScope.launch {
            dao.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            dao.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            dao.deleteNote(note)
        }
    }
}

class NotesViewModelFactory(private val dao: NoteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}