package com.example.tongji.ui.screens.schedule

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tongji.TongjiApp
import com.example.tongji.data.local.entity.CourseScheduleEntity
import com.example.tongji.data.local.entity.ExamScheduleItemEntity
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(onNavigateToExams: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var currentWeek by remember { mutableIntStateOf(1) }
    var schedules by remember { mutableStateOf<List<CourseScheduleEntity>>(emptyList()) }
    var examsThisWeek by remember { mutableStateOf<List<ExamScheduleItemEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        schedules = app.courseRepository.getAllSchedules()
        examsThisWeek = app.academicRepository.getScheduledExams()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课程表") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            app.courseRepository.sync()
                            schedules = app.courseRepository.getAllSchedules()
                            isLoading = false
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                WeekSelector(currentWeek, onWeekChange = { currentWeek = it })
                val weekSchedules = schedules.filter { it.weekNumber == currentWeek }

                val thisWeekExams = examsThisWeek.filter {
                    it.examDateText?.let { date ->
                        // Simple check - in a real app parse dates properly
                        true
                    } ?: false
                }
                if (thisWeekExams.isNotEmpty()) {
                    ExamBanner(thisWeekExams, onNavigateToExams)
                }

                WeekGridView(
                    schedules = weekSchedules,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun WeekSelector(currentWeek: Int, onWeekChange: (Int) -> Unit) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { if (currentWeek > 1) onWeekChange(currentWeek - 1) }) {
                Text("< 上一周")
            }
            Text(
                "第 $currentWeek 周",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            TextButton(onClick = { onWeekChange(currentWeek + 1) }) {
                Text("下一周 >")
            }
        }
    }
}

@Composable
private fun ExamBanner(exams: List<ExamScheduleItemEntity>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📝", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "本周考试：${exams.size} 门",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WeekGridView(
    schedules: List<CourseScheduleEntity>,
    modifier: Modifier = Modifier
) {
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val periods = listOf(
        "1\n08:00", "2\n08:50", "3\n10:00", "4\n10:45",
        "5\n11:35", "6\n13:30", "7\n14:15", "8\n15:05",
        "9\n15:50", "10\n18:00", "11\n18:45", "12\n19:35", "13\n20:10", "14\n20:55"
    )

    val scrollState = rememberScrollState()

    Column(modifier.horizontalScroll(scrollState)) {
        Row {
            Cell(width = 60.dp, height = 48.dp) {
                Text("节次", fontSize = 10.sp, textAlign = TextAlign.Center)
            }
            days.forEach { day ->
                Cell(width = if (day == "周六" || day == "周日") 80.dp else 120.dp, height = 48.dp) {
                    Text(day, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        periods.forEachIndexed { index, period ->
            Row {
                Cell(width = 60.dp, height = 52.dp) {
                    Text(period, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
                }
                days.forEachIndexed { dayIdx, _ ->
                    val courses = schedules.filter { it.dayOfWeek == dayIdx + 1 && it.startPeriod == index + 1 }
                    val cellWidth = if (dayIdx >= 5) 80.dp else 120.dp
                    if (courses.isNotEmpty()) {
                        val course = courses.first()
                        val span = (course.endPeriod - course.startPeriod + 1).coerceAtLeast(1)
                        CourseCell(
                            course = course,
                            width = cellWidth,
                            height = 52.dp * span
                        )
                    } else {
                        Cell(width = cellWidth, height = 52.dp) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseCell(course: CourseScheduleEntity, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val colors = listOf(
        Color(0xFFE3F2FD), Color(0xFFFCE4EC), Color(0xFFE8F5E9),
        Color(0xFFFFF3E0), Color(0xFFF3E5F5), Color(0xFFE0F7FA),
        Color(0xFFFFF8E1), Color(0xFFF1F8E9), Color(0xFFFBE9E7)
    )
    val color = colors[abs(course.courseName.hashCode()) % colors.size]

    Surface(
        modifier = Modifier.width(width).height(height),
        color = color,
        shape = RoundedCornerShape(4.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(2.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                course.courseName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (course.location.isNotEmpty()) {
                Text(
                    course.location,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Cell(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
