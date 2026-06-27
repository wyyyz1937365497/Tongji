package com.example.tongji.ui.screens.campus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tongji.TongjiApp
import kotlinx.coroutines.launch

data class CampusService(
    val name: String,
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusHomeScreen(
    onNavigateToActivities: () -> Unit,
    onNavigateToNotices: () -> Unit,
    onNavigateToCampusCard: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val app = TongjiApp.getInstance()
    var refreshing by remember { mutableStateOf(false) }

    val services = listOf(
        CampusService("卓越星活动", Icons.Default.Star, "查看校园活动", onNavigateToActivities),
        CampusService("通知公告", Icons.Default.Notifications, "教学管理通知", onNavigateToNotices),
        CampusService("校园卡", Icons.Default.CreditCard, "余额与交易", onNavigateToCampusCard),
        CampusService("考试安排", Icons.Default.EditCalendar, "查看考试信息", onNavigateToExams),
        CampusService("课程成绩", Icons.Default.Grade, "GPA与成绩", onNavigateToGrades),
        CampusService("图书馆座位", Icons.Default.MenuBook, "座位查询", onNavigateToLibrary)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("校园") },
                actions = {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(services) { service ->
                ServiceCard(service)
            }
        }
    }
}

@Composable
private fun ServiceCard(service: CampusService) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = service.onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                service.icon,
                contentDescription = service.name,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    service.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    service.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
