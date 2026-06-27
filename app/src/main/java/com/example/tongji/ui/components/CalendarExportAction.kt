package com.example.tongji.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CalendarExportAction(
    onExport: (Context) -> Int,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.EventAvailable,
    contentDescription: String = "添加到日历",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var dialogMsg by remember { mutableStateOf("") }

    val permissions = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    fun doExport() {
        scope.launch {
            val count = withContext(Dispatchers.IO) { onExport(context) }
            dialogMsg = if (count > 0) {
                "已添加 $count 项到系统日历"
            } else {
                "未能添加（可能缺少时间信息或无可用日历账户）"
            }
            showDialog = true
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (granted) {
            doExport()
        } else {
            dialogMsg = "需要日历读写权限才能添加日程"
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("好的") }
            },
            text = { Text(dialogMsg) }
        )
    }

    IconButton(
        onClick = {
            val granted = permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (granted) doExport() else launcher.launch(permissions)
        },
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
