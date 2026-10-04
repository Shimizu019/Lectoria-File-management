package com.lectoria.ui.subjects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lectoria.data.repository.LectoriaRepository
import com.lectoria.data.storage.FileStorageManager
import com.lectoria.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A subject plus how many files it currently holds. */
data class SubjectRow(
    val name: String,
    val fileCount: Int
)

data class SubjectsUiState(
    val subjects: List<SubjectRow> = emptyList()
)

/**
 * Holds the subjects of one class. [className] arrives through navigation, so the
 * ViewModel is created with a small factory.
 */
class SubjectsViewModel(
    application: Application,
    private val className: String
) : AndroidViewModel(application) {

    private val repository = LectoriaRepository(FileStorageManager(application))

    private val _uiState = MutableStateFlow(SubjectsUiState())
    val uiState: StateFlow<SubjectsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val rows = repository.subjects(className).map { subject ->
            SubjectRow(
                name = subject.name,
                fileCount = repository.fileCount(className, subject.name)
            )
        }
        _uiState.value = SubjectsUiState(subjects = rows)
    }

    fun addSubject(name: String) {
        val safeName = FileUtils.sanitizeName(name)
        if (safeName.isEmpty()) return
        if (repository.createSubject(className, safeName)) refresh()
    }

    fun renameSubject(currentName: String, newName: String) {
        val safeName = FileUtils.sanitizeName(newName)
        if (safeName.isEmpty() || safeName == currentName) return
        if (repository.renameSubject(className, currentName, safeName)) refresh()
    }

    fun deleteSubject(name: String) {
        if (repository.deleteSubject(className, name)) refresh()
    }

    companion object {
        fun factory(application: Application, className: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SubjectsViewModel(application, className) }
            }
    }
}