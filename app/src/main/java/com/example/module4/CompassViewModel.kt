package com.example.module4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CompassViewModel(
    private val repository: CompassRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CompassState())
    val state: StateFlow<CompassState> = _state.asStateFlow()
    private var azimuthJob: kotlinx.coroutines.Job? = null

    fun onResume() {
        if (!repository.hasRequiredSensors()) {
            _state.update {
                it.copy(
                    error = "Устройство не поддерживает датчик ориентации",
                    isAvailable = false
                )
            }
            return
        }
        _state.update { it.copy(isAvailable = true) }

        azimuthJob = viewModelScope.launch {
            repository.getAzimuthFlow().collect { azimuth ->
                _state.update { currentState ->
                    val smoothedAzimuth = smoothRotation(currentState.azimuth, azimuth)
                    currentState.copy(
                        azimuth = smoothedAzimuth,
                        rawAzimuth = azimuth
                    )
                }
            }
        }
    }

    fun onPause() {
        azimuthJob?.cancel()
        azimuthJob = null
    }

    private fun smoothRotation(current: Int, target: Int, factor: Float = 0.3f): Int {
        var diff = target - current
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360
        val newValue = current + (diff * factor).toInt()
        return (newValue + 360) % 360
    }
}

data class CompassState(
    val azimuth: Int = 0,
    val rawAzimuth: Int = 0,
    val error: String = "",
    val isAvailable: Boolean = true
)