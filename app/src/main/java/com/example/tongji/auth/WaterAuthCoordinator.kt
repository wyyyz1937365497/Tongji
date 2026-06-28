package com.example.tongji.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.tongji.data.remote.api.WaterApi
import com.example.tongji.data.remote.model.WaterSsoCheckResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cookie
import okhttp3.HttpUrl
import org.json.JSONObject
import kotlin.coroutines.resume

private const val TAG = "WaterAuth"
private const val PAY_ORIGIN = "https://pay-yikatong.tongji.edu.cn"
private const val KS_ORIGIN = "https://ks.tongji.edu.cn"

class WaterAuthCoordinator(
    private val context: Context,
    private val waterApi: WaterApi,
    private val cookieJar: CookieJar
) {

    private var webView: WebView? = null

    data class AuthResult(
        val account: String,
        val aesKey: String,
        val password: String
    )

    suspend fun authenticate(): Result<AuthResult> = suspendCancellableCoroutine { continuation ->
        val wv = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            val ssoBox = mutableMapOf<String, Any>()

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    Log.d(TAG, "onPageFinished: $url")

                    // 等待并提取 sessionStorage.ano
                    if (url.startsWith(KS_ORIGIN)) {
                        view.postDelayed({
                            view.evaluateJavascript(
                                "(function() { return sessionStorage.getItem('ano') || ''; })();"
                            ) { result ->
                                val ano = result?.trim('"')?.replace("\\\"", "\"")?.takeIf { it.isNotEmpty() }
                                if (ano != null && !ssoBox.containsKey("completed")) {
                                    ssoBox["completed"] = true
                                    extractJsParams(view, ano, continuation)
                                }
                            }
                        }, 3000)
                    }
                }
            }

            addJavascriptInterface(WaterJsInterface(ssoBox), "WaterBridge")
            loadUrl(buildLoginUrl())
        }
        webView = wv
        continuation.invokeOnCancellation { webView?.destroy() }
    }

    private fun extractJsParams(
        view: WebView,
        account: String,
        continuation: kotlin.coroutines.Continuation<Result<AuthResult>>
    ) {
        view.evaluateJavascript(
            """
            (function() {
                var scripts = Array.from(document.scripts).map(s => s.src).filter(Boolean);
                var resources = performance.getEntriesByType('resource')
                    .map(e => e.name).filter(name => name.endsWith('.js'));
                return JSON.stringify({scripts: scripts, resources: resources});
            })();
            """.trimIndent()
        ) { result ->
            try {
                val json = org.json.JSONObject(result.trim('"').replace("\\\"", "\""))
                val urls = mutableListOf<String>()
                val scripts = json.optJSONArray("scripts")
                val resources = json.optJSONArray("resources")

                for (i in 0 until (scripts?.length() ?: 0)) {
                    val url = scripts?.optString(i) ?: continue
                    if (url.startsWith(KS_ORIGIN) && url.contains(".js") && !urls.contains(url)) {
                        urls.add(url)
                    }
                }
                for (i in 0 until (resources?.length() ?: 0)) {
                    val url = resources?.optString(i) ?: continue
                    if (url.startsWith(KS_ORIGIN) && url.contains(".js") && !urls.contains(url)) {
                        urls.add(url)
                    }
                }

                // Fetch JS content and parse params
                fetchAndParseJs(view, urls, account, continuation)
            } catch (e: Exception) {
                Log.e(TAG, "提取 JS URL 失败", e)
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun fetchAndParseJs(
        view: WebView,
        urls: List<String>,
        account: String,
        continuation: kotlin.coroutines.Continuation<Result<AuthResult>>
    ) {
        view.evaluateJavascript(
            """
            (async function() {
                var texts = [];
                var urls = ${org.json.JSONArray(urls)};
                for (var i = 0; i < urls.length; i++) {
                    try {
                        var r = await fetch(urls[i]);
                        if (r.ok) {
                            var t = await r.text();
                            if (t) texts.push(t);
                        }
                    } catch(e) {}
                }
                return JSON.stringify({texts: texts});
            })();
            """.trimIndent()
        ) { result ->
            try {
                val json = org.json.JSONObject(result.trim('"').replace("\\\"", "\""))
                val texts = json.optJSONArray("texts")
                val jsTexts = mutableListOf<String>()
                for (i in 0 until (texts?.length() ?: 0)) {
                    texts?.optString(i)?.let { jsTexts.add(it) }
                }

                val aesKey = findAesKey(jsTexts)
                val password = findPassword(jsTexts)

                if (aesKey != null && password != null) {
                    // Sync cookies
                    syncCookiesFromWebView(view)
                    continuation.resume(Result.success(AuthResult(account, aesKey, password)))
                } else {
                    continuation.resume(Result.failure(
                        RuntimeException("解析水控参数失败: aesKey=${aesKey != null}, password=${password != null}")
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析 JS 失败", e)
                continuation.resume(Result.failure(e))
            }
        }
    }

    private fun syncCookiesFromWebView(view: WebView) {
        try {
            val cookieManager = android.webkit.CookieManager.getInstance()
            val cookieStr = cookieManager.getCookie(KS_ORIGIN) ?: return
            val httpUrl = HttpUrl.Builder().scheme("https").host("ks.tongji.edu.cn").build()
            val cookies = cookieStr.split(";").mapNotNull { it.trim().split("=", limit = 2) }
                .filter { it.size == 2 }
                .map { (name, value) ->
                    Cookie.Builder()
                        .name(name.trim())
                        .value(value.trim())
                        .domain(httpUrl.host)
                        .path("/")
                        .expiresAt(System.currentTimeMillis() + 86400_000L)
                        .build()
                }
            cookieJar.saveFromResponse(httpUrl, cookies)
            Log.d(TAG, "已同步 ks cookies: ${cookies.size} 条")
        } catch (e: Exception) {
            Log.e(TAG, "同步 cookies 失败", e)
        }
    }

    private fun findAesKey(jsTexts: List<String>): String? {
        val regex = """["']([A-Za-z0-9+/]{22}==)["']""".toRegex()
        for (text in jsTexts) {
            for (match in regex.findAll(text)) {
                val candidate = match.groupValues[1]
                try {
                    val raw = Base64.decode(candidate, Base64.DEFAULT)
                    if (raw.size in listOf(16, 24, 32)) {
                        val around = text.substring(
                            maxOf(0, match.range.first - 500),
                            minOf(text.length, match.range.last + 1000)
                        )
                        if (around.contains("AES.encrypt") || around.contains("AES.decrypt")) {
                            return candidate
                        }
                    }
                } catch (_: Exception) { }
            }
        }
        return null
    }

    private fun findPassword(jsTexts: List<String>): String? {
        val regex = """userpassword\s*:\s*["']([^"']{6,100})["']""".toRegex()
        for (text in jsTexts) {
            for (match in regex.findAll(text)) {
                val c = match.groupValues[1]
                if (!c.lowercase().startsWith("string")) {
                    return c
                }
            }
        }
        return null
    }

    private fun buildLoginUrl(): String {
        val platUrl = "$PAY_ORIGIN/plat?loginFrom=h5"
        val target = "$PAY_ORIGIN/berserker-base/redirect?" +
                "type=url&url=${java.net.URLEncoder.encode(platUrl, "UTF-8")}"
        return "$PAY_ORIGIN/berserker-auth/cas/redirect/bamboocloud?" +
                "targetUrl=${java.net.URLEncoder.encode(target, "UTF-8")}"
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }

    // Dummy JS interface for potential future use
    private class WaterJsInterface(private val ssoBox: MutableMap<String, Any>) {
        @android.webkit.JavascriptInterface
        fun onSsoCheck(data: String) {
            ssoBox["ssoCheck"] = data
        }
    }
}