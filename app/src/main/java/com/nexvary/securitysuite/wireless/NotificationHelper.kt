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
                NotificationChannel(CHANNEL, "Wireless Watch alerts", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    fun alert(term: String, device: DisplayDevice) {
        notify(
            key = "watch:" + term + ":" + device.observation.stableId,
            title = "NEXVARY Wireless Watch",
            text = (device.match?.label ?: device.observation.name ?: "Device") + " matched: " + term,
            severity = device.match?.severity ?: AlertSeverity.INFO,
        )
    }

    fun alertAttention(device: DisplayDevice) {
        val match = device.match ?: return
        if (match.severity.weight < AlertSeverity.HIGH.weight) return
        notify(
            key = "attention:" + match.label + ":" + device.observation.stableId,
            title = if (match.severity == AlertSeverity.CRITICAL) {
                "NEXVARY critical wireless event"
            } else {
                "NEXVARY wireless attention"
            },
            text = match.label + " · " + match.reason,
            severity = match.severity,
        )
    }

    private fun notify(key: String, title: String, text: String, severity: AlertSeverity) {
        val now = System.currentTimeMillis()
        val cooldown = if (severity == AlertSeverity.CRITICAL) 30_000L else 120_000L
        if (now - (lastAlert[key] ?: 0L) < cooldown) return
        lastAlert[key] = now

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(
                if (severity.weight >= AlertSeverity.HIGH.weight) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                },
            )
            .setAutoCancel(true)
            .build()
        manager?.notify(key.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL = "wireless_watch_alerts"
    }
}
