package com.beatbox.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatbox.data.repository.AuthRepository
import com.beatbox.data.repository.ApiResult
import com.beatbox.util.SessionManager
import com.beatbox.util.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val themePreference: ThemePreference,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination.LOADING)
    val destination: StateFlow<SplashDestination> = _destination

    init {
        checkStartup()
    }

    private fun checkStartup() {
        viewModelScope.launch {
            // Check onboarding status
            val onboardingComplete = themePreference.isOnboardingComplete.first()

            // Check authentication
            val isLoggedIn = sessionManager.isLoggedIn()

            if (!onboardingComplete) {
                _destination.value = SplashDestination.ONBOARDING
            } else if (!isLoggedIn) {
                _destination.value = SplashDestination.LOGIN
            } else {
                // Verify session is still valid
                when (val result = authRepository.getCurrentUser()) {
                    is ApiResult.Success -> _destination.value = SplashDestination.HOME
                    else -> {
                        sessionManager.clearSession()
                        _destination.value = SplashDestination.LOGIN
                    }
                }
            }
        }
    }
}
