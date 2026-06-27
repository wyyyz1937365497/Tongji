package com.example.tongji.auth

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LibrarySpaceAuthCoordinator(private val context: Context) {

    private var webView: WebView? = null

    suspend fun authenticate(): Result<String> = suspendCancellableCoroutine { continuation ->
        val wv = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (url.contains("space.tongji.edu.cn")) {
                        view.evaluateJavascript(
                            "(function() { return localStorage.getItem('jwt') || ''; })();"
                        ) { result ->
                            val jwt = result.trim('"').replace("\\\"", "")
                            if (jwt.isNotEmpty()) {
                                val store = CredentialStore.getInstance(context)
                                store.putString(CredentialStore.KEY_LIBRARY_JWT, jwt)
                                continuation.resume(Result.success(jwt))
                            }
                        }
                    }
                }
            }
            loadUrl("https://space.tongji.edu.cn")
        }
        webView = wv
        continuation.invokeOnCancellation { webView?.destroy() }
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }
}
