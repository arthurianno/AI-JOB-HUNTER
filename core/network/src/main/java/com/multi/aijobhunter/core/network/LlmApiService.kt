package com.multi.aijobhunter.core.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface LlmApiService {
    @POST
    @Headers("Content-Type: application/json")
    suspend fun getChatCompletion(
        @Url url: String,
        @Header("Authorization") apiKey: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}
