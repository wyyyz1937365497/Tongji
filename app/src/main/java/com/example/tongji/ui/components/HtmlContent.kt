package com.example.tongji.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlContent(
    html: String,
    baseUrl: String = "about:blank",
    modifier: Modifier = Modifier
) {
    val textColor = String.format("#%06X", 0xFFFFFF and MaterialTheme.colorScheme.onSurface.toArgb())
    val bgColor = String.format("#%06X", 0xFFFFFF and MaterialTheme.colorScheme.surface.toArgb())
    val linkColor = String.format("#%06X", 0xFFFFFF and MaterialTheme.colorScheme.primary.toArgb())

    val wrappedHtml = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <style>
            html, body {
                margin: 0;
                padding: 0;
                color: $textColor;
                background-color: $bgColor;
                line-height: 1.6;
                font-size: 15px;
                word-wrap: break-word;
                overflow-x: hidden;
            }
            body { padding: 4px 4px 24px 4px; }
            p { margin: 0.6em 0; }
            img { max-width: 100% !important; height: auto !important; display: block; margin: 8px auto; }
            table { max-width: 100% !important; border-collapse: collapse; }
            td, th { padding: 4px 6px; }
            a { color: $linkColor; text-decoration: none; }
            span { white-space: normal; }
        </style>
        </head>
        <body>$html</body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.defaultTextEncodingName = "utf-8"
                settings.blockNetworkImage = false
                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                setBackgroundColor(Color.TRANSPARENT)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                baseUrl,
                wrappedHtml,
                "text/html",
                "utf-8",
                null
            )
        },
        modifier = modifier
    )
}
