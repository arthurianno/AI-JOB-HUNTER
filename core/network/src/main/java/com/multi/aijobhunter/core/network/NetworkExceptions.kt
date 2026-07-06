package com.multi.aijobhunter.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class NetworkException(override val message: String, val code: Int) : IOException(message)
class RateLimitException : IOException("LLM API limit exceeded. Please check or change your API token.")

class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = try {
            chain.proceed(chain.request())
        } catch (e: Exception) {
            throw NetworkException(e.message ?: "Unknown network connection error", 0)
        }

        if (response.code == 429) {
            throw RateLimitException()
        }

        if (!response.isSuccessful) {
            throw NetworkException("HTTP error code: ${response.code}", response.code)
        }

        return response
    }
}
