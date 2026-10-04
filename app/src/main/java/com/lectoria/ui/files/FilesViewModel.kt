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
import com.lectoria.util.FileOpenResult
import com.lectoria.util.FileOpener
import com.lectoria.util.errorMessage
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

    /**
     * Copies every file the user picked into this subject folder.
     * Existing files are never overwritten: the storage layer numbers
     * duplicates, e.g. a second "Lecture 01.pdf" becomes "Lecture 01 (2).pdf".
     */
    fun importFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)

            var saved = 0
            var failed = 0
            for (uri in uris) {
                if (repository.importFile(uri, className, subjectName) != null) {
                    saved++
                } else {
                    failed++
                }
            }

            _uiState.value = FilesUiState(
                files = repository.files(className, subjectName),
                message = importSummaryMessage(saved, failed)
            )
        }
    }

    /** Opens a file in another app, e.g. a PDF viewer. */
    fun openFile(file: LectureFile): FileOpenResult {
        val result = FileOpener.open(getApplication(), file)
        result.errorMessage?.let { message ->
            _uiState.value = _uiState.value.copy(message = message)
        }
        return result
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

/**
 * Message shown after a batch import, for example "4 files saved".
 * Returns null when there is nothing worth telling the user.
 */
internal fun importSummaryMessage(saved: Int, failed: Int): String? = when {
    saved == 0 && failed == 0 -> null
    saved == 0 -> "No files were saved"
    failed == 0 -> if (saved == 1) "1 file saved" else "$saved files saved"
    else -> "$saved of ${saved + failed} files saved"
}