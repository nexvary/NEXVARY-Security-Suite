package com.nexvary.securitysuite.wireless

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val lastAlert = mutableMapOf<String, Long>()

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            manager?.createNotificationChannel(
                NotificationChannel(CHANNEL, "Wireless Watch", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    fun alert(term: String, device: DisplayDevice) {
        val now = System.currentTimeMillis()
        val key = term + ":" + device.observation.stableId
        if (now - (lastAlert[key] ?: 0L) < 60_000) return
        lastAlert[key] = now

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val label = device.match?.label ?: device.observation.name ?: "Device"
        val text = label + " matched: " + term
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("NEXVARY Wireless Watch")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        manager?.notify(key.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL = "wireless_watch"
    }
}
