package com.example.tongji.ui.screens.activities

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tongji.TongjiApp
import com.example.tongji.data.local.entity.CampusActivityEntity
import com.example.tongji.data.repository.ActivityRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityListScreen(onBack: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var activities by remember { mutableStateOf<List<CampusActivityEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedModule by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            app.activityRepository.sync()
                .onSuccess { activities = app.activityRepository.getAll() }
                .onFailure {
                    error = it.message ?: "加载失败"
                    activities = app.activityRepository.getAll()
                }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val moduleOptions = remember(activities) {
        activities.mapNotNull { it.moduleCode }.distinct().sorted()
    }
    val statusOptions = remember(activities) {
        activities.mapNotNull { it.progressName }.filter { it.isNotEmpty() }.distinct()
    }

    val filtered = remember(activities, selectedModule, selectedStatus) {
        activities.filter { a ->
            (selectedModule == null || a.moduleCode == selectedModule) &&
            (selectedStatus == null || a.progressName == selectedStatus)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("卓越星活动") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null && activities.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { load() }) { Text("重试") }
                        }
                    }
                }
                activities.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无活动", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterSection(
                                label = "星值类别",
                                options = moduleOptions,
                                selected = selectedModule,
                                displayFor = { ActivityRepository.CATEGORY_NAME_MAP[it] ?: it },
                                onSelect = { code ->
                                    selectedModule = if (selectedModule == code) null else code
                                }
                            )
                        }
                        item {
                            FilterSection(
                                label = "进行状态",
                                options = statusOptions,
                                selected = selectedStatus,
                                displayFor = { it },
                                onSelect = { s ->
                                    selectedStatus = if (selectedStatus == s) null else s
                                }
                            )
                        }
                        item {
                            Text(
                                "共 ${filtered.size} / ${activities.size} 个活动",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(filtered, key = { it.remoteId }) { activity ->
                            ActivityCard(activity)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    label: String,
    options: List<String>,
    selected: String?,
    displayFor: (String) -> String,
    onSelect: (String) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selected == null,
                onClick = { selected?.let { onSelect(it) } },
                label = { Text("全部") }
            )
            options.forEach { opt ->
                FilterChip(
                    selected = selected == opt,
                    onClick = { onSelect(opt) },
                    label = { Text(displayFor(opt)) }
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(activity: CampusActivityEntity) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.CHINA) }
    val now = remember { System.currentTimeMillis() }
    val isOngoing = activity.activityDate != null && activity.activityEndDate != null &&
        now in activity.activityDate..activity.activityEndDate
    val isEnded = activity.activityEndDate != null && now > activity.activityEndDate

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    activity.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (activity.starPoints > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                formatPoints(activity.starPoints),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (activity.moduleName != null) {
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(activity.moduleName, fontSize = 11.sp) },
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val timeText = activity.activityDate?.let { dateFormat.format(Date(it)) } ?: ""
                if (timeText.isNotEmpty()) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!activity.location.isNullOrEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        activity.location!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            if (!activity.progressName.isNullOrEmpty() || isOngoing || isEnded) {
                Spacer(Modifier.height(6.dp))
                val statusText = when {
                    isOngoing -> "进行中"
                    isEnded -> "已结束"
                    else -> activity.progressName ?: ""
                }
                val statusColor = when {
                    isOngoing -> MaterialTheme.colorScheme.primary
                    isEnded -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        statusText,
                        fontSize = 11.sp,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (activity.source.isNotEmpty() && activity.source != "STAR平台") {
                Spacer(Modifier.height(4.dp))
                Text(
                    "主办：${activity.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatPoints(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString()
    else String.format("%.1f", value)
