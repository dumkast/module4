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

        setForegroundAsync(
            ForegroundInfo(
                Random.nextInt(),
                WeatherNotification.createForegroundNotification(applicationContext, "Загружается $city..."),
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