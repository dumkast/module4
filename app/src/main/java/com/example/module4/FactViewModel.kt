package com.example.module4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FactViewModel(
    private val repository: AnimalFactsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FactState())
    val state = _state.asStateFlow()

    fun onGenerateClick() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getRandomFact().collect { fact ->
                _state.update { it.copy(fact = fact, isLoading = false) }
            }
        }
    }
}

data class FactState(
    val fact: String = "",
    val isLoading: Boolean = false
)