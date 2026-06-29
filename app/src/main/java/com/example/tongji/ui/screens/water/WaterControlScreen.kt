package com.example.tongji.ui.screens.water

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tongji.TongjiApp
import com.example.tongji.data.remote.model.WaterController
import com.example.tongji.data.remote.model.WaterGroup
import com.example.tongji.data.repository.WaterRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterControlScreen(onBack: () -> Unit) {
    val app = TongjiApp.getInstance()
    val scope = rememberCoroutineScope()

    var groups by remember { mutableStateOf<List<WaterGroup>>(emptyList()) }
    var controllers by remember { mutableStateOf<Map<String, List<WaterController>>>(emptyMap()) }
    var expandedGroup by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = app.waterRepository.fetchGroups()
                result.onSuccess { list ->
                    groups = list
                }.onFailure { e ->
                    errorMessage = e.message
                }
            } catch (e: Exception) {
                errorMessage = e.message
            }
            isLoading = false
        }
    }

    fun loadControllers(group: WaterGroup) {
        scope.launch {
            val result = app.waterRepository.fetchControllers(group)
            result.onSuccess { list ->
                controllers = controllers.toMutableMap().apply {
                    put(group.classNo ?: "", list)
                }
            }.onFailure { e ->
                errorMessage = e.message
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能控水") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (errorMessage != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(onClick = { loadData() }) { Text("重试") }
                                }
                            }
                        }
                    }

                    if (groups.isEmpty() && !isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无数据，请先在设置中登录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    items(groups) { group ->
                        GroupCard(
                            group = group,
                            isExpanded = expandedGroup == group.classNo,
                            controllers = controllers[group.classNo] ?: emptyList(),
                            onExpand = {
                                if (expandedGroup == group.classNo) {
                                    expandedGroup = null
                                } else {
                                    expandedGroup = group.classNo
                                    if (controllers[group.classNo].isNullOrEmpty()) {
                                        loadControllers(group)
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
private fun GroupCard(
    group: WaterGroup,
    isExpanded: Boolean,
    controllers: List<WaterController>,
    onExpand: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.className ?: "未知分组", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "编号: ${group.classNo ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (isExpanded) {
                Spacer(Modifier.height(12.dp))
                if (controllers.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                } else {
                    controllers.forEach { controller ->
                        ControllerItem(controller)
                        if (controller != controllers.last()) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControllerItem(controller: WaterController) {
    val status = WaterRepository.statusText(controller.posNum)
    val statusColor = when (controller.posNum) {
        1 -> MaterialTheme.colorScheme.primary
        4 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(controller.className ?: "未知控水器", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "编号: ${controller.classNo ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(status, fontWeight = FontWeight.Bold, color = statusColor)
    }
}
