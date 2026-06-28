package com.example.tongji.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
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
import com.example.tongji.data.repository.AreaInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryDetailScreen(
    libraryId: String,
    libraryName: String,
    onBack: () -> Unit,
    onNavigateToSeatMap: (areaId: String, areaName: String) -> Unit
) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var areas by remember { mutableStateOf<List<AreaInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    fun load() {
        scope.launch {
            isLoading = true
            areas = app.librarySpaceRepository.fetchAreas(libraryId)
            isLoading = false
        }
    }

    LaunchedEffect(libraryId) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(libraryName) },
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("座位区域", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("研讨室", modifier = Modifier.padding(12.dp))
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedTab == 0) {
                FloorAreaList(areas) { area ->
                    onNavigateToSeatMap(area.areaId, area.name)
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无研讨室数据", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun FloorAreaList(areas: List<AreaInfo>, onAreaClick: (AreaInfo) -> Unit) {
    val grouped = areas.groupBy { it.floorName.ifEmpty { "其他" } }
    val expandedStates = remember(grouped.keys) {
        mutableStateMapOf<String, Boolean>().apply {
            grouped.keys.forEachIndexed { index, key -> put(key, index == 0) }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (floorName, floorAreas) ->
            val totalFree = floorAreas.sumOf { it.freeSeats }
            val totalAll = floorAreas.sumOf { it.totalSeats }
            val isExpanded = expandedStates[floorName] ?: false

            item(key = "floor_$floorName") {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        expandedStates[floorName] = !isExpanded
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(floorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "$totalAll 个座位 · 空闲 $totalFree",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }
            }

            items(
                items = floorAreas,
                key = { it.areaId }
            ) { area ->
                AnimatedVisibility(visible = isExpanded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .clickable { onAreaClick(area) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(area.name, fontWeight = FontWeight.Medium)
                                if (area.typeName.isNotEmpty()) {
                                    Text(
                                        area.typeName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Text(
                                "${area.freeSeats}/${area.totalSeats}",
                                color = if (area.freeSeats > 0)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
