package com.fundiman.cink

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class LockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("cink_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("service_enabled", false)

        val serviceIntent = Intent(context, LockService::class.java)

        // logic: if it's a BOOT event, start it NO MATTER WHAT.
        // if it's a SCREEN event, respect the user's current session toggle.
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // auto-enable the flag on boot so it stays active for screen events too
            prefs.edit().putBoolean("service_enabled", true).apply()
            startTheService(context, serviceIntent)
        }
        else if (isEnabled && (intent.action == Intent.ACTION_SCREEN_OFF || intent.action == Intent.ACTION_SCREEN_ON)) {
            startTheService(context, serviceIntent)
        }
    }

    private fun startTheService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}