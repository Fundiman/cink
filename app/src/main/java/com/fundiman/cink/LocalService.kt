package com.fundiman.cink

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

class LockService : Service() {

    private var windowManager: WindowManager? = null
    private var webView: WebView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val indexFile = File(filesDir, "lockscreen/index.html")
        if (indexFile.exists()) {
            showLockOverlay(indexFile.absolutePath)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    private fun showLockOverlay(filePath: String) {
        if (webView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            overScrollMode = View.OVER_SCROLL_NEVER

            // bridge to kill the overlay
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun dismiss() {
                    Handler(Looper.getMainLooper()).post { removeOverlay() }
                }
            }, "CinkNative")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript("""
                        window.cink = {
                            unlock: function() {
                                if (window.CinkNative) { CinkNative.dismiss(); }
                            }
                        };
                    """.trimIndent(), null)
                }
                override fun onReceivedError(view: WebView?, req: WebResourceRequest?, err: WebResourceError?) {
                    removeOverlay() // fail-safe
                }
            }
            loadUrl("file:///$filePath")
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        // full game mode: hide nav keys and status bar
        params.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN)

        // use the notch area
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        windowManager?.addView(webView, params)
    }

    private fun removeOverlay() {
        if (webView != null) {
            try {
                windowManager?.removeView(webView)
            } catch (e: Exception) {}
            webView = null
        }
        stopSelf()
    }

    private fun createNotification() {
        val channelId = "cink_lock_service"
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, "cink active", NotificationManager.IMPORTANCE_MIN)
        manager.createNotificationChannel(channel)

        val configIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, configIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("cink is active")
            .setContentText("tap here to configure or stop the service")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }
}