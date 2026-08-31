package com.example.tongji.ui.screens.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tongji.TongjiApp
import com.example.tongji.data.local.entity.PracticeHourRecordEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// 实践环节要求：3学年内 24 学时 = 8 必修 + 16 选修
private const val REQUIRED_HOURS = 8.0
private const val ELECTIVE_HOURS = 16.0

private data class PlateSummary(
    val plateName: String,
    val nature: String,
    val actual: Double,
    val certified: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeHourScreen(onBack: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<PracticeHourRecordEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var syncError by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            syncError = null
            app.academicRepository.syncPracticeHours()
                .onFailure { syncError = it.message }
            records = app.academicRepository.getPracticeRecords()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    // 汇总计算
    val requiredActual = records.filter { it.nature == "1" }.sumOf { it.hour }
    val electiveActual = records.filter { it.nature == "2" }.sumOf { it.hour }
    val requiredDone = requiredActual >= REQUIRED_HOURS
    val electiveDone = electiveActual >= ELECTIVE_HOURS

    val plateSummaries = records
        .groupBy { it.plateName }
        .map { (plateName, items) ->
            val nature = items.firstOrNull()?.nature ?: ""
            val actual = items.sumOf { it.hour }
            val cap = if (nature == "1") REQUIRED_HOURS else ELECTIVE_HOURS
            PlateSummary(plateName, nature, actual, minOf(actual, cap))
        }
        .sortedWith(compareByDescending<PlateSummary> { it.nature }.thenByDescending { it.actual })

    val terms = records.groupBy { it.calendarId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("社会实践") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                syncError?.let { message ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                "同步失败：${message ?: "未知错误"}，当前展示缓存数据",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                item {
                    SummaryCard(
                        requiredActual = requiredActual,
                        electiveActual = electiveActual,
                        requiredDone = requiredDone,
                        electiveDone = electiveDone
                    )
                }
                if (plateSummaries.isNotEmpty()) {
                    item { PlateSummaryCard(plateSummaries) }
                }
                if (terms.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "暂无社会实践记录",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    terms.forEach { (term, items) ->
                        item(key = term) {
                            TermSection(term = term, records = items)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    requiredActual: Double,
    electiveActual: Double,
    requiredDone: Boolean,
    electiveDone: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "实践环节",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "要求在3学年完成24学时，包括8学时必修和16学时选修",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(12.dp))
            HourProgressRow("必修", requiredActual, REQUIRED_HOURS, requiredDone)
            Spacer(Modifier.height(10.dp))
            HourProgressRow("选修", electiveActual, ELECTIVE_HOURS, electiveDone)
        }
    }
}

@Composable
private fun HourProgressRow(label: String, actual: Double, required: Double, done: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$label ${formatHour(actual)} / ${formatHour(required)} 学时",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
            Badge(
                containerColor = if (done) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    if (done) "完成" else "未完成",
                    color = if (done) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (actual / required).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlateSummaryCard(plates: List<PlateSummary>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("实践环节明细", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                SummaryHeaderCell("板块", 1.4f)
                SummaryHeaderCell("性质", 0.7f)
                SummaryHeaderCell("实际学时", 1f)
                SummaryHeaderCell("认定学时", 1f)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            plates.forEach { plate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryTextCell(plate.plateName, 1.4f, FontWeight.Medium)
                    SummaryTextCell(if (plate.nature == "1") "必修" else "选修", 0.7f)
                    SummaryTextCell(formatHour(plate.actual), 1f)
                    SummaryTextCell(formatHour(plate.certified), 1f, FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RowScope.SummaryHeaderCell(text: String, weight: Float) {
    SummaryTextCell(text, weight, FontWeight.SemiBold)
}

@Composable
private fun RowScope.SummaryTextCell(text: String, weight: Float, fontWeight: FontWeight? = null) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        fontSize = 13.sp,
        fontWeight = fontWeight,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TermSection(term: String, records: List<PracticeHourRecordEntity>) {
    var expanded by remember(term) { mutableStateOf(false) }

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
                        term,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("${records.size}条", fontSize = 10.sp)
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
                    records.forEach { record ->
                        RecordItem(record)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordItem(record: PracticeHourRecordEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(record.plateName, fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatDate(record.activityDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "+${formatHour(record.hour)} 学时",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatHour(hour: Double): String {
    return if (hour == hour.toLong().toDouble()) {
        hour.toLong().toString()
    } else {
        String.format(Locale.CHINA, "%.1f", hour)
    }
}

private fun formatDate(date: String): String {
    return try {
        val src = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dst = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
        dst.format(src.parse(date)!!)
    } catch (_: Exception) {
        date
    }
}
