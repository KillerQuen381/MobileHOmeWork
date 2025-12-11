package com.example.mobilehomework

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilehomework.ui.theme.MobileHomeWorkTheme

enum class FilterType {
    ALL, ACTIVE, COMPLETED
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileHomeWorkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val repository = remember { TaskRepository(context) }
    
    var taskText by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(repository.loadTasks()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(FilterType.ALL) }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var editText by remember { mutableStateOf("") }

    // Сохранение задач при изменении
    LaunchedEffect(tasks) {
        repository.saveTasks(tasks)
    }

    // Фильтрация и поиск
    val filteredTasks = remember(tasks, filterType, searchQuery) {
        tasks.filter { task ->
            val matchesFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.ACTIVE -> !task.completed
                FilterType.COMPLETED -> task.completed
            }
            val matchesSearch = task.text.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Мои задачи",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (tasks.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                tasks = emptyList()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Очистить все",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Поиск
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск задач") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистить поиск"
                            )
                        }
                    }
                },
                singleLine = true
            )

            // Фильтры
            FilterChips(
                selectedFilter = filterType,
                onFilterSelected = { filterType = it }
            )

            // Поле ввода новой задачи
            OutlinedTextField(
                value = taskText,
                onValueChange = { taskText = it },
                label = { Text("Новая задача") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (taskText.isNotBlank()) {
                                tasks = tasks + TaskItem(
                                    id = System.currentTimeMillis(),
                                    text = taskText,
                                    completed = false
                                )
                                taskText = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить"
                        )
                    }
                }
            )

            // Список задач
            AnimatedVisibility(
                visible = filteredTasks.isEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (tasks.isEmpty()) {
                                "Нет задач.\nДобавьте новую задачу выше."
                            } else {
                                "Задачи не найдены"
                            },
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = filteredTasks.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            isEditing = editingTaskId == task.id,
                            editText = editText,
                            onEditTextChange = { editText = it },
                            onToggleComplete = {
                                tasks = tasks.map { t ->
                                    if (t.id == task.id) t.copy(completed = !t.completed)
                                    else t
                                }
                            },
                            onEdit = {
                                editingTaskId = task.id
                                editText = task.text
                            },
                            onSaveEdit = {
                                tasks = tasks.map { t ->
                                    if (t.id == task.id) t.copy(text = editText)
                                    else t
                                }
                                editingTaskId = null
                                editText = ""
                            },
                            onCancelEdit = {
                                editingTaskId = null
                                editText = ""
                            },
                            onDelete = {
                                tasks = tasks.filter { it.id != task.id }
                            }
                        )
                    }
                }
            }

            // Статистика
            if (tasks.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Всего",
                            value = tasks.size.toString()
                        )
                        StatItem(
                            label = "Выполнено",
                            value = tasks.count { it.completed }.toString()
                        )
                        StatItem(
                            label = "Осталось",
                            value = tasks.count { !it.completed }.toString()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChips(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == FilterType.ALL,
            onClick = { onFilterSelected(FilterType.ALL) },
            label = { Text("Все") }
        )
        FilterChip(
            selected = selectedFilter == FilterType.ACTIVE,
            onClick = { onFilterSelected(FilterType.ACTIVE) },
            label = { Text("Активные") }
        )
        FilterChip(
            selected = selectedFilter == FilterType.COMPLETED,
            onClick = { onFilterSelected(FilterType.COMPLETED) },
            label = { Text("Выполненные") }
        )
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    isEditing: Boolean,
    editText: String,
    onEditTextChange: (String) -> Unit,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (isEditing) {
            // Режим редактирования
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = onEditTextChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = onSaveEdit) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Сохранить",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onCancelEdit) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Отмена",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            // Обычный режим
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = task.completed,
                        onCheckedChange = { onToggleComplete() }
                    )
                    Text(
                        text = task.text,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                        style = if (task.completed) {
                            MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = TextDecoration.LineThrough
                            )
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = if (task.completed) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

data class TaskItem(
    val id: Long,
    val text: String,
    val completed: Boolean
)
