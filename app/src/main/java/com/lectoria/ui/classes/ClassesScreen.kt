package com.lectoria.ui.classes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lectoria.ui.components.ConfirmDialog
import com.lectoria.ui.components.EmptyState
import com.lectoria.ui.components.FolderItem
import com.lectoria.ui.components.NameInputDialog

/**
 * Step 1 of the flow: pick the class, e.g. "BSIT 3-6".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassesScreen(
    onOpenClass: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClassesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var dialog by remember { mutableStateOf<ClassDialog?>(null) }
    var nameInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Classes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                nameInput = ""
                dialog = ClassDialog.Create
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add class")
            }
        }
    ) { padding ->
        if (state.classes.isEmpty()) {
            EmptyState(
                title = "No classes yet",
                bodyText = "Tap + and create your first class, for example BSIT 3-6.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.classes, key = { it.name }) { classRow ->
                    FolderItem(
                        name = classRow.name,
                        subtitle = "${classRow.subjectCount} subjects · ${classRow.fileCount} files",
                        onClick = { onOpenClass(classRow.name) },
                        onRename = {
                            nameInput = classRow.name
                            dialog = ClassDialog.Rename(classRow.name)
                        },
                        onDelete = { dialog = ClassDialog.Delete(classRow.name) }
                    )
                }
            }
        }
    }

    when (val current = dialog) {
        ClassDialog.Create -> NameInputDialog(
            title = "Create class",
            label = "Class name",
            placeholder = "BSIT 3-6",
            value = nameInput,
            confirmLabel = "Create",
            onValueChange = { nameInput = it },
            onConfirm = {
                viewModel.addClass(nameInput)
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        is ClassDialog.Rename -> NameInputDialog(
            title = "Rename class",
            label = "Class name",
            placeholder = current.previousName,
            value = nameInput,
            confirmLabel = "Save",
            onValueChange = { nameInput = it },
            onConfirm = {
                viewModel.renameClass(current.previousName, nameInput)
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        is ClassDialog.Delete -> ConfirmDialog(
            title = "Delete ${current.className}?",
            message = "The class folder and all of its subjects and files will be removed.",
            onConfirm = {
                viewModel.deleteClass(current.className)
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        null -> Unit
    }
}

private sealed interface ClassDialog {
    data object Create : ClassDialog
    data class Rename(val previousName: String) : ClassDialog
    data class Delete(val className: String) : ClassDialog
}