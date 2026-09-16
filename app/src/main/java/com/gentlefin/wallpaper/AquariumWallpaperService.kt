package com.gentlefin.wallpaper

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

class AquariumWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = AquariumEngine()

    inner class AquariumEngine : Engine() {

        private var webView: WebView? = null
        private var isVisible = false
        private val handler = Handler(Looper.getMainLooper())
        private val frameIntervalMs = 33L

        private val debugLogLines = mutableListOf<String>()
        private val debugPaintBg = Paint().apply {
            color = Color.argb(200, 0, 0, 0)
            style = Paint.Style.FILL
        }
        private val debugPaintText = Paint().apply {
            color = Color.YELLOW
            textSize = 24f
            isAntiAlias = true
        }

        private fun logDebug(line: String) {
            synchronized(debugLogLines) {
                debugLogLines.add(line)
                if (debugLogLines.size > 12) debugLogLines.removeAt(0)
            }
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (isVisible) {
                    handler.postDelayed(this, frameIntervalMs)
                }
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            if (webView == null) {
                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this@AquariumWallpaperService))
                    .build()

                webView = WebView(this@AquariumWallpaperService).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        setRenderPriority(WebSettings.RenderPriority.HIGH)
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                            logDebug("[${cm.messageLevel()}] ${cm.message()} (baris ${cm.lineNumber()})")
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            return assetLoader.shouldInterceptRequest(request.url)
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError
                        ) {
                            super.onReceivedError(view, request, error)
                            logDebug("[LOAD-FAIL] ${request.url} -> ${error.description}")
                        }
                    }

                    loadUrl("https://appassets.androidplatform.net/assets/index.html")
                }
                val w = desiredMinimumWidth
                val h = desiredMinimumHeight
                webView?.layout(0, 0, w, h)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            webView?.layout(0, 0, width, height)
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK)
                    webView?.draw(canvas)

                    synchronized(debugLogLines) {
                        if (debugLogLines.isNotEmpty()) {
                            val lineHeight = 32f
                            val maxWidth = canvas.width.toFloat() - 24f
                            val wrapped = mutableListOf<String>()
                            for (line in debugLogLines) {
                                var remaining = line
                                while (remaining.isNotEmpty()) {
                                    var cut = debugPaintText.breakText(remaining, true, maxWidth, null)
                                    if (cut <= 0) cut = remaining.length
                                    wrapped.add(remaining.substring(0, cut))
                                    remaining = remaining.substring(cut)
                                }
                            }
                            val shown = if (wrapped.size > 20) wrapped.takeLast(20) else wrapped
                            val boxHeight = 20f + lineHeight * shown.size
                            canvas.drawRect(0f, 0f, canvas.width.toFloat(), boxHeight, debugPaintBg)
                            var y = 34f
                            for (line in shown) {
                                canvas.drawText(line, 12f, y, debugPaintText)
                                y += lineHeight
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) { }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                webView?.onResume()
                webView?.resumeTimers()
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
                webView?.onPause()
                webView?.pauseTimers()
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            webView?.dispatchTouchEvent(event)
            super.onTouchEvent(event)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
            webView?.destroy()
            webView = null
        }
    }
}
