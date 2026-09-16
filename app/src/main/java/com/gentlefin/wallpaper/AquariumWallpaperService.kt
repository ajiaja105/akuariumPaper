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
import android.webkit.WebSettings
import android.webkit.WebView

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
            textSize = 28f
            isAntiAlias = true
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
                webView = WebView(this@AquariumWallpaperService).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        setRenderPriority(WebSettings.RenderPriority.HIGH)
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                            val line = "[${cm.messageLevel()}] ${cm.message()} (baris ${cm.lineNumber()})"
                            synchronized(debugLogLines) {
                                debugLogLines.add(line)
                                if (debugLogLines.size > 12) debugLogLines.removeAt(0)
                            }
                            return true
                        }
                    }
                    loadUrl("file:///android_asset/index.html")
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
                    webView?.draw(canvas)

                    synchronized(debugLogLines) {
                        if (debugLogLines.isNotEmpty()) {
                            val lineHeight = 34f
                            val boxHeight = 20f + lineHeight * debugLogLines.size
                            canvas.drawRect(0f, 0f, canvas.width.toFloat(), boxHeight, debugPaintBg)
                            var y = 40f
                            for (line in debugLogLines) {
                                canvas.drawText(line, 16f, y, debugPaintText)
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
        
