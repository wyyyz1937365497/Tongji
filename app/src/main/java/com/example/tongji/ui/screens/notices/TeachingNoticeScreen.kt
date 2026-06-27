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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                },
                scrollBehavior = scrollBehavior
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
    val title = detail["title"] as? String ?: ""
    val publishTime = detail["publishTime"] as? String ?: ""
    val createUser = detail["createUser"] as? String ?: ""
    val contentHtml = detail["content"] as? String ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (createUser.isNotEmpty()) {
                Text(
                    createUser,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                publishTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        com.example.tongji.ui.components.HtmlContent(
            html = contentHtml,
            baseUrl = "https://1.tongji.edu.cn/",
            modifier = Modifier.fillMaxSize()
        )
    }
}
