package com.example.module4

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlin.random.Random
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RandomNumberService : Service() {
    private val binder = LocalBinder()
    private var job: Job? = null
    private val _randomValue = MutableStateFlow(0)
    val randomValueFlow = _randomValue.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): RandomNumberService = this@RandomNumberService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d("RandomNumberService", "Service bound")
        startGenerating()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopGenerating()
        Log.d("RandomNumberService", "Service unbound")
        return super.onUnbind(intent)
    }

    private fun startGenerating() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                _randomValue.value = Random.nextInt(0, 101)
                delay(1000)
            }
        }
    }

    private fun stopGenerating() {
        job?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGenerating()
    }
}