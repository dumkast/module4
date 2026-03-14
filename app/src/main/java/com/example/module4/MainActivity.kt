package com.example.module4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.module4.ui.theme.Module4Theme
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val cities = listOf("Москва", "Нью-Йорк", "Лондон")
    private lateinit var workManager: WorkManager
    private val cityStatuses = mutableStateListOf<Triple<String,String,String>>()
    private var finalReport by mutableStateOf("")
    private var statusText by mutableStateOf("Готов начать")
    private var buttonEnabled by mutableStateOf(true)
    private var showCancel by mutableStateOf(false)
    private var cityWorkRequests = mutableListOf<OneTimeWorkRequest>()
    private var reportWorkId: UUID? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        cityStatuses.clear()
        cityStatuses.addAll(cities.map { Triple(it,"Ожидание","") })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            Module4Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Прогноз погоды", style = MaterialTheme.typography.headlineMedium)
                        Text(statusText, style = MaterialTheme.typography.bodyLarge)
                        cityStatuses.forEach { (city,status,temp) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ){
                                Row(
                                    verticalAlignment=Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier=Modifier.fillMaxSize()
                                ){
                                    Column{
                                        Text(city, style=MaterialTheme.typography.titleMedium)
                                        Text(status)
                                    }
                                    if(status=="Загружается...") CircularProgressIndicator(modifier=Modifier.size(24.dp))
                                    else if(temp.isNotEmpty()) Text(temp)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically){
                            Button(onClick={
                                startWeather()
                                buttonEnabled=false
                                showCancel=true
                                statusText="Загрузка..."
                            }, enabled=buttonEnabled){
                                Text(if(buttonEnabled)"Собрать прогноз" else "В процессе...")
                            }
                            if(showCancel){
                                Spacer(Modifier.width(16.dp))
                                Button(onClick={
                                    workManager.cancelAllWorkByTag("weather_work")
                                    cityStatuses.clear()
                                    cityStatuses.addAll(cities.map { Triple(it,"Ожидание","") })
                                    buttonEnabled=true
                                    showCancel=false
                                    statusText="Готов начать"
                                    finalReport=""
                                    cityWorkRequests.clear()
                                    reportWorkId = null
                                    WeatherNotification.removeNotification(applicationContext)
                                }){ Text("Отменить") }
                            }
                        }
                        if(finalReport.isNotEmpty()){
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier=Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ){ Text(finalReport) }
                        }
                    }
                }
            }
        }
    }

    private fun startWeather() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                statusText = "Нет разрешения на уведомления"
                buttonEnabled = true
                showCancel = false
                return
            }
        }
        WeatherNotification.createForegroundNotification(
            applicationContext,
            "Начинаем загрузку..."
        )
        cityWorkRequests = cities.mapIndexed { index, city ->
            OneTimeWorkRequestBuilder<CityWeatherWorker>()
                .setInputData(
                    workDataOf(
                        "city" to city,
                        "city_index" to index,
                        "total_cities" to cities.size
                    )
                )
                .addTag("weather_work")
                .build()
        }.toMutableList()
        val cityWorkIds = cityWorkRequests.map { it.id.toString() }.toTypedArray()
        val reportWork = OneTimeWorkRequestBuilder<ReportWorker>()
            .setInputData(
                workDataOf(
                    "city_work_ids" to cityWorkIds,
                    "total_cities" to cities.size
                )
            )
            .addTag("weather_work")
            .build()
        reportWorkId = reportWork.id
        workManager.beginWith(cityWorkRequests)
            .then(reportWork)
            .enqueue()
        cities.forEachIndexed { index, city ->
            workManager.getWorkInfoByIdLiveData(cityWorkRequests[index].id)
                .observe(this) { info ->
                    info?.let {
                        val status = when(it.state){
                            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> "Загружается..."
                            WorkInfo.State.SUCCEEDED -> "Готово"
                            WorkInfo.State.CANCELLED -> "Отменено"
                            else -> "Ожидание"
                        }
                        val temp = if(it.state==WorkInfo.State.SUCCEEDED){
                            val t = it.outputData.getInt("temperature",0)
                            val cond = it.outputData.getString("conditions")?:""
                            "$t°C, $cond"
                        } else ""
                        cityStatuses[index] = Triple(city,status,temp)
                    }
                }
        }
        workManager.getWorkInfoByIdLiveData(reportWork.id)
            .observe(this) { info ->
                if(info?.state==WorkInfo.State.SUCCEEDED){
                    finalReport = info.outputData.getString("report")?:""
                    statusText = "Отчёт готов!"
                    buttonEnabled = true
                    showCancel = false
                    cityWorkRequests.clear()
                    reportWorkId = null
                }
            }
    }
}