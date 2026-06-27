package com.example.tongji.ui.screens.settings

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tongji.TongjiApp
import com.example.tongji.auth.CampusModel
import com.example.tongji.auth.CredentialStore
import com.example.tongji.state.TermInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateToLogin: () -> Unit = {}) {
    val app = TongjiApp.getInstance()
    val authState by CampusModel.authState.collectAsState()
    val userProfile by CampusModel.userProfile.collectAsState()
    val termName = TermInfo.simpleName
    val currentWeek = TermInfo.currentWeek

    Log.d("SettingsScreen", "authState=${authState.javaClass.simpleName}, isLoggedIn=${authState.isLoggedIn}")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (authState.isLoggedIn) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("账号信息", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        userProfile?.let { profile ->
                            InfoRow("姓名", profile.name)
                            InfoRow("学号", profile.uid)
                            profile.facultyName?.let { InfoRow("学院", it) }
                            profile.deptOrMajor?.let { InfoRow("专业", it) }
                            profile.grade?.let { InfoRow("年级", it) }
                        }
                        if (!termName.isNullOrEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            InfoRow("学期", termName)
                        }
                        if (currentWeek != null) {
                            InfoRow("周次", "第${currentWeek}周")
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                Log.d("SettingsScreen", "用户点击退出登录")
                                CredentialStore.getInstance(app).clear()
                                CampusModel.markLoggedOut()
                                CampusModel.clearProfile()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("退出登录")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(top = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("尚未登录", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "请登录同济一系统账号以使用全部功能",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            Log.d("SettingsScreen", "用户点击登录，导航到登录页面")
                            onNavigateToLogin()
                        }) {
                            Text("登录")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "济你太美 v1.0",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            "$label：",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(64.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
