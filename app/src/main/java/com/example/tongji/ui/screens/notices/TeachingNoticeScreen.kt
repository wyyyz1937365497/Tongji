package com.example.tongji.ui.screens.notices

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
import com.example.tongji.TongjiApp
import com.example.tongji.data.local.entity.TeachingNoticeEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingNoticeScreen(onBack: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()
    var notices by remember { mutableStateOf<List<TeachingNoticeEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedNoticeId by remember { mutableStateOf<String?>(null) }
    var noticeDetail by remember { mutableStateOf<Map<String, Any>?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            app.teachingNoticeRepository.sync()
            notices = app.teachingNoticeRepository.getAll()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedNoticeId != null) "通知详情" else "通知公告") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedNoticeId != null) {
                            selectedNoticeId = null
                            noticeDetail = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (selectedNoticeId == null) {
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
            } else if (selectedNoticeId != null && noticeDetail != null) {
                NoticeDetailView(noticeDetail!!)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notices, key = { it.id }) { notice ->
                        NoticeCard(
                            notice = notice,
                            onClick = {
                                scope.launch {
                                    selectedNoticeId = notice.id
                                    val detail = app.teachingNoticeRepository.getDetail(notice.id)
                                    noticeDetail = detail
                                    if (!notice.read) {
                                        app.teachingNoticeRepository.markAsRead(notice.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(notice: TeachingNoticeEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notice.topStatus == 1)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (notice.topStatus == 1) {
                    Text(
                        "置顶",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    notice.title,
                    fontWeight = if (!notice.read) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                notice.publishTimeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoticeDetailView(detail: Map<String, Any>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            detail["title"] as? String ?: "",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            detail["publishTimeText"] as? String ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            detail["contentHTML"] as? String ?: (detail["content"] as? String ?: "暂无内容"),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
