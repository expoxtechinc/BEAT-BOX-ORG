package com.beatbox.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatbox.util.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val themePreference: ThemePreference,
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            themePreference.setOnboardingComplete(true)
        }
    }
}
