package com.example.module4

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var randomNumberService: RandomNumberService? = null
    private var serviceBound by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RandomNumberService.LocalBinder
            randomNumberService = binder.getService()
            serviceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            randomNumberService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var number by remember { mutableStateOf(0) }
            val backgroundColor = Color.White
            val buttonColor = Color(0xFF388E3C)
            LaunchedEffect(serviceBound) {
                if (serviceBound) {
                    while (serviceBound) {
                        randomNumberService?.let {
                            number = it.randomValueFlow.value
                        }
                        kotlinx.coroutines.delay(1000)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Случайное число",
                        fontSize = 24.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = number.toString(),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (!serviceBound) {
                        Button(
                            onClick = {
                                val intent = Intent(this@MainActivity, RandomNumberService::class.java)
                                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Подключиться", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (serviceBound) {
                                    unbindService(serviceConnection)
                                    serviceBound = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Отключиться", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}