package com.example.tongji.ui.screens.schedule

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tongji.TongjiApp
import com.example.tongji.data.local.entity.CourseScheduleEntity
import com.example.tongji.data.local.entity.ExamScheduleItemEntity
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(onNavigateToExams: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var schedules by remember { mutableStateOf<List<CourseScheduleEntity>>(emptyList()) }
    var examsThisWeek by remember { mutableStateOf<List<ExamScheduleItemEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf("") }
    var calendarViewRef by remember { mutableStateOf<CalendarView?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            app.courseRepository.sync()
            schedules = app.courseRepository.getAllSchedules()
            examsThisWeek = app.academicRepository.getScheduledExams()
            isLoading = false
            calendarViewRef?.let { cv ->
                val map = buildSchemeMap(schedules)
                cv.setSchemeDate(map)
            }
        }
    }

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
                    IconButton(onClick = { load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && schedules.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                CalendarViewContainer(
                    schedules = schedules,
                    onDateSelected = { year, month, day ->
                        selectedDate = String.format("%04d-%02d-%02d", year, month, day)
                    },
                    onViewCreated = { cv ->
                        calendarViewRef = cv
                        if (schedules.isNotEmpty()) {
                            val map = buildSchemeMap(schedules)
                            cv.setSchemeDate(map)
                        }
                    }
                )

                val thisWeekExams = examsThisWeek.filter {
                    it.examDateText?.let { date -> true } ?: false
                }
                if (thisWeekExams.isNotEmpty()) {
                    ExamBanner(thisWeekExams, onNavigateToExams)
                }

                val daySchedules = if (selectedDate.isNotEmpty()) {
                    schedules.filter { it.date == selectedDate }.sortedBy { it.startPeriod }
                } else {
                    emptyList()
                }

                if (selectedDate.isNotEmpty()) {
                    Text(
                        "$selectedDate 课程",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (daySchedules.isEmpty() && selectedDate.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("当天无课程", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(daySchedules) { course ->
                            CourseCard(course)
                        }
                    }
                }
            }
        }
    }
}

private fun buildSchemeMap(schedules: List<CourseScheduleEntity>): Map<String, Calendar> {
    val map = mutableMapOf<String, Calendar>()
    val uniqueDates = schedules.mapNotNull { it.date }.distinct()
    for (dateStr in uniqueDates) {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val year = parts[0].toIntOrNull() ?: continue
            val month = parts[1].toIntOrNull() ?: continue
            val day = parts[2].toIntOrNull() ?: continue
            val calendar = Calendar().apply {
                this.year = year
                this.month = month
                this.day = day
                this.schemeColor = 0xFF2196F3.toInt()
            }
            map[calendar.toString()] = calendar
        }
    }
    return map
}

@Composable
private fun CalendarViewContainer(
    schedules: List<CourseScheduleEntity>,
    onDateSelected: (Int, Int, Int) -> Unit,
    onViewCreated: (CalendarView) -> Unit
) {
    AndroidView(
        factory = { context ->
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val calendarView = CalendarView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setMonthView(com.example.tongji.ui.screens.schedule.calendarview.CourseMonthView::class.java)
                setWeekView(com.example.tongji.ui.screens.schedule.calendarview.CourseWeekView::class.java)
                setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener {
                    override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
                        onDateSelected(calendar.year, calendar.month, calendar.day)
                    }
                    override fun onCalendarOutOfRange(calendar: Calendar?) {}
                })
            }
            layout.addView(calendarView)
            onViewCreated(calendarView)
            layout
        },
        update = { layout ->
            val calendarView = layout.getChildAt(0) as? CalendarView
            calendarView?.let { cv ->
                val map = buildSchemeMap(schedules)
                cv.setSchemeDate(map)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CourseCard(course: CourseScheduleEntity) {
    val colors = listOf(
        Color(0xFFE3F2FD), Color(0xFFFCE4EC), Color(0xFFE8F5E9),
        Color(0xFFFFF3E0), Color(0xFFF3E5F5), Color(0xFFE0F7FA),
        Color(0xFFFFF8E1), Color(0xFFF1F8E9), Color(0xFFFBE9E7)
    )
    val color = colors[abs(course.courseName.hashCode()) % colors.size]

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                course.courseName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${course.startPeriod}-${course.endPeriod}节",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    course.location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (course.teacher.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    course.teacher,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
