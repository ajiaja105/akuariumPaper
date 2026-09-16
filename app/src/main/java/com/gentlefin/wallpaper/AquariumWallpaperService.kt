package com.gentlefin.wallpaper

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * ==============================================================
 *  CATATAN PENTING - BACA DULU SEBELUM BUILD
 * ==============================================================
 *
 * Pendekatan di file ini: WebView dibuat DI MEMORI (tidak pernah
 * ditempel ke Window/Activity sungguhan), lalu isinya digambar
 * manual berulang-ulang (~30x/detik) ke Canvas milik Surface
 * wallpaper lewat webView.draw(canvas). Ini pendekatan paling
 * umum dipakai contoh WebView-di-WallpaperService yang beredar.
 *
 * TAPI - ini justru titik paling rawan buat kasus kamu: WebGL
 * (dipakai skrip akuarium via Three.js) di banyak device BUTUH
 * WebView benar2 "attached ke window" supaya GPU driver mau
 * bikin context render. WebView yang cuma hidup di memori (spt
 * kode di bawah) beberapa kali dilaporkan gagal dengan pesan
 * "Error creating WebGL context" - walau kode HTML/JS-nya sendiri
 * 100% benar dan lancar kalau dibuka di browser/Activity biasa.
 *
 * Kalau nanti muncul error itu pas dicoba di HP kamu, ini rencana
 * cadangan yang perlu diriset lebih lanjut (BELUM diimplementasikan
 * di file ini, karena butuh testing langsung di device yang tidak
 * bisa saya lakukan dari sini):
 *   1. Teknik "VirtualDisplay" - render WebView ke display virtual
 *      terpisah yang benar2 "hidup", lalu proyeksikan hasilnya ke
 *      Surface wallpaper. Dilaporkan tidak 100% konsisten di semua
 *      versi Android (pernah jalan di Android 11, error di Android 7).
 *   2. Alternatif paling aman: turunkan skrip jadi Canvas2D biasa
 *      (bukan WebGL/Three.js) - lebih terbatas visualnya, tapi
 *      dijamin jalan di WallpaperService tanpa masalah GPU context.
 *
 * Uji coba LANGSUNG di HP kamu adalah cara paling pasti buat tau
 * apakah pendekatan sederhana ini cukup atau perlu opsi cadangan.
 * ==============================================================
 */
class AquariumWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = AquariumEngine()

    inner class AquariumEngine : Engine() {

        private var webView: WebView? = null
        private var isVisible = false
        private val handler = Handler(Looper.getMainLooper())
        private val frameIntervalMs = 33L // ~30 fps target buat loop gambar manual ini

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
            setTouchEventsEnabled(true) // wajib, supaya bisa terima tap "Feed Fish"
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
                    webChromeClient = WebChromeClient()

                    // index.html = salinan Gfv9.html, ditaruh di app/src/main/assets/
                    // PENTING: semua referensi .glb / get-model.js di dalam HTML
                    // HARUS diubah dulu supaya load LANGSUNG dari folder assets
                    // (file:///android_asset/namafile.glb), BUKAN lewat
                    // guardedGlbUrl()/Netlify Function - itu tidak ada di app ini.
                    loadUrl("file:///android_asset/index.html")
                }
                // Ukuran WebView disamakan dengan ukuran Surface wallpaper,
                // supaya konten tergambar penuh layar, bukan cuma pojok kecil.
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
                }
            } catch (e: Exception) {
                // Surface bisa saja sudah tidak valid (mis. wallpaper baru saja
                // diganti/dimatikan) - tangkap supaya app tidak crash.
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) { /* abaikan */ }
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
                // Penting: hentikan loop gambar & pause WebView saat wallpaper
                // tidak terlihat (misal user buka app lain) - hemat baterai/CPU.
                handler.removeCallbacks(drawRunnable)
                webView?.onPause()
                webView?.pauseTimers()
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            // Teruskan tap ke WebView, supaya tombol "Feed Fish" di
            // dalam HTML bisa dipencet dari layar utama/home screen.
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
