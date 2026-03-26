package com.taskig.android.ui.signin

import android.app.PendingIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskig.android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SignInState {
    data object Idle : SignInState
    data object Loading : SignInState
    data class NeedsConsent(val pendingIntent: PendingIntent) : SignInState
    data object Success : SignInState
    data class Error(val message: String) : SignInState
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SignInState>(SignInState.Idle)
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun setLoading() {
        _state.value = SignInState.Loading
    }

    fun setNeedsConsent(pendingIntent: PendingIntent) {
        _state.value = SignInState.NeedsConsent(pendingIntent)
    }

    fun onAuthSuccess(email: String) {
        viewModelScope.launch {
            repository.saveAccountEmail(email)
            _state.value = SignInState.Success
        }
    }

    fun onAuthError(message: String) {
        _state.value = SignInState.Error(message)
    }

    fun resetError() {
        _state.value = SignInState.Idle
    }
}
