package com.example.module4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CurrencyViewModel(
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CurrencyState())
    val state: StateFlow<CurrencyState> = _state.asStateFlow()
    private var previousRate = 90.0

    init {
        startAutoUpdate()
    }

    private fun startAutoUpdate() {
        viewModelScope.launch {
            while (true) {
                updateRate()
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    fun onRefreshClick() {
        viewModelScope.launch {
            updateRate()
        }
    }

    private suspend fun updateRate() {
        repository.getCurrentRate().collect { (newRate, time) ->
            val trend = when {
                newRate > previousRate -> Trend.UP
                newRate < previousRate -> Trend.DOWN
                else -> Trend.SAME
            }
            val formatter = DecimalFormat(
                "#0.00",
                DecimalFormatSymbols(Locale.US)
            )
            val formattedRate = formatter.format(newRate).replace(",", ".")
            _state.update {
                CurrencyState(
                    rate = formattedRate,
                    trend = trend,
                    lastUpdateTime = time,
                    previousRate = previousRate
                )
            }
            previousRate = newRate
        }
    }
}

enum class Trend {
    UP, DOWN, SAME
}

data class CurrencyState(
    val rate: String = "90.00",
    val trend: Trend = Trend.SAME,
    val lastUpdateTime: String = "",
    val previousRate: Double = 90.0
)