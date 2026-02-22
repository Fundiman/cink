package com.fundiman.cink

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val receiver = LockReceiver()
    private lateinit var serviceSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val urlInput = findViewById<EditText>(R.id.urlInput)
        val downloadBtn = findViewById<Button>(R.id.downloadBtn)
        val testBtn = findViewById<Button>(R.id.testBtn)
        serviceSwitch = findViewById(R.id.serviceSwitch)

        val prefs = getSharedPreferences("cink_prefs", Context.MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        serviceSwitch.isChecked = prefs.getBoolean("service_enabled", false)

        downloadBtn.setOnClickListener {
            val url = urlInput.text.toString()
            if (url.isNotEmpty()) {
                Toast.makeText(this, "downloading...", Toast.LENGTH_SHORT).show()
                downloadAndInstallZip(url)
            } else {
                Toast.makeText(this, "enter a url first!", Toast.LENGTH_SHORT).show()
            }
        }

        // manual test
        testBtn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "lockscreen test launched!", Toast.LENGTH_SHORT).show()
                startForegroundService(Intent(this, LockService::class.java))
            }
        }

        // master toggle
        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!Settings.canDrawOverlays(this)) {
                    serviceSwitch.isChecked = false
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    prefs.edit().putBoolean("service_enabled", true).apply()
                    Toast.makeText(this, "lockscreen enabled", Toast.LENGTH_SHORT).show()
                }
            } else {
                prefs.edit().putBoolean("service_enabled", false).apply()
                stopService(Intent(this, LockService::class.java))
                Toast.makeText(this, "lockscreen disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadAndInstallZip(zipUrl: String) {
        thread {
            try {
                val connection = URL(zipUrl).openConnection()
                connection.connect()
                val targetDir = File(filesDir, "lockscreen")
                if (targetDir.exists()) targetDir.deleteRecursively()
                targetDir.mkdirs()
                ZipInputStream(connection.getInputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val file = File(targetDir, entry.name)
                        if (entry.isDirectory) file.mkdirs()
                        else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { zip.copyTo(it) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                runOnUiThread { Toast.makeText(this, "theme applied successfully!", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "error: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("cink_prefs", Context.MODE_PRIVATE)
        serviceSwitch.isChecked = prefs.getBoolean("service_enabled", false)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(receiver) } catch (e: Exception) {}
    }
}