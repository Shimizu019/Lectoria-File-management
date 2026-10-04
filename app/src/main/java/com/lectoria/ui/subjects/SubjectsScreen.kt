package com.lectoria.ui.subjects

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lectoria.ui.components.ConfirmDialog
import com.lectoria.ui.components.EmptyState
import com.lectoria.ui.components.FolderItem
import com.lectoria.ui.components.NameInputDialog

/**
 * Step 2 of the flow: pick the subject inside a class, e.g. "Web Systems".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    className: String,
    onOpenSubject: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: SubjectsViewModel = viewModel(
        key = "subjects-$className",
        factory = SubjectsViewModel.factory(application, className)
    )
    val state by viewModel.uiState.collectAsState()
    var dialog by remember { mutableStateOf<SubjectDialog?>(null) }
    var nameInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(className) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                nameInput = ""
                dialog = SubjectDialog.Create
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add subject")
            }
        }
    ) { padding ->
        if (state.subjects.isEmpty()) {
            EmptyState(
                title = "No subjects yet",
                bodyText = "Tap + and add a subject of $className, for example Web Systems.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.subjects, key = { it.name }) { subjectRow ->
                    FolderItem(
                        name = subjectRow.name,
                        subtitle = "${subjectRow.fileCount} files",
                        onClick = { onOpenSubject(subjectRow.name) },
                        onRename = {
                            nameInput = subjectRow.name
                            dialog = SubjectDialog.Rename(subjectRow.name)
                        },
                        onDelete = { dialog = SubjectDialog.Delete(subjectRow.name) }
                    )
                }
            }
        }
    }

    when (val current = dialog) {
        SubjectDialog.Create -> NameInputDialog(
            title = "Create subject",
            label = "Subject name",
            placeholder = "Web Systems",
            value = nameInput,
            confirmLabel = "Create",
            onValueChange = { nameInput = it },
            onConfirm = {
                viewModel.addSubject(nameInput)
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        is SubjectDialog.Rename -> NameInputDialog(
            title = "Rename subject",
            label = "Subject name",
            placeholder = current.previousName,
            value = nameInput,
            confirmLabel = "Save",
            onValueChange = { nameInput = it },
            onConfirm = {
                viewModel.renameSubject(current.previousName, nameInput)
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        is SubjectDialog.Delete -> ConfirmDialog(
            title = "Delete ${current.subjectName}?",
            message = "The subject folder and every file inside it will be removed.",
            onConfirm = {
                viewModel.deleteSubject(current.subjectName)
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        null -> Unit
    }
}

private sealed interface SubjectDialog {
    data object Create : SubjectDialog
    data class Rename(val previousName: String) : SubjectDialog
    data class Delete(val subjectName: String) : SubjectDialog
}