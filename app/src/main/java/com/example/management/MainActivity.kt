package com.example.management

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.management.ui.theme.ManagementTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ManagementTheme {
                var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
                
                if (isLoggedIn) {
                    MainScreen(viewModel)
                } else {
                    AuthScreen(onAuthSuccess = { isLoggedIn = true })
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: TaskViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            TaskListScreen(
                viewModel,
                onEdit = { taskToEdit = it }
            )
        }
    }

    if (showDialog) {
        AddTaskDialog(
            onDismiss = { showDialog = false },
            onConfirm = { title, desc, date ->
                viewModel.addTask(title, desc, date)
                showDialog = false
            }
        )
    }

    taskToEdit?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, desc ->
                viewModel.updateTask(task.id, title, desc)
                taskToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(viewModel: TaskViewModel, onEdit: (Task) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dataStore = remember { PreferencesManager(context) }
    val tasks by viewModel.tasks.collectAsState()
    val sortBy by dataStore.sortBy.collectAsState(initial = "Status")
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val sortedTasks = remember(tasks, sortBy) {
        when (sortBy) {
            "Date" -> tasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }
            else -> tasks.sortedBy { it.status }
        }
    }

    Column {
        Text(
            text = "Team Manager",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = sortBy == "Status", 
                onClick = { scope.launch { dataStore.saveSortOrder("Status") } }, 
                label = { Text("Sort by Status") }
            )
            FilterChip(
                selected = sortBy == "Date", 
                onClick = { scope.launch { dataStore.saveSortOrder("Date") } }, 
                label = { Text("Sort by Date") }
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                isRefreshing = false
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedTasks, key = { it.id }) { task ->
                    Box(modifier = Modifier.animateItem()) {
                        TaskItem(
                            task,
                            onStatusChange = { viewModel.updateTaskStatus(task.id, task.status) },
                            onDelete = { viewModel.deleteTask(task.id) },
                            onEdit = { onEdit(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onStatusChange: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEdit()
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title, 
                    style = MaterialTheme.typography.titleLarge, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = task.description, 
                style = MaterialTheme.typography.bodyLarge, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.status + (task.dueDate?.let { " | Due: ${java.text.SimpleDateFormat("MMM dd").format(java.util.Date(it))}" } ?: ""), 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStatusChange()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cycle", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (String, String, Long?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(dueDate?.let { "Due: ${java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(it))}" } ?: "Select Due Date")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, desc, dueDate) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun EditTaskDialog(task: Task, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, desc) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
