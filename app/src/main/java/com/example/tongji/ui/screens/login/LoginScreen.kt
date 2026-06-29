package com.example.tongji.ui.screens.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.tongji.util.WaterCipher
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "LoginScreen"
private const val INITIAL_URL = "https://1.tongji.edu.cn"
private const val ALL_TONGJI_SSO_URL = "https://all.tongji.edu.cn/new/index.html"
private const val YIKATONG_SSO_URL = "https://yikatong.tongji.edu.cn/user/user"
private const val SPACE_SSO_URL = "https://space.tongji.edu.cn/api/Oauth3/login"

private const val PAY_ORIGIN = "https://pay-yikatong.tongji.edu.cn"
private const val KS_ORIGIN = "https://ks.tongji.edu.cn"

private val WATER_LOGIN_URL: String by lazy {
    val platUrl = "$PAY_ORIGIN/plat?loginFrom=h5"
    val inner = "$PAY_ORIGIN/berserker-base/redirect?type=url&url=" + java.net.URLEncoder.encode(platUrl, "UTF-8")
    "$PAY_ORIGIN/berserker-auth/cas/redirect/bamboocloud?targetUrl=" + java.net.URLEncoder.encode(inner, "UTF-8")
}

private const val JWT_HOOK_JS = """
(function() {
    if (window.__jwt_hook_installed) return;
    window.__jwt_hook_installed = true;
    window.__captured_jwt = '';
    var origSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function() {
        var self = this;
        self.addEventListener('load', function() {
            try {
                if (self.responseURL && self.responseURL.indexOf('/api/cas/user') >= 0) {
                    var data = JSON.parse(self.responseText);
                    if (data.member && data.member.token) {
                        window.__captured_jwt = data.member.token;
                    }
                }
            } catch(e) {}
        });
        return origSend.apply(self, arguments);
    };
})();
"""

private enum class LoginPhase {
    IDLE, LOGGED_IN, ALL_TONGJI_DONE, YIKATONG_DONE, SPACE_DONE, WATER_PENDING, WATER_DONE
}

private fun syncCookiesToJar(context: android.content.Context, url: String) {
    val cookieManager = CookieManager.getInstance()
    val cookieJar = CookieJar.getInstance(context)
    val cookieStr = cookieManager.getCookie(url) ?: return
    Log.d(TAG, "同步 cookies for $url: ${cookieStr.take(200)}")
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
                    .expiresAt(System.currentTimeMillis() + 86400_000L)
                    .httpOnly()
                    .build()
            }
        cookieJar.saveFromResponse(httpUrl, cookies)
        Log.d(TAG, "cookies 已同步: ${cookies.size} 条 for $host")
    } catch (e: Exception) {
        Log.e(TAG, "同步 cookies 失败: ${e.message}", e)
    }
}

private fun extractCasToken(url: String): String? {
    val marker = "#/cas?cas="
    val idx = url.indexOf(marker)
    if (idx < 0) return null
    val start = idx + marker.length
    val rest = url.substring(start)
    return rest.substringBefore("&").substringBefore("#").takeIf { it.isNotEmpty() }
}

private fun extractSynjonesAuth(url: String): String? {
    return try {
        val uri = android.net.Uri.parse(url)
        uri.getQueryParameter("synjones-auth")
    } catch (_: Exception) { null }
}

