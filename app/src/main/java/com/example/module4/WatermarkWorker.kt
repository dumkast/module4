package com.example.module4

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class WatermarkWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            Log.d("PhotoWorker", "WatermarkWorker: добавляем водяной знак")
            for (i in 1..100) {
                delay(40)
                ProgressManager.updateProgress(i)
            }
            Log.d("PhotoWorker", "WatermarkWorker: водяной знак добавлен")
            Result.success()
        } catch (e: Exception) {
            Log.e("PhotoWorker", "WatermarkWorker ошибка: ${e.message}")
            Result.failure()
        }
    }
}