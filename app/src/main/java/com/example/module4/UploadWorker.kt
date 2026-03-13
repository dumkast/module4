package com.example.module4

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            Log.d("PhotoWorker", "UploadWorker: загружаем в облако")
            for (i in 1..100) {
                delay(30) // Имитация работы
                ProgressManager.updateProgress(i)
            }
            Log.d("PhotoWorker", "UploadWorker: загрузка завершена")
            Result.success()
        } catch (e: Exception) {
            Log.e("PhotoWorker", "UploadWorker ошибка: ${e.message}")
            Result.failure()
        }
    }
}