package com.beatbox.data.api

import com.beatbox.util.SessionManager
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles token refresh when a 401 response is received.
 * Attempts to refresh the access token using the refresh token
 * and retries the failed request.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiClientBuilder: ApiClientBuilder,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops
        if (response.code != 401) return null
        if (responseCount(response) >= 2) return null

        val refreshToken = sessionManager.getRefreshToken() ?: return null

        // Synchronous refresh call
        val refreshRequest = Request.Builder()
            .url(apiClientBuilder.baseUrl + "auth/refresh")
            .post(okhttp3.RequestBody.create(
                okhttp3.MediaType.get("application/json"),
                """{"refreshToken":"$refreshToken"}"""
            ))
            .build()

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val refreshResponse = client.newCall(refreshRequest).execute()
            if (!refreshResponse.isSuccessful) {
                sessionManager.clearSession()
                return null
            }

            val body = refreshResponse.body?.string() ?: return null
            val gson = com.google.gson.Gson()
            val tokenResponse = gson.fromJson(body, TokenRefreshResponse::class.java)

            val newAccessToken = tokenResponse.data?.accessToken ?: return null
            val newRefreshToken = tokenResponse.data?.refreshToken ?: return null

            sessionManager.saveTokens(newAccessToken, newRefreshToken)

            // Retry the original request with the new token
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        } catch (e: Exception) {
            sessionManager.clearSession()
            return null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prev = response.priorResponse
        while (prev != null) {
            count++
            prev = prev.priorResponse
        }
        return count
    }
}

data class TokenRefreshResponse(
    val success: Boolean,
    val data: TokenRefreshData?,
)

data class TokenRefreshData(
    val accessToken: String,
    val refreshToken: String,
)

/**
 * Provides the base URL for API calls.
 */
@Singleton
class ApiClientBuilder @Inject constructor() {
    val baseUrl: String = com.beatbox.BuildConfig.API_BASE_URL
    val uploadBaseUrl: String = com.beatbox.BuildConfig.UPLOAD_BASE_URL
}
