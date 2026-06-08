package com.example.management

data class Task(
    val id: String = "",
    val projectId: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "To-Do",
    val assignedTo: String = "",
    val dueDate: Long? = null
)
