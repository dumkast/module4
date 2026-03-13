package com.example.module4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.*
import com.example.module4.ui.theme.Module4Theme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val workManager = WorkManager.getInstance(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Module4Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoProcessingScreen(workManager)
                }
            }
        }
    }
}

@Composable
fun PhotoProcessingScreen(workManager: WorkManager) {
    var currentStep by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var resultLink by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showInitialDelay by remember { mutableStateOf(false) }
    val progress by ProgressManager.progress.collectAsStateWithLifecycle(0)
    LaunchedEffect(showInitialDelay) {
        if (showInitialDelay) {
            delay(1500)
            showInitialDelay = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                errorMessage.isNotEmpty() -> "Ошибка!"
                resultLink.isNotEmpty() -> "Фото загружено!"
                showInitialDelay -> "Запущена обработка..."
                isProcessing -> currentStep
                else -> "Готов к работе"
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                errorMessage.isNotEmpty() -> MaterialTheme.colorScheme.error
                resultLink.isNotEmpty() -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (isProcessing || currentStep.isNotEmpty() || errorMessage.isNotEmpty() || resultLink.isNotEmpty() || showInitialDelay) {
            Text(
                text = when {
                    errorMessage.isNotEmpty() -> errorMessage
                    resultLink.isNotEmpty() -> resultLink
                    else -> "Это может занять несколько секунд..."
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        if (isProcessing || showInitialDelay) {
            LinearProgressIndicator(
                progress = { if (showInitialDelay) 0f else progress / 100f },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                isProcessing = true
                showInitialDelay = true
                errorMessage = ""
                resultLink = ""
                currentStep = ""
                ProgressManager.resetProgress()
                startPhotoProcessing(workManager) { step, error, link ->
                    currentStep = step ?: ""
                    errorMessage = error ?: ""
                    resultLink = link ?: ""
                    isProcessing = error == null && link == null
                }
            },
            enabled = !isProcessing && !showInitialDelay,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = if (isProcessing || showInitialDelay) "Обработка..." else "Начать обработку и загрузку",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun startPhotoProcessing(
    workManager: WorkManager,
    onUpdate: (String?, String?, String?) -> Unit
) {
    val compressWork = OneTimeWorkRequestBuilder<CompressWorker>().build()
    val watermarkWork = OneTimeWorkRequestBuilder<WatermarkWorker>().build()
    val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>().build()

    workManager.getWorkInfoByIdLiveData(compressWork.id)
        .observeForever { workInfo ->
            workInfo?.let { info ->
                when (info.state) {
                    WorkInfo.State.RUNNING -> onUpdate("Сжимаем фото...", null, null)
                    WorkInfo.State.FAILED -> onUpdate(null, "Ошибка при сжатии фото", null)
                    else -> {}
                }
            }
        }
    workManager.getWorkInfoByIdLiveData(watermarkWork.id)
        .observeForever { workInfo ->
            workInfo?.let { info ->
                when (info.state) {
                    WorkInfo.State.RUNNING -> onUpdate("Добавляем водяной знак...", null, null)
                    WorkInfo.State.FAILED -> onUpdate(null, "Ошибка при добавлении водяного знака", null)
                    else -> {}
                }
            }
        }
    workManager.getWorkInfoByIdLiveData(uploadWork.id)
        .observeForever { workInfo ->
            workInfo?.let { info ->
                when (info.state) {
                    WorkInfo.State.RUNNING -> onUpdate("Загружаем в облако...", null, null)
                    WorkInfo.State.SUCCEEDED -> {
                        val link = "Ссылка на загруженное фото: https://cloud.example.com/uploaded_${System.currentTimeMillis()}.jpg"
                        onUpdate(null, null, link)
                    }
                    WorkInfo.State.FAILED -> onUpdate(null, "Ошибка при загрузке фото", null)
                    else -> {}
                }
            }
        }
    workManager.beginWith(compressWork)
        .then(watermarkWork)
        .then(uploadWork)
        .enqueue()
}