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
                            "(function() { var sd = localStorage.getItem('sessiondata'); if (sd) { try { var p = JSON.parse(sd); return JSON.stringify({uid: p.uid || p.UID || '', name: p.name || p.xm || '', aesKey: p.aesKey || '', aesIv: p.aesIv || ''}); } catch(e) {} } return JSON.stringify({}); })();"
                        ) { result ->
                            try {
                                val json = org.json.JSONObject(result.trim('"').replace("\\\"", "\""))
                                val uid = json.optString("uid")
                                val name = json.optString("name", "").takeIf { it.isNotEmpty() }
                                val aesKey = json.optString("aesKey", "").takeIf { it.isNotEmpty() }
                                val aesIv = json.optString("aesIv", "").takeIf { it.isNotEmpty() }
                                if (uid.isNotEmpty()) {
                                    val store = CredentialStore.getInstance(context)
                                    store.putString(CredentialStore.KEY_UID, uid)
                                    name?.let { store.putString(CredentialStore.KEY_NAME, it) }
                                    aesKey?.let { store.putString(CredentialStore.KEY_AES_KEY, it) }
                                    aesIv?.let { store.putString(CredentialStore.KEY_AES_IV, it) }
                                    continuation.resume(Result.success(Unit))
                                    return@evaluateJavascript
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
