package com.example.tongji.ui.screens.schedule

import android.view.LayoutInflater
import android.widget.FrameLayout
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tongji.R
import com.example.tongji.TongjiApp
import com.example.tongji.data.local.entity.CourseScheduleEntity
import com.example.tongji.data.local.entity.ExamScheduleItemEntity
import com.example.tongji.ui.screens.schedule.calendarview.CalendarThemeApplier
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarLayout
import com.haibin.calendarview.CalendarView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(onNavigateToExams: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    var schedules by remember { mutableStateOf<List<CourseScheduleEntity>>(emptyList()) }
    var examsThisWeek by remember { mutableStateOf<List<ExamScheduleItemEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val selectedDateState = remember { mutableStateOf(todayStr) }
    val daySchedulesState = remember { mutableStateOf<List<CourseScheduleEntity>>(emptyList()) }

    val schemeColorArgb = remember(colorScheme) { colorScheme.primary.toArgb() }

    LaunchedEffect(selectedDateState.value, schedules) {
        daySchedulesState.value = schedules
            .filter { it.date == selectedDateState.value }
            .sortedBy { it.startPeriod }
    }

    fun load() {
        scope.launch {
            isLoading = true
            app.courseRepository.sync()
            schedules = app.courseRepository.getAllSchedules()
            examsThisWeek = app.academicRepository.getScheduledExams()
            isLoading = false
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
            CourseCalendarWithContent(
                schedules = schedules,
                examsThisWeek = examsThisWeek,
                colorScheme = colorScheme,
                selectedDateState = selectedDateState,
                daySchedulesState = daySchedulesState,
                onNavigateToExams = onNavigateToExams,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

@Composable
private fun CourseCalendarWithContent(
    schedules: List<CourseScheduleEntity>,
    examsThisWeek: List<ExamScheduleItemEntity>,
    colorScheme: ColorScheme,
    selectedDateState: MutableState<String>,
    daySchedulesState: MutableState<List<CourseScheduleEntity>>,
    onNavigateToExams: () -> Unit,
    modifier: Modifier = Modifier
) {
    val schemeColorArgb = colorScheme.primary.toArgb()
    AndroidView(
        factory = { context ->
            val root = LayoutInflater.from(context)
                .inflate(R.layout.view_course_calendar, null) as CalendarLayout
            val calendarView = root.findViewById<CalendarView>(R.id.course_calendar_view)
            val contentView = root.findViewById<FrameLayout>(R.id.course_content_view)

            calendarView.setMonthView(
                com.example.tongji.ui.screens.schedule.calendarview.CourseMonthView::class.java
            )
            calendarView.setWeekView(
                com.example.tongji.ui.screens.schedule.calendarview.CourseWeekView::class.java
            )
            CalendarThemeApplier.apply(calendarView, colorScheme)

            calendarView.setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener {
                override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
                    selectedDateState.value = String.format(
                        "%04d-%02d-%02d", calendar.year, calendar.month, calendar.day
                    )
                }
                override fun onCalendarOutOfRange(calendar: Calendar?) {}
            })

            calendarView.setSchemeDate(buildSchemeMap(schedules, schemeColorArgb))

            val composeView = ComposeView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    MaterialTheme(colorScheme = colorScheme) {
                        val courses by daySchedulesState
                        val selectedDate by selectedDateState
                        CourseDetailPane(
                            courses = courses,
                            selectedDate = selectedDate,
                            examsThisWeek = examsThisWeek,
                            onNavigateToExams = onNavigateToExams
                        )
                    }
                }
            }
            contentView.addView(composeView)
            root
        },
        update = { root ->
            val calendarView = root.findViewById<CalendarView>(R.id.course_calendar_view)
            calendarView.setSchemeDate(buildSchemeMap(schedules, schemeColorArgb))
        },
        modifier = modifier
    )
}

@Composable
private fun CourseDetailPane(
    courses: List<CourseScheduleEntity>,
    selectedDate: String,
    examsThisWeek: List<ExamScheduleItemEntity>,
    onNavigateToExams: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        if (examsThisWeek.isNotEmpty()) {
            ExamBanner(examsThisWeek, onNavigateToExams)
        }

        Text(
            text = if (selectedDate.isNotEmpty()) "$selectedDate · ${courses.size} 节课" else "请选择日期",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (courses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "当天无课程",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(courses) { course ->
                    CourseCard(course)
                }
            }
        }
    }
}

private fun buildSchemeMap(
    schedules: List<CourseScheduleEntity>,
    schemeColor: Int
): Map<String, Calendar> {
    val map = mutableMapOf<String, Calendar>()
    for (dateStr in schedules.mapNotNull { it.date }.distinct()) {
        val parts = dateStr.split("-")
        if (parts.size != 3) continue
        val year = parts[0].toIntOrNull() ?: continue
        val month = parts[1].toIntOrNull() ?: continue
        val day = parts[2].toIntOrNull() ?: continue
        val calendar = Calendar().apply {
            this.year = year
            this.month = month
            this.day = day
            this.schemeColor = schemeColor
        }
        map[calendar.toString()] = calendar
    }
    return map
}

@Composable
private fun CourseCard(course: CourseScheduleEntity) {
    val containerColors = remember {
        listOf(
            0xFFE3F2FD.toInt(), 0xFFFCE4EC.toInt(), 0xFFE8F5E9.toInt(),
            0xFFFFF3E0.toInt(), 0xFFF3E5F5.toInt(), 0xFFE0F7FA.toInt(),
            0xFFFFF8E1.toInt(), 0xFFF1F8E9.toInt(), 0xFFFBE9E7.toInt()
        )
    }
    val color = androidx.compose.ui.graphics.Color(
        containerColors[abs(course.courseName.hashCode()) % containerColors.size]
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(course.courseName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "第${course.startPeriod}-${course.endPeriod}节",
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📝", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text("本周考试：${exams.size} 门", fontWeight = FontWeight.Medium)
        }
    }
}
