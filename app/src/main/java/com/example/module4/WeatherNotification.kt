package com.example.module4

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object WeatherNotification {
    private const val CHANNEL_ID = "weather_channel"
    private const val NOTIFICATION_ID = 1001

    fun createForegroundNotification(context: Context, text: String): Notification {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Прогноз погоды",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Прогноз погоды")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    fun updateNotification(context: Context, text: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createForegroundNotification(context, text)
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun removeNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}