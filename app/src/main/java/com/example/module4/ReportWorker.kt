package com.example.module4

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.*
import kotlinx.coroutines.delay
import kotlin.random.Random
import java.util.UUID

class ReportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val totalCities = inputData.getInt("total_cities", 3)
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                WeatherNotification.createForegroundNotification(
                    applicationContext,
                    "Формируем итоговый отчёт..."
                ),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        )
        delay(1000)
        val allWorkInfos = WorkManager.getInstance(applicationContext)
            .getWorkInfosByTag("weather_work")
            .get()
        val cityReports = mutableListOf<String>()
        val temps = mutableListOf<Int>()
        val currentWorkIdStrings = inputData.getStringArray("city_work_ids") ?: emptyArray()
        val currentWorkIds = currentWorkIdStrings.map { UUID.fromString(it) }.toSet()
        var completedCount = 0
        for (workInfo in allWorkInfos) {
            if (workInfo.state == WorkInfo.State.SUCCEEDED &&
                currentWorkIds.contains(workInfo.id)) {
                val city = workInfo.outputData.getString("city")
                val temp = workInfo.outputData.getInt("temperature", Int.MIN_VALUE)
                val conditions = workInfo.outputData.getString("conditions")
                if (city != null && temp != Int.MIN_VALUE && conditions != null) {
                    cityReports.add("$city: $temp°C, $conditions")
                    temps.add(temp)
                    completedCount++
                }
            }
        }
        WeatherNotification.updateNotification(
            applicationContext,
            "Загружено $completedCount из $totalCities городов"
        )
        val avg = if(temps.isNotEmpty()) temps.average().toInt() else 0
        val report = if (cityReports.isNotEmpty()) {
            "ИТОГОВЫЙ ПРОГНОЗ:\n\n" +
                    cityReports.joinToString("\n") +
                    "\n\nСредняя температура: $avg°C"
        } else {
            "Не удалось получить данные о погоде"
        }
        WeatherNotification.updateNotification(
            applicationContext,
            "Отчёт готов! Средняя температура: $avg°C"
        )
        return Result.success(workDataOf("report" to report))
    }
}