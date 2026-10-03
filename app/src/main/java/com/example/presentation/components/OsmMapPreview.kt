package com.example.presentation.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OsmMapPreview(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    val delta = 0.008
    val bbox = listOf(
        longitude - delta,
        latitude - delta,
        longitude + delta,
        latitude + delta
    ).joinToString("%2C")
    val marker = Uri.encode("${latitude},${longitude}")
    val url = "https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${marker}"

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return true
                    }
                }
                tag = url
                loadUrl(url)
            }
        },
        update = { view ->
            if (view.tag != url) {
                view.tag = url
                view.loadUrl(url)
            }
        }
    )
}
