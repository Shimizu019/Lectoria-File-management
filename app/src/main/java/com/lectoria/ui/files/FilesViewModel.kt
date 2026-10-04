package com.lectoria.ui.files

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lectoria.data.model.LectureFile
import com.lectoria.data.repository.LectoriaRepository
import com.lectoria.data.storage.FileStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FilesUiState(
    val files: List<LectureFile> = emptyList(),
    val isImporting: Boolean = false,
    val message: String? = null
)

/**
 * Holds the files of one subject and copies picked files into it.
 */
class FilesViewModel(
    application: Application,
    private val className: String,
    private val subjectName: String
) : AndroidViewModel(application) {

    private val repository = LectoriaRepository(FileStorageManager(application))

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(files = repository.files(className, subjectName))
    }

    /** Copies the file behind [uri] into this subject folder. */
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)
            val imported = repository.importFile(uri, className, subjectName)
            _uiState.value = FilesUiState(
                files = repository.files(className, subjectName),
                message = if (imported != null) {
                    "Saved ${imported.name}"
                } else {
                    "That file could not be saved"
                }
            )
        }
    }

    fun deleteFile(fileName: String) {
        if (repository.deleteFile(className, subjectName, fileName)) refresh()
    }

    /** Called after the message has been shown so it is not repeated on rotation. */
    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    companion object {
        fun factory(
            application: Application,
            className: String,
            subjectName: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { FilesViewModel(application, className, subjectName) }
        }
    }
}