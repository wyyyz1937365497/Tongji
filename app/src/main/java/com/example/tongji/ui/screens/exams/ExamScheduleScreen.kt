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
import com.example.tongji.TongjiApp
import com.example.tongji.data.local.entity.ExamScheduleItemEntity
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
                                Text("暂无考试安排")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamCard(exam: ExamScheduleItemEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(exam.courseName, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (exam.examDateText != null && exam.examDateText!!.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${exam.examDateText} ${exam.startTimeText ?: ""} - ${exam.endTimeText ?: ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (exam.examSite != null && exam.examSite!!.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        exam.examSite!!,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
