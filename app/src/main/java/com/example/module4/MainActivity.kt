package com.example.module4

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.module4.ui.theme.AppColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var counterJob: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent(this, TimerService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (!checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    .equals(android.content.pm.PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        setContent {
            var seconds by remember { mutableStateOf(0) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Text(
                        text = "$seconds",
                        fontSize = 48.sp,
                        color = Color.Black
                    )
                    Button(
                        onClick = {
                            startForegroundService(serviceIntent)
                            counterJob?.cancel()
                            counterJob = lifecycleScope.launch {
                                var s = 0
                                while (true) {
                                    seconds = s
                                    delay(1000)
                                    s++
                                }
                            }
                        },
                        modifier = Modifier.width(200.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Button)
                    ) {
                        Text("Старт", color = Color.White)
                    }
                    Button(
                        onClick = {
                            stopService(serviceIntent)
                            counterJob?.cancel()
                        },
                        modifier = Modifier.width(200.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Button)
                    ) {
                        Text("Стоп", color = Color.White)
                    }
                }
            }
        }
    }
}