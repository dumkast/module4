package com.example.module4

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    private val CHANNEL_ID = "TimerChannel"
    private val NOTIF_ID = 1
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        runnable = object : Runnable {
            override fun run() {
                seconds++
                updateNotification(seconds)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification(seconds))
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(seconds: Int) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Таймер работает")
        .setContentText("Прошло $seconds секунд")
        .setSmallIcon(R.drawable.ic_timer)
        .setOngoing(true)
        .build()

    private fun updateNotification(seconds: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification(seconds))
    }
}