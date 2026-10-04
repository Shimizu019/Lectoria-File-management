package com.lectoria.ui.files

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lectoria.ui.components.ConfirmDialog
import com.lectoria.ui.components.EmptyState
import com.lectoria.ui.components.FileItem
import com.lectoria.util.Constants

/**
 * Step 3 of the flow: the files of one subject, plus the button that imports a
 * file through the system picker.
 *
 * Opening a file is intentionally left for a later stage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    className: String,
    subjectName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: FilesViewModel = viewModel(
        key = "files-$className-$subjectName",
        factory = FilesViewModel.factory(application, className, subjectName)
    )
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var filePendingDeletion by remember { mutableStateOf<String?>(null) }

    // The picker returns a uri; Lectoria copies it into the subject folder.
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importFile(uri)
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(subjectName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { picker.launch(Constants.IMPORTABLE_MIME_TYPES) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add file") }
            )
        }
    ) { padding ->
        when {
            state.isImporting -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.files.isEmpty() -> EmptyState(
                title = "No files yet",
                bodyText = "Tap Add file and pick a PDF, PPT or DOC to save it in $subjectName.",
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.files, key = { it.name }) { lectureFile ->
                    FileItem(
                        file = lectureFile,
                        onClick = { /* preview comes in a later stage */ },
                        onDelete = { filePendingDeletion = lectureFile.name }
                    )
                }
            }
        }
    }

    filePendingDeletion?.let { fileName ->
        ConfirmDialog(
            title = "Delete $fileName?",
            message = "This removes the saved copy from your device.",
            onConfirm = {
                viewModel.deleteFile(fileName)
                filePendingDeletion = null
            },
            onDismiss = { filePendingDeletion = null }
        )
    }
}