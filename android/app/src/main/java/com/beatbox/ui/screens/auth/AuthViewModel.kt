package com.beatbox.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatbox.data.dto.UserDto
import com.beatbox.data.repository.ApiResult
import com.beatbox.data.repository.AuthRepository
import com.beatbox.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val user: UserDto? = null,
    val resetToken: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _loginState = MutableStateFlow(AuthUiState())
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(AuthUiState())
    val registerState: StateFlow<AuthUiState> = _registerState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow(AuthUiState())
    val forgotPasswordState: StateFlow<AuthUiState> = _forgotPasswordState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow(AuthUiState())
    val resetPasswordState: StateFlow<AuthUiState> = _resetPasswordState.asStateFlow()

    fun login(email: String, password: String) {
        _loginState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = authRepository.login(email, password)) {
                is ApiResult.Success -> {
                    sessionManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                    sessionManager.saveUserId(result.data.user.id)
                    _loginState.value = AuthUiState(isSuccess = true, user = result.data.user)
                }
                is ApiResult.Error -> {
                    _loginState.value = AuthUiState(error = result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun register(email: String, password: String, username: String, displayName: String?) {
        _registerState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = authRepository.register(email, password, username, displayName)) {
                is ApiResult.Success -> {
                    sessionManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                    sessionManager.saveUserId(result.data.user.id)
                    _registerState.value = AuthUiState(isSuccess = true, user = result.data.user)
                }
                is ApiResult.Error -> {
                    _registerState.value = AuthUiState(error = result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun forgotPassword(email: String) {
        _forgotPasswordState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = authRepository.forgotPassword(email)) {
                is ApiResult.Success -> {
                    // In dev mode, the backend returns a resetToken for testing
                    val token = (result.data as? Map<*, *>)?.get("resetToken") as? String
                    _forgotPasswordState.value = AuthUiState(isSuccess = true, resetToken = token)
                }
                is ApiResult.Error -> {
                    _forgotPasswordState.value = AuthUiState(error = result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun resetPassword(token: String, password: String) {
        _resetPasswordState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = authRepository.resetPassword(token, password)) {
                is ApiResult.Success -> {
                    _resetPasswordState.value = AuthUiState(isSuccess = true)
                }
                is ApiResult.Error -> {
                    _resetPasswordState.value = AuthUiState(error = result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun clearLoginError() { _loginState.value = _loginState.value.copy(error = null) }
    fun clearRegisterError() { _registerState.value = _registerState.value.copy(error = null) }
    fun clearForgotPasswordError() { _forgotPasswordState.value = _forgotPasswordState.value.copy(error = null) }
    fun clearResetPasswordError() { _resetPasswordState.value = _resetPasswordState.value.copy(error = null) }
}
