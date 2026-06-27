package com.example.tongji.ui.screens.grades

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tongji.TongjiApp
import com.example.tongji.data.local.dao.TermInfo
import com.example.tongji.data.local.entity.GradeCourseRecordEntity
import com.example.tongji.data.local.entity.GradeSummaryEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeReportScreen(onBack: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf<GradeSummaryEntity?>(null) }
    var terms by remember { mutableStateOf<List<TermInfo>>(emptyList()) }
    var selectedTerm by remember { mutableIntStateOf(0) }
    var courses by remember { mutableStateOf<List<GradeCourseRecordEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        scope.launch {
            isLoading = true
            app.academicRepository.syncGrades()
            summary = app.academicRepository.getSummary()
            terms = app.academicRepository.getTerms()
            if (terms.isNotEmpty() && selectedTerm == 0) {
                selectedTerm = terms.first().termCode
            }
            if (selectedTerm != 0) {
                courses = app.academicRepository.getCoursesForTerm(selectedTerm)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    LaunchedEffect(selectedTerm) {
        if (selectedTerm != 0) {
            courses = app.academicRepository.getCoursesForTerm(selectedTerm)
        }
    }

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
                    item {
                        TermSelector(
                            terms = terms,
                            selectedTermCode = selectedTerm,
                            onTermSelected = { selectedTerm = it }
                        )
                    }
                }
                items(courses, key = { it.sourceId }) { course ->
                    GradeCourseCard(course)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermSelector(
    terms: List<TermInfo>,
    selectedTermCode: Int,
    onTermSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTerm = terms.find { it.termCode == selectedTermCode } ?: terms.first()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedTerm.termName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            terms.forEach { term ->
                DropdownMenuItem(
                    text = { Text(term.termName) },
                    onClick = {
                        onTermSelected(term.termCode)
                        expanded = false
                    }
                )
            }
        }
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
