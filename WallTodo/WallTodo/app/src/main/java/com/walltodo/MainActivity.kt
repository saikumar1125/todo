package com.walltodo

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    val todos = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val status = MutableStateFlow<String?>(null)

    private fun change(block: suspend () -> Unit) = viewModelScope.launch {
        block()
        val ok = WallpaperUpdater.update(getApplication())
        status.value = if (ok) null else "Couldn't set lock screen wallpaper on this device"
    }
    fun add(title: String) = change { dao.insert(Todo(title = title)) }
    fun edit(t: Todo, title: String) = change { dao.update(t.copy(title = title)) }
    fun toggle(t: Todo) = change {
        dao.update(t.copy(isDone = !t.isDone, doneAt = if (!t.isDone) System.currentTimeMillis() else null))
    }
    fun remove(t: Todo) = change { dao.delete(t) }
    fun refresh() = change { }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Screen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(vm: TodoViewModel = viewModel()) {
    val todos by vm.todos.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<Todo?>(null) }
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("WallTodo") }, actions = {
                TextButton(onClick = { vm.refresh() }) { Text("Update wallpaper") }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "Add") }
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            status?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            if (todos.isEmpty()) Text("No tasks yet. Tap + to add one.", Modifier.padding(16.dp))
            LazyColumn(contentPadding = PaddingValues(8.dp)) {
                items(todos, key = { it.id }) { t ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(t.isDone, { vm.toggle(t) })
                        Text(
                            t.title, Modifier.weight(1f),
                            textDecoration = if (t.isDone) TextDecoration.LineThrough else null
                        )
                        IconButton({ dialog = t }) { Icon(Icons.Default.Edit, "Edit") }
                        IconButton({ vm.remove(t) }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }

    if (adding || dialog != null) {
        val editing = dialog
        var text by remember(editing) { mutableStateOf(editing?.title ?: "") }
        AlertDialog(
            onDismissRequest = { adding = false; dialog = null },
            title = { Text(if (editing == null) "New task" else "Edit task") },
            text = { OutlinedTextField(text, { text = it }, singleLine = false) },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) {
                        if (editing == null) vm.add(text.trim()) else vm.edit(editing, text.trim())
                    }
                    adding = false; dialog = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton({ adding = false; dialog = null }) { Text("Cancel") } }
        )
    }
}
