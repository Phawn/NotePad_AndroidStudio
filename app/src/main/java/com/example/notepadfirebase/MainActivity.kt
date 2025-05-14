package com.example.notepadfirebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.notepadfirebase.ui.theme.NotepadFirebaseTheme
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
// Removed unused Check icon import
// Removed unused ImageVector import
// Removed ViewModel imports as they're not used
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotepadFirebaseTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var currentUser by remember { mutableStateOf<User?>(null) }

    when (currentScreen) {
        Screen.Login -> LoginScreen(
            onLoginSuccess = { user ->
                currentUser = user
                currentScreen = Screen.TaskManager
            },
            onRegisterClick = {
                currentScreen = Screen.Register
            }
        )
        Screen.Register -> RegisterScreen(
            onRegisterSuccess = { user ->
                currentUser = user
                currentScreen = Screen.TaskManager
            },
            onBackToLogin = {
                currentScreen = Screen.Login
            }
        )
        Screen.TaskManager -> TaskManagerScreen(
            currentUser = currentUser,
            onLogout = {
                currentUser = null
                currentScreen = Screen.Login
            }
        )
    }
}

sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object TaskManager : Screen()
}

data class User(
    val username: String = "",
    val password: String = ""
)

data class Task(
    val id: String? = null,
    val username: String = "",
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false
)

enum class TaskFilter {
    ALL, COMPLETED, ONGOING
}

@Composable
fun LoginScreen(
    onLoginSuccess: (User) -> Unit,
    onRegisterClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isNotBlank() && password.isNotBlank()) {
                    val database = FirebaseDatabase.getInstance().reference.child("Users")
                    database.child(username).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists() && snapshot.child("password").getValue(String::class.java) == password) {
                                onLoginSuccess(User(username, password))
                            } else {
                                message = "Invalid username or password"
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            message = "Error: ${error.message}"
                        }
                    })
                } else {
                    message = "Please enter both username and password"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
        Spacer(Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(text = message)
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: (User) -> Unit,
    onBackToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()) {
                    if (password == confirmPassword) {
                        val database = FirebaseDatabase.getInstance().reference.child("Users")
                        database.child(username).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    message = "Username already exists"
                                } else {
                                    database.child(username).setValue(User(username, password))
                                        .addOnSuccessListener {
                                            onRegisterSuccess(User(username, password))
                                        }
                                        .addOnFailureListener {
                                            message = "Error: ${it.message}"
                                        }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                message = "Error: ${error.message}"
                            }
                        })
                    } else {
                        message = "Passwords do not match"
                    }
                } else {
                    message = "Please fill in all fields"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Login")
        }
        Spacer(Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(text = message)
        }
    }
}

@Composable
fun TaskManagerScreen(
    currentUser: User?,
    onLogout: () -> Unit
) {
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var showAddTaskScreen by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var currentFilter by remember { mutableStateOf(TaskFilter.ALL) }
    // Removed unused coroutine scope

    val database = FirebaseDatabase.getInstance().reference
        .child("Tasks")
        .child(currentUser?.username ?: "")

    LaunchedEffect(currentUser?.username) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasksList = mutableListOf<Task>()
                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue(Task::class.java)?.copy(id = taskSnapshot.key)
                    task?.let { tasksList.add(it) }
                }
                tasks = tasksList
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    val filteredTasks = when (currentFilter) {
        TaskFilter.ALL -> tasks
        TaskFilter.COMPLETED -> tasks.filter { it.completed }
        TaskFilter.ONGOING -> tasks.filter { !it.completed }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome, ${currentUser?.username}",
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = onLogout) {
                Text("Logout")
            }
        }
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Task Manager",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (showAddTaskScreen) {
            AddEditTaskScreen(
                task = selectedTask,
                onSave = { title, description, completed ->
                    if (selectedTask == null) {
                        // Add new task
                        val newTask = Task(
                            username = currentUser?.username ?: "",
                            title = title,
                            description = description,
                            completed = completed
                        )
                        database.push().setValue(newTask)
                    } else {
                        // Update existing task
                        selectedTask?.let { task ->
                            database.child(task.id ?: "").setValue(
                                task.copy(title = title, description = description, completed = completed)
                            )
                        }
                    }
                    showAddTaskScreen = false
                    selectedTask = null
                },
                onCancel = {
                    showAddTaskScreen = false
                    selectedTask = null
                }
            )
        } else {
            // Task filter buttons
            FilterButtons(
                currentFilter = currentFilter,
                onFilterSelected = { filter ->
                    currentFilter = filter
                }
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTasks) { task ->
                    TaskItem(
                        task = task,
                        onEdit = {
                            selectedTask = task
                            showAddTaskScreen = true
                        },
                        onDelete = {
                            database.child(task.id ?: "").removeValue()
                        },
                        onToggleComplete = {
                            database.child(task.id ?: "").setValue(
                                task.copy(completed = !task.completed)
                            )
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FloatingActionButton(
                    onClick = {
                        selectedTask = null
                        showAddTaskScreen = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    }
}

@Composable
fun FilterButtons(
    currentFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FilterButton(
            text = "All",
            selected = currentFilter == TaskFilter.ALL,
            onClick = { onFilterSelected(TaskFilter.ALL) }
        )
        FilterButton(
            text = "Ongoing",
            selected = currentFilter == TaskFilter.ONGOING,
            onClick = { onFilterSelected(TaskFilter.ONGOING) }
        )
        FilterButton(
            text = "Completed",
            selected = currentFilter == TaskFilter.COMPLETED,
            onClick = { onFilterSelected(TaskFilter.COMPLETED) }
        )
    }
}

@Composable
fun FilterButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text)
    }
}

