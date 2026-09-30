package com.nexvary.securitysuite.wireless

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class WirelessMonitorService : Service() {
    private lateinit var ble: BleScanner
    private lateinit var wifi: WifiScanner

    override fun onCreate() {
        super.onCreate()
        ble = BleScanner(this, ObservationBus::emit)
        wifi = WifiScanner(this, ObservationBus::emit)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("NEXVARY Security Suite")
                .setContentText("Wireless Watch is observing nearby broadcasts")
                .setOngoing(true)
                .setSilent(true)
                .build(),
        )
        val bleStarted = ble.start()
        val wifiStarted = wifi.start()
        ObservationBus.setActive(bleStarted || wifiStarted)
        if (!bleStarted && !wifiStarted) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        ble.stop()
        wifi.stop()
        ObservationBus.setActive(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Wireless Watch service",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private companion object {
        const val CHANNEL_ID = "wireless_watch_service"
        const val NOTIFICATION_ID = 4101
    }
}
