package com.beatbox.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Securely stores authentication tokens using EncryptedSharedPreferences.
 * Tokens are never stored in plain text.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        Constants.ENCRYPTED_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _isLoggedIn = MutableStateFlow(prefs.contains(Constants.KEY_ACCESS_TOKEN))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _accessToken = MutableStateFlow(prefs.getString(Constants.KEY_ACCESS_TOKEN, null))
    val accessToken: StateFlow<String?> = _accessToken

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(Constants.KEY_ACCESS_TOKEN, accessToken)
            .putString(Constants.KEY_REFRESH_TOKEN, refreshToken)
            .apply()
        _accessToken.value = accessToken
        _isLoggedIn.value = true
    }

    fun getAccessToken(): String? = prefs.getString(Constants.KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(Constants.KEY_REFRESH_TOKEN, null)

    fun saveUserId(userId: String) {
        prefs.edit().putString(Constants.KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? = prefs.getString(Constants.KEY_USER_ID, null)

    fun clearSession() {
        prefs.edit()
            .remove(Constants.KEY_ACCESS_TOKEN)
            .remove(Constants.KEY_REFRESH_TOKEN)
            .remove(Constants.KEY_USER_ID)
            .apply()
        _accessToken.value = null
        _isLoggedIn.value = false
    }

    fun isLoggedIn(): Boolean = prefs.contains(Constants.KEY_ACCESS_TOKEN)
}
