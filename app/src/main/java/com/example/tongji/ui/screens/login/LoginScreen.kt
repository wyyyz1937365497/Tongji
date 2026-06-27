package com.example.tongji.ui.screens.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tongji.TongjiApp
import com.example.tongji.auth.CampusModel
import com.example.tongji.auth.CookieJar
import com.example.tongji.auth.CredentialStore
import kotlinx.coroutines.launch

private const val TAG = "LoginScreen"
private const val INITIAL_URL = "https://1.tongji.edu.cn"

private fun syncCookiesToJar(context: android.content.Context, url: String) {
    val cookieManager = CookieManager.getInstance()
    val cookieJar = CookieJar.getInstance(context)
    val cookieStr = cookieManager.getCookie(url) ?: return
    Log.d(TAG, "同步 cookies: ${cookieStr.take(200)}")
    try {
        val host = try { java.net.URL(url).host } catch (_: Exception) { return }
        val httpUrl = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host(host)
            .build()
        val cookies = cookieStr.split(";").mapNotNull { it.trim().split("=", limit = 2) }
            .filter { it.size == 2 }
            .map { (name, value) ->
                okhttp3.Cookie.Builder()
                    .name(name.trim())
                    .value(value.trim())
                    .domain(httpUrl.host)
                    .path("/")
                    .expiresAt(System.currentTimeMillis() + 86400_000L) // 24h
                    .httpOnly()
                    .build()
            }
        cookieJar.saveFromResponse(httpUrl, cookies)
        Log.d(TAG, "cookies 已同步: ${cookies.size} 条")
    } catch (e: Exception) {
        Log.e(TAG, "同步 cookies 失败: ${e.message}", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(INITIAL_URL) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录同济一系统") },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "返回按钮点击")
                        webView?.destroy()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        Log.d(TAG, "WebView factory 创建")
                        webView = this

                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.userAgentString = settings.userAgentString

                        CookieManager.getInstance().setAcceptCookie(true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                Log.d(TAG, "onPageStarted: url=$url")
                                url?.let { currentUrl = it }
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                Log.d(TAG, "onPageFinished: url=$url")
                                currentUrl = url
                                isLoading = false

                                val host = try { java.net.URL(url).host } catch (_: Exception) { "" }
                                Log.d(TAG, "host=$host")

                                if (host.contains("1.tongji.edu.cn") || host.contains("workbench.tongji.edu.cn")) {
                                    Log.d(TAG, "命中一系统域名，开始提取凭证")

                                    val cookieStr = CookieManager.getInstance().getCookie(url)
                                    Log.d(TAG, "cookies: ${cookieStr?.take(200)}")

                                    try {
                                        val uri = java.net.URI.create(url)
                                        val queryMap = mutableMapOf<String, String>()
                                        uri.query?.split("&")?.forEach { pair ->
                                            val kv = pair.split("=", limit = 2)
                                            if (kv.size == 2) {
                                                queryMap[kv[0]] = java.net.URLDecoder.decode(kv[1], "UTF-8")
                                            }
                                        }
                                        val urlUid = queryMap["uid"]
                                        if (!urlUid.isNullOrEmpty() && urlUid.matches(Regex("^\\d+$"))) {
                                            Log.d(TAG, "成功从 URL 提取 uid: $urlUid")
                                            val store = CredentialStore.getInstance(context)
                                            store.putString(CredentialStore.KEY_UID, urlUid)
                                            CampusModel.markValid()
                                            syncCookiesToJar(context, url)
                                            scope.launch {
                                                try {
                                                    TongjiApp.getInstance().sessionRepository.refreshSessionUser()
                                                } catch (_: Exception) { }
                                            }
                                            webView?.destroy()
                                            webView = null
                                            onBack()
                                            return@onPageFinished
                                        } else if (!urlUid.isNullOrEmpty()) {
                                            Log.d(TAG, "URL uid 不是学号，跳过: $urlUid")
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "从 URL 提取 uid 失败: ${e.message}")
                                    }

                                    tryExtractSessionData(view, url, 1)
                                }
                            }

                            private fun tryExtractSessionData(view: WebView, url: String, attempt: Int) {
                                if (attempt > 10) {
                                    Log.d(TAG, "sessiondata 提取轮询超时")
                                    return
                                }
                                view.evaluateJavascript(
                                    "(function() {" +
                                    "  var sd = localStorage.getItem('sessiondata');" +
                                    "  var sid = sessionStorage.getItem('sessionid') || sessionStorage.getItem('sessionId') || localStorage.getItem('sessionid') || localStorage.getItem('sessionId') || '';" +
                                    "  if (sd) {" +
                                    "    try {" +
                                    "      var p = JSON.parse(sd);" +
                                    "      return JSON.stringify({uid: p.uid || p.UID || '', aesKey: p.aesKey || '', aesIv: p.aesIv || '', sessionId: sid});" +
                                    "    } catch(e) {" +
                                    "      return JSON.stringify({uid: sd || '', sessionId: sid});" +
                                    "    }" +
                                    "  }" +
                                    "  return JSON.stringify({uid: '', sessionId: sid});" +
                                    "})();"
                                ) { result ->
                                    Log.d(TAG, "evaluateJavascript sessiondata (attempt=$attempt): ${result?.take(300)}")
                                    try {
                                        val jsResult = result?.trim('"')?.replace("\\\"", "\"") ?: "{}"
                                        val json = org.json.JSONObject(jsResult)
                                        val uid = json.optString("uid", "")
                                        val aesKey = json.optString("aesKey", "").takeIf { it.isNotEmpty() }
                                        val aesIv = json.optString("aesIv", "").takeIf { it.isNotEmpty() }
                                        val sessionId = json.optString("sessionId", "").takeIf { it.isNotEmpty() }
                                        Log.d(TAG, "解析 localStorage uid='$uid' aesKey=${aesKey != null} sessionId=${sessionId != null}")

                                        if (uid.isNotEmpty()) {
                                            Log.d(TAG, "登录成功! uid=$uid sessionId=${sessionId?.take(8)}")
                                            val store = CredentialStore.getInstance(context)
                                            store.putString(CredentialStore.KEY_UID, uid)
                                            aesKey?.let { store.putString(CredentialStore.KEY_AES_KEY, it) }
                                            aesIv?.let { store.putString(CredentialStore.KEY_AES_IV, it) }
                                            sessionId?.let { store.putString(CredentialStore.KEY_SESSION_ID, it) }
                                            CampusModel.markValid()
                                            syncCookiesToJar(context, url)
                                            scope.launch {
                                                try {
                                                    Log.d(TAG, "刷新 session...")
                                                    TongjiApp.getInstance().sessionRepository.refreshSessionUser()
                                                    Log.d(TAG, "session 刷新完成")
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "session 刷新失败: ${e.message}", e)
                                                }
                                            }
                                            webView?.destroy()
                                            webView = null
                                            onBack()
                                        } else {
                                            Log.d(TAG, "uid 为空，1.5秒后重试 (attempt=$attempt)")
                                            view.postDelayed({ tryExtractSessionData(view, url, attempt + 1) }, 1500)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "解析 localStorage 结果失败: ${e.message}")
                                        view.postDelayed({ tryExtractSessionData(view, url, attempt + 1) }, 1500)
                                    }
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                Log.e(TAG, "onReceivedError: code=$errorCode desc=$description url=$failingUrl")
                                super.onReceivedError(view, errorCode, description, failingUrl)
                            }
                        }

                        loadUrl(INITIAL_URL)
                        Log.d(TAG, "开始加载: $INITIAL_URL")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}
