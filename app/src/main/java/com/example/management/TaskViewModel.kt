package com.example.management

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    init {
        // Real-time listener for tasks
        db.collection("tasks")
            .whereEqualTo("projectId", "default_project")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("TaskViewModel", "Error fetching tasks", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val taskList = snapshot.toObjects(Task::class.java)
                    android.util.Log.d("TaskViewModel", "Fetched ${taskList.size} tasks")
                    _tasks.value = taskList
                }
            }
    }

    fun loadTasksForProject(projectId: String) {
        db.collection("tasks")
            .whereEqualTo("projectId", projectId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _tasks.value = snapshot.toObjects(Task::class.java)
                }
            }
    }

    fun addTask(title: String, description: String, dueDate: Long?) {
        val newTask = Task(
            id = db.collection("tasks").document().id,
            projectId = "default_project",
            title = title,
            description = description,
            status = "To-Do",
            dueDate = dueDate
        )
        db.collection("tasks").document(newTask.id).set(newTask)
    }

    fun updateTask(taskId: String, title: String, description: String) {
        db.collection("tasks").document(taskId)
            .update("title", title, "description", description)
    }

    fun updateTaskStatus(taskId: String, currentStatus: String) {
        val nextStatus = when (currentStatus) {
            "To-Do" -> "In Progress"
            "In Progress" -> "Done"
            else -> "To-Do"
        }
        db.collection("tasks").document(taskId).update("status", nextStatus)
    }

    fun deleteTask(taskId: String) {
        db.collection("tasks").document(taskId).delete()
    }
}
