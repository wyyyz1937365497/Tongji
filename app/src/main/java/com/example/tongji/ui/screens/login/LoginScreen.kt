package com.example.tongji.ui.screens.login

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    initialUrl: String = "https://1.tongji.edu.cn",
    onBack: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var currentUrl by remember { mutableStateOf(initialUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            currentUrl = url
                            if (url.contains("workbench.tongji.edu.cn") || url.contains("1.tongji.edu.cn/app")) {
                                view.evaluateJavascript(
                                    "(function() { return localStorage.getItem('uid') || ''; })();"
                                ) { result ->
                                    val uid = result.trim('"').replace("\\\"", "")
                                    if (uid.isNotEmpty()) {
                                        onLoginSuccess(uid)
                                    }
                                }
                            }
                        }
                    }
                    loadUrl(initialUrl)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