@Composable
fun TaskItem(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggleComplete() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .clickable(onClick = onEdit)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (task.description.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Task")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Task")
                }
            }
        }
    }
}

@Composable
fun AddEditTaskScreen(
    task: Task?,
    onSave: (String, String, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var completed by remember { mutableStateOf(task?.completed ?: false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (task == null) "Add New Task" else "Edit Task",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))

        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            minLines = 3
        )
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = completed,
                onCheckedChange = { completed = it }
            )
            Text(
                text = "Mark as completed",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onSave(title, description, completed) },
                enabled = title.isNotBlank()
            ) {
                Text(if (task == null) "Add Task" else "Update Task")
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Login Screen",
    widthDp = 360,
    heightDp = 640
)
@Composable
fun LoginScreenPreview() {
    NotepadFirebaseTheme {
        LoginScreen(
            onLoginSuccess = {},
            onRegisterClick = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "Register Screen",
    widthDp = 360,
    heightDp = 640
)
@Composable
fun RegisterScreenPreview() {
    NotepadFirebaseTheme {
        RegisterScreen(
            onRegisterSuccess = {},
            onBackToLogin = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "Task Manager Screen",
    widthDp = 360,
    heightDp = 640
)
@Composable
fun TaskManagerScreenPreview() {
    NotepadFirebaseTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Welcome, johndoe",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = {}) {
                        Text("Logout")
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Task Manager",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                FilterButtons(
                    currentFilter = TaskFilter.ALL,
                    onFilterSelected = {}
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(3) { index ->
                        TaskItem(
                            task = Task(
                                id = index.toString(),
                                username = "johndoe",
                                title = "Sample Task $index",
                                description = "This is a sample task for preview purposes.",
                                completed = index % 2 == 0
                            ),
                            onEdit = {},
                            onDelete = {},
                            onToggleComplete = {}
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FloatingActionButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Add Task Screen")
@Composable
fun AddTaskScreenPreview() {
    NotepadFirebaseTheme {
        AddEditTaskScreen(
            task = null,
            onSave = { _, _, _ -> },
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Task Screen")
@Composable
fun EditTaskScreenPreview() {
    NotepadFirebaseTheme {
        AddEditTaskScreen(
            task = Task(
                id = "1",
                username = "johndoe",
                title = "Sample Task",
                description = "This is a sample task for preview purposes.",
                completed = true
            ),
            onSave = { _, _, _ -> },
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, name = "Task Item")
@Composable
fun TaskItemPreview() {
    NotepadFirebaseTheme {
        TaskItem(
            task = Task(
                id = "1",
                username = "johndoe",
                title = "Complete Project Report",
                description = "Need to finish the quarterly report by Friday.",
                completed = false
            ),
            onEdit = {},
            onDelete = {},
            onToggleComplete = {}
        )
    }
}