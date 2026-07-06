package com.multi.aijobhunter.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<State, Intent, Effect>(initialState: State) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = Channel<Effect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    protected val currentState: State get() = _uiState.value

    abstract fun handleIntent(intent: Intent)

    protected fun updateState(reducer: State.() -> State) {
        _uiState.update { it.reducer() }
    }

    protected fun sendSideEffect(effect: Effect) {
        viewModelScope.launch { _sideEffect.send(effect) }
    }
}
