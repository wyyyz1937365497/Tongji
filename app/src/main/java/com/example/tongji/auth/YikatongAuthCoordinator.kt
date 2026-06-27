package com.example.tongji.auth

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class YikatongAuthCoordinator(private val context: Context) {

    private var webView: WebView? = null

    suspend fun authenticate(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val wv = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (url.contains("pay-yikatong.tongji.edu.cn")) {
                        view.evaluateJavascript(
                            "(function() { return JSON.stringify({token: localStorage.getItem('synjones-auth') || '', cookie: document.cookie}); })();"
                        ) { result ->
                            try {
                                val json = org.json.JSONObject(result.trim('"').replace("\\\"", "\""))
                                val token = json.optString("token")
                                val cookie = json.optString("cookie")
                                if (token.isNotEmpty()) {
                                    val store = CredentialStore.getInstance(context)
                                    store.putString(CredentialStore.KEY_YIKATONG_TOKEN, token)
                                    store.putString(CredentialStore.KEY_YIKATONG_COOKIE, cookie)
                                    continuation.resume(Result.success(Unit))
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
            loadUrl("https://pay-yikatong.tongji.edu.cn")
        }
        webView = wv
        continuation.invokeOnCancellation { webView?.destroy() }
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }
}
