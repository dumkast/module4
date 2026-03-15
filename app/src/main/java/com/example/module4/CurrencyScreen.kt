package com.example.module4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CurrencyScreen(viewModel: CurrencyViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Курс USD/RUB",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "${state.rate} ₽",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = when (state.trend) {
                Trend.UP -> MaterialTheme.colorScheme.primary
                Trend.DOWN -> MaterialTheme.colorScheme.error
                Trend.SAME -> MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = when (state.trend) {
                Trend.UP -> "▲"
                Trend.DOWN -> "▼"
                Trend.SAME -> "•"
            },
            fontSize = 32.sp,
            color = when (state.trend) {
                Trend.UP -> MaterialTheme.colorScheme.primary
                Trend.DOWN -> MaterialTheme.colorScheme.error
                Trend.SAME -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (state.lastUpdateTime.isNotBlank()) {
            Text(
                text = "Последнее обновление: ${state.lastUpdateTime}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = viewModel::onRefreshClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Обновить сейчас")
        }
    }
}