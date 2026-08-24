package com.beatbox.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = Constants.PREFS_NAME)

@Singleton
class ThemePreference @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class ThemeMode(val value: Int) {
        LIGHT(0),
        DARK(1),
        SYSTEM(2);

        companion object {
            fun fromValue(value: Int): ThemeMode = entries.find { it.value == value } ?: SYSTEM
        }
    }

    private val themeKey = intPreferencesKey(Constants.KEY_THEME_MODE)
    private val onboardingKey = intPreferencesKey(Constants.KEY_ONBOARDING_COMPLETE)

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { prefs -> ThemeMode.fromValue(prefs[themeKey] ?: 2) }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.value }
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data
        .map { prefs -> (prefs[onboardingKey] ?: 0) == 1 }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[onboardingKey] = if (complete) 1 else 0 }
    }
}
