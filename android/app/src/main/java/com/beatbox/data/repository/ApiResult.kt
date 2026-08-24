package com.beatbox.data.repository

/**
 * Sealed class representing the result of an API operation.
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val code: String, val message: String) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    fun isLoading(): Boolean = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): Error? = this as? Error
}

inline fun <T> safeApiCall(apiCall: () -> retrofit2.Response<com.beatbox.data.dto.ApiResponse<T>>): ApiResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success && body.data != null) {
                ApiResult.Success(body.data)
            } else if (body != null && body.error != null) {
                ApiResult.Error(body.error.code, body.error.message)
            } else {
                ApiResult.Error("UNKNOWN", "An unknown error occurred.")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val message = parseErrorMessage(errorBody) ?: "Request failed with code ${response.code()}"
            ApiResult.Error("HTTP_${response.code()}", message)
        }
    } catch (e: java.net.SocketTimeoutException) {
        ApiResult.Error("TIMEOUT", "Request timed out. Please check your connection and try again.")
    } catch (e: java.net.UnknownHostException) {
        ApiResult.Error("NO_CONNECTION", "No internet connection. Please check your network.")
    } catch (e: Exception) {
        ApiResult.Error("NETWORK_ERROR", e.message ?: "A network error occurred.")
    }
}

inline fun <T> safeApiCallList(apiCall: () -> retrofit2.Response<com.beatbox.data.dto.ApiResponse<List<T>>>): ApiResult<List<T>> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success) {
                ApiResult.Success(body.data ?: emptyList())
            } else if (body != null && body.error != null) {
                ApiResult.Error(body.error.code, body.error.message)
            } else {
                ApiResult.Error("UNKNOWN", "An unknown error occurred.")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val message = parseErrorMessage(errorBody) ?: "Request failed with code ${response.code()}"
            ApiResult.Error("HTTP_${response.code()}", message)
        }
    } catch (e: java.net.SocketTimeoutException) {
        ApiResult.Error("TIMEOUT", "Request timed out. Please check your connection and try again.")
    } catch (e: java.net.UnknownHostException) {
        ApiResult.Error("NO_CONNECTION", "No internet connection. Please check your network.")
    } catch (e: Exception) {
        ApiResult.Error("NETWORK_ERROR", e.message ?: "A network error occurred.")
    }
}

fun parseErrorMessage(errorBody: String?): String? {
    if (errorBody.isNullOrEmpty()) return null
    return try {
        val gson = com.google.gson.Gson()
        val response = gson.fromJson(errorBody, com.beatbox.data.dto.ApiResponse::class.java)
        response?.error?.message
    } catch (e: Exception) {
        null
    }
}
