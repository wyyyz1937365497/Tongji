package com.example.tongji.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.tongji.data.local.entity.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onBack: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var targetLibraries by remember { mutableStateOf<List<LibrarySpaceOverviewEntity>>(emptyList()) }
    var otherLibraries by remember { mutableStateOf<List<LibrarySpaceOverviewEntity>>(emptyList()) }
    var selectedLibraryId by remember { mutableStateOf<String?>(null) }
    var areas by remember { mutableStateOf<List<LibrarySpaceAreaEntity>>(emptyList()) }
    var rooms by remember { mutableStateOf<List<LibrarySpaceRoomEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    fun load() {
        scope.launch {
            isLoading = true
            app.librarySpaceRepository.sync()
            targetLibraries = app.librarySpaceRepository.getTargetLibraries()
            otherLibraries = app.librarySpaceRepository.getOtherLibraries()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    fun loadLibraryDetail(libraryId: String) {
        scope.launch {
            areas = app.librarySpaceRepository.getAreasForLibrary(libraryId)
            rooms = app.librarySpaceRepository.getRoomsForLibrary(libraryId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedLibraryId != null) "图书馆详情" else "图书馆座位") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedLibraryId != null) {
                            selectedLibraryId = null
                            areas = emptyList()
                            rooms = emptyList()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (selectedLibraryId == null) {
                        IconButton(onClick = { load() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
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
            } else if (selectedLibraryId != null) {
                LibraryDetailView(
                    libraryId = selectedLibraryId!!,
                    areas = areas,
                    rooms = rooms,
                    selectedTab = selectedTab,
                    onTabChange = { selectedTab = it }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (targetLibraries.isNotEmpty()) {
                        item {
                            Text("目标图书馆", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(targetLibraries, key = { it.libraryId }) { lib ->
                            LibraryCard(
                                overview = lib,
                                onClick = {
                                    selectedLibraryId = lib.libraryId
                                    loadLibraryDetail(lib.libraryId)
                                }
                            )
                        }
                    }
                    if (otherLibraries.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("其他图书馆", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(otherLibraries, key = { it.libraryId }) { lib ->
                            LibraryCard(
                                overview = lib,
                                onClick = {
                                    selectedLibraryId = lib.libraryId
                                    loadLibraryDetail(lib.libraryId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(overview: LibrarySpaceOverviewEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(overview.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "剩余 ${overview.freeSeats} / 总数 ${overview.totalSeats}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val occupancy = if (overview.totalSeats > 0)
                ((overview.totalSeats - overview.freeSeats).toFloat() / overview.totalSeats * 100).toInt()
            else 0
            Text(
                "${occupancy}%",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = when {
                    occupancy < 50 -> MaterialTheme.colorScheme.primary
                    occupancy < 80 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun LibraryDetailView(
    libraryId: String,
    areas: List<LibrarySpaceAreaEntity>,
    rooms: List<LibrarySpaceRoomEntity>,
    selectedTab: Int,
    onTabChange: (Int) -> Unit
) {
    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { onTabChange(0) }) {
                Text("座位区域", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { onTabChange(1) }) {
                Text("研讨室", modifier = Modifier.padding(12.dp))
            }
        }
        when (selectedTab) {
            0 -> SeatAreasView(areas)
            1 -> SeminarRoomsView(rooms)
        }
    }
}

@Composable
private fun SeatAreasView(areas: List<LibrarySpaceAreaEntity>) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(areas, key = { it.areaId }) { area ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(area.mergedName.ifEmpty { area.name }, fontWeight = FontWeight.Medium)
                        Text(
                            area.floorName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "空闲 ${area.freeSeats}/${area.totalSeats}",
                        color = if (area.freeSeats > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SeminarRoomsView(rooms: List<LibrarySpaceRoomEntity>) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rooms, key = { it.roomId }) { room ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MeetingRoom, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(room.mergedName.ifEmpty { room.name }, fontWeight = FontWeight.Medium)
                        Text(
                            room.floorName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
