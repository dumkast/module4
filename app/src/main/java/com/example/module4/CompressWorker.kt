package com.example.module4

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ProgressManager {
    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()
    fun updateProgress(value: Int) {
        _progress.value = value
    }
    fun resetProgress() {
        _progress.value = 0
    }
}

class CompressWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            Log.d("PhotoWorker", "CompressWorker: начинаем сжатие")
            for (i in 1..100) {
                delay(50)
                ProgressManager.updateProgress(i)
            }
            Log.d("PhotoWorker", "CompressWorker: сжатие завершено")
            Result.success(workDataOf("result" to "Фото сжато"))
        } catch (e: Exception) {
            Log.e("PhotoWorker", "CompressWorker ошибка: ${e.message}")
            Result.failure()
        }
    }
}