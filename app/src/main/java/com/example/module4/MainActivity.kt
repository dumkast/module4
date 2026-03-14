package com.example.module4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)

        cityStatuses.clear()
        cityStatuses.addAll(cities.map { Triple(it,"Ожидание","") })

        setContent {
            Module4Theme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
                            statusText="Загружаем погоду для ${cities.size} городов..."
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
                                statusText="Загрузка отменена"
                                finalReport=""
                                cityWorkRequests.clear()
                                reportWorkId = null
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

    private fun startWeather() {
        cityWorkRequests = cities.map { city ->
            OneTimeWorkRequestBuilder<CityWeatherWorker>()
                .setInputData(workDataOf("city" to city))
                .addTag("weather_work")
                .build()
        }.toMutableList()

        val cityWorkIds = cityWorkRequests.map { it.id.toString() }.toTypedArray()

        val reportWork = OneTimeWorkRequestBuilder<ReportWorker>()
            .setInputData(
                workDataOf("city_work_ids" to cityWorkIds)
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

                        val inProcess = cityStatuses.count { it.second=="Загружается..." }
                        val doneCities = cityStatuses.filter { it.second=="Готово" }.map { it.first }
                        statusText = when {
                            doneCities.size == cities.size -> "Все данные получены, формируем отчёт..."
                            else -> "Готово: ${doneCities.joinToString(", ")}${if(inProcess>0) ", $inProcess в процессе..." else ""}"
                        }
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