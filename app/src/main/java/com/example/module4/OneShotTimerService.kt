package com.example.module4

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class OneShotTimerService : Service() {
    private val CHANNEL_ID = "OneShotTimerChannel"
    private val NOTIF_ID = 3
    private var serviceJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getIntExtra("seconds", 10) ?: 10
        createNotificationChannel()
        serviceJob = CoroutineScope(Dispatchers.Default).launch {
            delay(seconds * 1000L)
            showNotification()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "One-Shot Timer",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    private fun showNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Таймер завершён")
            .setContentText("Время вышло!")
            .setSmallIcon(R.drawable.ic_timer)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIF_ID, notif)
    }
}