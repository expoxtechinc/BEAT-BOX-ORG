package com.beatbox.data.repository

import com.beatbox.data.api.BeatBoxApi
import com.beatbox.data.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: BeatBoxApi,
) {
    suspend fun register(email: String, password: String, username: String, displayName: String?): ApiResult<AuthResponse> {
        return safeApiCall { api.register(RegisterRequest(email, password, username, displayName)) }
    }

    suspend fun login(email: String, password: String): ApiResult<AuthResponse> {
        return safeApiCall { api.login(LoginRequest(email, password)) }
    }

    suspend fun refresh(refreshToken: String): ApiResult<TokenResponse> {
        return safeApiCall { api.refresh(RefreshRequest(refreshToken)) }
    }

    suspend fun logout(refreshToken: String): ApiResult<Any> {
        return safeApiCall { api.logout(RefreshRequest(refreshToken)) }
    }

    suspend fun logoutAll(): ApiResult<Any> {
        return safeApiCall { api.logoutAll() }
    }

    suspend fun getCurrentUser(): ApiResult<UserDto> {
        return safeApiCall { api.getCurrentUser() }
    }

    suspend fun verifyEmail(token: String): ApiResult<Any> {
        return safeApiCall { api.verifyEmail(VerifyEmailRequest(token)) }
    }

    suspend fun forgotPassword(email: String): ApiResult<Any> {
        return safeApiCall { api.forgotPassword(ForgotPasswordRequest(email)) }
    }

    suspend fun resetPassword(token: String, password: String): ApiResult<Any> {
        return safeApiCall { api.resetPassword(ResetPasswordRequest(token, password)) }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Any> {
        return safeApiCall { api.changePassword(ChangePasswordRequest(currentPassword, newPassword)) }
    }

    suspend fun deleteAccount(password: String): ApiResult<Any> {
        return safeApiCall { api.deleteAccount(DeleteAccountRequest(password)) }
    }
}
