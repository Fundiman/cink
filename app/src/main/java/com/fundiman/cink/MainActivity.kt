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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val receiver = LockReceiver()
    private lateinit var serviceSwitch: SwitchCompat
    private lateinit var selectedFileName: TextView
    private lateinit var applyThemeBtn: Button
    private var selectedUri: Uri? = null

    // SAF File Picker Launcher
    private val pickZipLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            // Update UI to show we have a file
            selectedFileName.text = "Selected: ${it.lastPathSegment?.substringAfterLast('/') ?: "theme.zip"}"
            applyThemeBtn.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val selectFileBtn = findViewById<Button>(R.id.selectFileBtn)
        selectedFileName = findViewById(R.id.selectedFileName)
        applyThemeBtn = findViewById(R.id.applyThemeBtn)
        val testBtn = findViewById<Button>(R.id.testBtn)
        serviceSwitch = findViewById(R.id.serviceSwitch)

        val prefs = getSharedPreferences("cink_prefs", Context.MODE_PRIVATE)

        // Notification Permission for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Receiver Registration
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        serviceSwitch.isChecked = prefs.getBoolean("service_enabled", false)

        // Open File Picker
        selectFileBtn.setOnClickListener {
            pickZipLauncher.launch("application/zip")
        }

        // Apply Local Zip via SAF
        applyThemeBtn.setOnClickListener {
            selectedUri?.let { uri ->
                Toast.makeText(this, "applying theme...", Toast.LENGTH_SHORT).show()
                installZipFromUri(uri)
            }
        }

        // Manual Test
        testBtn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "lockscreen test launched!", Toast.LENGTH_SHORT).show()
                startForegroundService(Intent(this, LockService::class.java))
            }
        }

        // Master Toggle
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

    private fun installZipFromUri(uri: Uri) {
        thread {
            try {
                val targetDir = File(filesDir, "lockscreen")
                if (targetDir.exists()) targetDir.deleteRecursively()
                targetDir.mkdirs()

                // Use contentResolver to open the stream from SAF
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val file = File(targetDir, entry.name)
                            if (entry.isDirectory) {
                                file.mkdirs()
                            } else {
                                file.parentFile?.mkdirs()
                                FileOutputStream(file).use { output ->
                                    zip.copyTo(output)
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }
                runOnUiThread {
                    Toast.makeText(this, "theme applied successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "error: ${e.message}", Toast.LENGTH_LONG).show()
                }
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