package com.lectoria.ui.classes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lectoria.data.repository.LectoriaRepository
import com.lectoria.data.storage.FileStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A class plus the counts shown under its name. */
data class ClassRow(
    val name: String,
    val subjectCount: Int,
    val fileCount: Int
)

data class ClassesUiState(
    val classes: List<ClassRow> = emptyList()
)

/**
 * Holds the class list. The filesystem is the source of truth, so every action
 * simply re-reads the folders after the change.
 */
class ClassesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LectoriaRepository(FileStorageManager(application))

    private val _uiState = MutableStateFlow(ClassesUiState())
    val uiState: StateFlow<ClassesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val rows = repository.classes().map { classFolder ->
            val subjects = repository.subjects(classFolder.name)
            ClassRow(
                name = classFolder.name,
                subjectCount = subjects.size,
                fileCount = subjects.sumOf { subject ->
                    repository.fileCount(classFolder.name, subject.name)
                }
            )
        }
        _uiState.value = ClassesUiState(classes = rows)
    }

    fun addClass(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (repository.createClass(trimmed)) refresh()
    }

    fun renameClass(currentName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == currentName) return
        if (repository.renameClass(currentName, trimmed)) refresh()
    }

    fun deleteClass(name: String) {
        if (repository.deleteClass(name)) refresh()
    }
}