private fun buildWaterRedirectUrl(token: String): String {
    val encoded = java.net.URLEncoder.encode(token, "UTF-8")
    return "$PAY_ORIGIN/berserker-base/redirect?appId=240&type=app" +
            "&synjones-auth=$encoded&synAccessSource=wechat-work&loginFrom=wechat-work"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(INITIAL_URL) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loginPhase by remember { mutableStateOf(LoginPhase.IDLE) }
    var settleRunnable by remember { mutableStateOf<Runnable?>(null) }
    var spaceCasToken by remember { mutableStateOf<String?>(null) }
    var waterSynjonesAuth by remember { mutableStateOf<String?>(null) }

    fun cancelSettle() {
        settleRunnable?.let {
            webView?.removeCallbacks(it)
            settleRunnable = null
        }
    }

    fun scheduleSettle(view: WebView, tag: String, onSettle: () -> Unit) {
        cancelSettle()
        val runnable = Runnable {
            Log.d(TAG, "$tag 页面稳定 5 秒")
            onSettle()
        }
        settleRunnable = runnable
        view.postDelayed(runnable, 5000)
    }

    fun finishLogin() {
        loginPhase = LoginPhase.WATER_DONE
        webView?.destroy()
        webView = null
        onBack()
    }

    fun startWaterPhase() {
        loginPhase = LoginPhase.WATER_PENDING
        Log.d(TAG, "=== 开始水控 SSO 阶段 ===")
        Log.d(TAG, "导航到 pay-yikatong 登录入口")
        webView?.loadUrl(WATER_LOGIN_URL)
    }

    var waterPhaseDone by remember { mutableStateOf(false) }

    fun saveWaterParams(account: String, aesKey: String, password: String) {
        val store = CredentialStore.getInstance(context)
        store.putString(CredentialStore.KEY_WATER_ACCOUNT, account)
        store.putString(CredentialStore.KEY_WATER_AES_KEY, aesKey)
        store.putString(CredentialStore.KEY_WATER_PASSWORD, password)
        TongjiApp.getInstance().waterRepository.setAuthParams(
            com.example.tongji.data.repository.WaterRepository.WaterAuthParams(account, aesKey, password)
        )
        Log.d(TAG, "水控参数已保存 (aesKey from ${if (aesKey == WaterCipher.DEFAULT_AES_KEY) "fallback" else "js"})")
        syncCookiesToJar(context, KS_ORIGIN)
        finishLogin()
    }

    val onWaterAccount: (String?) -> Unit = lambda@{ account ->
        if (waterPhaseDone) { return@lambda }
        waterPhaseDone = true

        val acct = account?.trim('"')?.replace("\\\"", "\"")?.takeIf { it.isNotEmpty() }
        if (acct == null) {
            Log.e(TAG, "水控: 未能获取 ano")
            finishLogin()
            return@lambda
        }
        val wv = webView ?: run {
            Log.e(TAG, "水控: WebView 已销毁")
            finishLogin()
            return@lambda
        }
        Log.d(TAG, "水控: 获取一卡通号 $acct")
        wv.evaluateJavascript(
            """
            (function() {
                var scripts = Array.from(document.scripts).map(s => s.src).filter(Boolean);
                var resources = [];
                try {
                    resources = performance.getEntriesByType('resource')
                        .map(e => e.name).filter(name => name.endsWith('.js'));
                } catch(e) {}
                return JSON.stringify({scripts: scripts, resources: resources});
            })();
            """.trimIndent()
        ) { result ->
            try {
                val json = JSONObject(result.trim('"').replace("\\\"", "\""))
                val urls = mutableListOf<String>()
                val seen = mutableSetOf<String>()
                val scripts = json.optJSONArray("scripts")
                val resources = json.optJSONArray("resources")
                for (i in 0 until (scripts?.length() ?: 0)) {
                    val u = scripts?.optString(i) ?: continue
                    if (u.startsWith(KS_ORIGIN) && u.contains(".js") && u !in seen) { seen.add(u); urls.add(u) }
                }
                for (i in 0 until (resources?.length() ?: 0)) {
                    val u = resources?.optString(i) ?: continue
                    if (u.startsWith(KS_ORIGIN) && u.contains(".js") && u !in seen) { seen.add(u); urls.add(u) }
                }
                Log.d(TAG, "水控: 找到 ${urls.size} 个 JS 文件")
                val jsArray = org.json.JSONArray(urls)
                wv.evaluateJavascript(
                    """
                    (async function() {
                        var texts = [];
                        var urls = ${jsArray};
                        for (var i = 0; i < urls.length; i++) {
                            try {
                                var r = await fetch(urls[i]);
                                if (r.ok) { var t = await r.text(); if (t) texts.push(t); }
                            } catch(e) {}
                        }
                        return JSON.stringify({texts: texts});
                    })();
                    """.trimIndent()
                ) { res ->
                    var aesKey: String? = null
                    var password: String? = null
                    try {
                        val j = JSONObject(res.trim('"').replace("\\\"", "\""))
                        val texts = j.optJSONArray("texts")
                        val jsTexts = mutableListOf<String>()
                        for (i in 0 until (texts?.length() ?: 0)) {
                            texts?.optString(i)?.let { jsTexts.add(it) }
                        }
                        Log.d(TAG, "水控: 获取到 ${jsTexts.size} 个 JS 文本")
                        aesKey = WaterCipher.findAesKey(jsTexts)
                        password = WaterCipher.findPassword(jsTexts)
                    } catch (e: Exception) {
                        Log.e(TAG, "水控: 解析 JS 失败", e)
                    }
                    if (aesKey == null) aesKey = WaterCipher.DEFAULT_AES_KEY
                    if (password == null) password = WaterCipher.DEFAULT_PASSWORD
                    saveWaterParams(acct, aesKey, password)
                }
            } catch (e: Exception) {
                Log.e(TAG, "水控: 提取 JS URL 失败，使用 fallback 值", e)
                saveWaterParams(acct, WaterCipher.DEFAULT_AES_KEY, WaterCipher.DEFAULT_PASSWORD)
            }
        }
    }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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

                        CookieManager.getInstance().setAcceptCookie(true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                Log.d(TAG, "onPageStarted: url=$url")
                                url?.let { currentUrl = it }
                                cancelSettle()
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                Log.d(TAG, "onPageFinished: url=$url phase=$loginPhase")
                                currentUrl = url
                                isLoading = false

                                val host = try { java.net.URL(url).host } catch (_: Exception) { "" }

                                // ===== 水控阶段: pay-yikatong → 捕获 synjones-auth =====
                                if (loginPhase == LoginPhase.WATER_PENDING && host.contains("pay-yikatong.tongji.edu.cn")) {
                                    val token = extractSynjonesAuth(url)
                                    if (token != null && token.isNotEmpty()) {
                                        waterSynjonesAuth = token
                                        Log.d(TAG, "水控: 捕获 synjones-auth, 准备跳转 ks")
                                        view.postDelayed({
                                            view.loadUrl(buildWaterRedirectUrl(token))
                                        }, 1500)
                                    }
                                    return@onPageFinished
                                }

                                // ===== 水控阶段: ks.tongji.edu.cn → 提取参数后关闭 =====
                                if (loginPhase == LoginPhase.WATER_PENDING && url.startsWith(KS_ORIGIN) && !waterPhaseDone) {
                                    Log.d(TAG, "水控: 到达 ks.tongji.edu.cn, 等待页面加载后提取参数")
                                    view.postDelayed({
                                        if (waterPhaseDone) return@postDelayed
                                        view.evaluateJavascript("(function() { return sessionStorage.getItem('ano') || ''; })();") { result ->
                                            onWaterAccount(result)
                                        }
                                    }, 3000)
                                    return@onPageFinished
                                }

                                if (loginPhase == LoginPhase.WATER_PENDING || loginPhase == LoginPhase.WATER_DONE) {
                                    return@onPageFinished
                                }

                                // ===== all.tongji 阶段 =====
                                if (host == "all.tongji.edu.cn" && loginPhase == LoginPhase.LOGGED_IN) {
                                    Log.d(TAG, "all.tongji.edu.cn 加载完成，延迟 5 秒同步 CAS cookie")
                                    view.postDelayed({
                                        syncCookiesToJar(context, url)
                                        loginPhase = LoginPhase.ALL_TONGJI_DONE
                                        Log.d(TAG, "导航到 yikatong.tongji.edu.cn 获取 SSO cookie")
                                        view.loadUrl(YIKATONG_SSO_URL)
                                    }, 5000)
                                    return@onPageFinished
                                }

                                // ===== yikatong 阶段 =====
                                if (host == "yikatong.tongji.edu.cn" && loginPhase == LoginPhase.ALL_TONGJI_DONE) {
                                    scheduleSettle(view, "yikatong") {
                                        syncCookiesToJar(context, url)
                                        loginPhase = LoginPhase.YIKATONG_DONE
                                        Log.d(TAG, "导航到 space.tongji.edu.cn 获取图书馆 SSO")
                                        view.loadUrl(SPACE_SSO_URL)
                                    }
                                    return@onPageFinished
                                }

                                // ===== space 阶段 =====
                                if (host.contains("space.tongji.edu.cn") && loginPhase == LoginPhase.YIKATONG_DONE) {
                                    val casToken = extractCasToken(url)
                                    if (casToken != null) {
                                        spaceCasToken = casToken
                                        Log.d(TAG, "捕获 cas token: $casToken")
                                        view.evaluateJavascript(JWT_HOOK_JS, null)
                                    }
                                    scheduleSettle(view, "space") {
                                        syncCookiesToJar(context, url)
                                        view.evaluateJavascript("(window.__captured_jwt || '')") { result ->
                                            val hookJwt = result?.trim('"')?.replace("\\\"", "\"")?.takeIf { it.isNotEmpty() }
                                            if (hookJwt != null && hookJwt.startsWith("eyJ")) {
                                                CredentialStore.getInstance(context)
                                                    .putString(CredentialStore.KEY_LIBRARY_JWT, hookJwt)
                                                Log.d(TAG, "图书馆 JWT 从 XHR hook 捕获, len=${hookJwt.length}")
                                                startWaterPhase()
                                            } else {
                                                val token = spaceCasToken
                                                if (token != null) {
                                                    scope.launch {
                                                        try {
                                                            val jwt = TongjiApp.getInstance().librarySpaceRepository
                                                                .fetchJwt(token)
                                                            if (jwt != null) {
                                                                CredentialStore.getInstance(context)
                                                                    .putString(CredentialStore.KEY_LIBRARY_JWT, jwt)
                                                                Log.d(TAG, "图书馆 JWT 从 API 获取, len=${jwt.length}")
                                                            } else {
                                                                Log.e(TAG, "图书馆 JWT 为空 (cas token 可能已消耗)")
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e(TAG, "图书馆 JWT 获取失败: ${e.message}")
                                                        }
                                                        startWaterPhase()
                                                    }
                                                } else {
                                                    Log.e(TAG, "未找到 cas token")
                                                    startWaterPhase()
                                                }
                                            }
                                        }
                                    }
                                    return@onPageFinished
                                }

                                if (loginPhase == LoginPhase.YIKATONG_DONE ||
                                    loginPhase == LoginPhase.SPACE_DONE ||
                                    loginPhase == LoginPhase.WATER_PENDING ||
                                    loginPhase == LoginPhase.WATER_DONE) {
                                    return@onPageFinished
                                }

                                // ===== 1.tongji 登录阶段 =====
                                if (host.contains("1.tongji.edu.cn") || host.contains("workbench.tongji.edu.cn")) {
                                    Log.d(TAG, "命中一系统域名，开始提取凭证")
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
                                            loginPhase = LoginPhase.LOGGED_IN
                                            Log.d(TAG, "导航到 all.tongji.edu.cn 获取 SSO cookie")
                                            view.loadUrl(ALL_TONGJI_SSO_URL)
                                            scope.launch {
                                                try { TongjiApp.getInstance().sessionRepository.refreshSessionUser() } catch (_: Exception) { }
                                            }
                                            return@onPageFinished
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
                                    try {
                                        val jsResult = result?.trim('"')?.replace("\\\"", "\"") ?: "{}"
                                        val json = org.json.JSONObject(jsResult)
                                        val uid = json.optString("uid", "")
                                        val aesKey = json.optString("aesKey", "").takeIf { it.isNotEmpty() }
                                        val aesIv = json.optString("aesIv", "").takeIf { it.isNotEmpty() }
                                        val sessionId = json.optString("sessionId", "").takeIf { it.isNotEmpty() }

                                        if (uid.isNotEmpty()) {
                                            val store = CredentialStore.getInstance(context)
                                            store.putString(CredentialStore.KEY_UID, uid)
                                            aesKey?.let { store.putString(CredentialStore.KEY_AES_KEY, it) }
                                            aesIv?.let { store.putString(CredentialStore.KEY_AES_IV, it) }
                                            sessionId?.let { store.putString(CredentialStore.KEY_SESSION_ID, it) }
                                            CampusModel.markValid()
                                            syncCookiesToJar(context, url)
                                            loginPhase = LoginPhase.LOGGED_IN
                                            view.loadUrl(ALL_TONGJI_SSO_URL)
                                            scope.launch {
                                                try { TongjiApp.getInstance().sessionRepository.refreshSessionUser() } catch (e: Exception) { Log.e(TAG, "session 刷新失败: ${e.message}") }
                                            }
                                        } else {
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
