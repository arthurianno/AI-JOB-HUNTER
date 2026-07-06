package com.multi.aijobhunter.core.network

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<ChatMessage>,
    val response_format: ResponseFormat? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String
)
