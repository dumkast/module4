package com.example.module4

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlin.random.Random

class CityWeatherWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val city = inputData.getString("city") ?: return Result.failure()
        val totalCities = inputData.getInt("total_cities", 3)
        val currentIndex = inputData.getInt("city_index", 0)
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                WeatherNotification.createForegroundNotification(
                    applicationContext,
                    "Загружается $city... (${currentIndex + 1}/$totalCities)"
                ),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        )
        delay(2000 + Random.nextLong(0, 1000))
        val temperature = Random.nextInt(-10, 30)
        val conditions = listOf("Солнечно","Облачно","Дождь","Снег").random()
        return Result.success(
            workDataOf(
                "city" to city,
                "temperature" to temperature,
                "conditions" to conditions
            )
        )
    }
}