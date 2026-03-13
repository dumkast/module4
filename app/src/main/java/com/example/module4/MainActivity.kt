package com.example.module4

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.module4.ui.theme.Module4Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Module4Theme {
                var input by remember { mutableStateOf(TextFieldValue("")) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            label = { Text("Введите время в секундах") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.tertiary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            modifier = Modifier.width(320.dp)
                        )
                        Button(
                            onClick = {
                                val seconds = input.text.toIntOrNull() ?: 0
                                if (seconds > 0) {
                                    val intent = Intent(this@MainActivity, OneShotTimerService::class.java)
                                    intent.putExtra("seconds", seconds)
                                    startService(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Text("Запустить таймер")
                        }
                    }
                }
            }
        }
    }
}