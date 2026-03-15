package com.example.module4

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class CurrencyRepository {
    private var lastRate = 90.0

    fun getCurrentRate(): Flow<Pair<Double, String>> = flow {
        val change = Random.nextDouble(-2.0, 2.0)
        lastRate = (lastRate + change).coerceIn(85.0, 95.0)
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        delay(500)
        emit(lastRate to time)
    }
}