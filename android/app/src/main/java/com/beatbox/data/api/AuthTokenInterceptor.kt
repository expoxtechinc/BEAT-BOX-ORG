package com.beatbox.data.api

import com.beatbox.util.Constants
import com.beatbox.util.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds the Bearer token to authenticated API requests.
 */
@Singleton
class AuthTokenInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.getAccessToken()
        val request = chain.request().newBuilder()

        if (token != null) {
            request.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(request.build())
    }
}
