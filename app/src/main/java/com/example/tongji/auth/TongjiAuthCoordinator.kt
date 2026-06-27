package com.example.tongji.auth

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TongjiAuthCoordinator(private val context: Context) {

    private var webView: WebView? = null

    suspend fun startFreshInteractiveLogin(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val wv = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (url.contains("workbench.tongji.edu.cn") || url.contains("1.tongji.edu.cn")) {
                        view.evaluateJavascript(
                            "(function() { return JSON.stringify({uid: localStorage.getItem('sessiondata') || '{}'}); })();"
                        ) { result ->
                            try {
                                val json = org.json.JSONObject(result.trim('"').replace("\\\"", "\""))
                                val sessionData = json.optJSONObject("uid")
                                if (sessionData != null) {
                                    val uid = sessionData.optString("uid")
                                    val aesKey = sessionData.optString("aesKey")
                                    val aesIv = sessionData.optString("aesIv")
                                    if (uid.isNotEmpty()) {
                                        val store = CredentialStore.getInstance(context)
                                        store.putString(CredentialStore.KEY_UID, uid)
                                        continuation.resume(Result.success(Unit))
                                        return@evaluateJavascript
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
            loadUrl("https://1.tongji.edu.cn")
        }
        webView = wv
        continuation.invokeOnCancellation { webView?.destroy() }
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }
}
