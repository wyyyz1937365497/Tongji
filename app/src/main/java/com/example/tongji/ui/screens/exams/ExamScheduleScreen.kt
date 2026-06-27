package com.example.tongji.ui.screens.exams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.tongji.data.local.entity.ExamScheduleItemEntity
import com.example.tongji.ui.components.CalendarExportAction
import com.example.tongji.util.CalendarExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScheduleScreen(onBack: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var scheduledExams by remember { mutableStateOf<List<ExamScheduleItemEntity>>(emptyList()) }
    var unscheduledExams by remember { mutableStateOf<List<ExamScheduleItemEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        scope.launch {
            isLoading = true
            app.academicRepository.syncExams()
            scheduledExams = app.academicRepository.getScheduledExams()
            unscheduledExams = app.academicRepository.getUnscheduledExams()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("考试安排") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (scheduledExams.isNotEmpty()) {
                        CalendarExportAction(onExport = { ctx ->
                            CalendarExporter.exportExams(ctx, scheduledExams)
                        })
                    }
                    IconButton(onClick = { load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!scheduledExams.firstOrNull()?.switchRemark.isNullOrEmpty()) {
                        item {
                            InfoBanner(scheduledExams.first().switchRemark)
                        }
                    }
                    if (scheduledExams.isNotEmpty()) {
                        item {
                            Text(
                                "已安排考试（${scheduledExams.size}）",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(scheduledExams, key = { it.sourceId }) { exam ->
                            ExamCard(exam)
                        }
                    }
                    if (unscheduledExams.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "未安排考试（${unscheduledExams.size}）",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(unscheduledExams, key = { it.sourceId }) { exam ->
                            ExamCard(exam)
                        }
                    }
                    if (scheduledExams.isEmpty() && unscheduledExams.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("暂无考试安排", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ExamCard(exam: ExamScheduleItemEntity) {
    val dateText = exam.examDateText?.takeIf { it.isNotEmpty() }
    val dateOnly = dateText?.substringBefore(" ")?.trim()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exam.courseName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(exam.calendarName, fontSize = 10.sp) },
                    modifier = Modifier.height(24.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            if (dateText != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        buildString {
                            append(dateOnly)
                            val st = exam.startTimeText
                            val et = exam.endTimeText
                            if (!st.isNullOrEmpty() || !et.isNullOrEmpty()) {
                                append("  $st - $et")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!exam.examTimeText.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    exam.examTimeText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!exam.examSite.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        exam.examSite!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (!exam.remark.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    exam.remark!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
