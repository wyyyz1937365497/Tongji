package com.example.tongji.ui.screens.grades

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import com.example.tongji.data.local.dao.TermInfo
import com.example.tongji.data.local.entity.GradeCourseRecordEntity
import com.example.tongji.data.local.entity.GradeSummaryEntity
import com.example.tongji.state.TermInfo as GlobalTermInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeReportScreen(onBack: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf<GradeSummaryEntity?>(null) }
    var terms by remember { mutableStateOf<List<TermInfo>>(emptyList()) }
    var allCourses by remember { mutableStateOf<List<GradeCourseRecordEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        scope.launch {
            isLoading = true
            app.academicRepository.syncGrades()
            summary = app.academicRepository.getSummary()
            terms = app.academicRepository.getTerms()
            allCourses = terms.flatMap { term ->
                app.academicRepository.getCoursesForTerm(term.termCode)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课程成绩") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                item { SummaryCard(summary) }
                if (terms.isNotEmpty()) {
                    items(terms, key = { it.termCode }) { term ->
                        TermSection(
                            term = term,
                            courses = allCourses.filter { it.termCode == term.termCode }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TermSection(
    term: TermInfo,
    courses: List<GradeCourseRecordEntity>
) {
    val isCurrent = GlobalTermInfo.isCurrentTerm(term.termCode)
    var expanded by remember(term.termCode) { mutableStateOf(isCurrent) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        term.termName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("当前", fontSize = 10.sp)
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    if (courses.isEmpty()) {
                        Text(
                            "本学期暂无成绩记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    } else {
                        courses.forEach { course ->
                            GradeCourseCard(course)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: GradeSummaryEntity?) {
    if (summary == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("总绩点", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                summary.totalGradePoint,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("已修学分", summary.actualCredit)
                StatItem("未过学分", summary.failingCredits)
                StatItem("未过门数", summary.failingCourseCount)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GradeCourseCard(course: GradeCourseRecordEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(course.courseName, fontWeight = FontWeight.Medium)
                Row {
                    Text(
                        course.creditText + "学分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (course.courseCategory != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            course.courseCategory!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    course.scoreText ?: "—",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "绩点 ${course.gradePointText ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
