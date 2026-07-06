package com.multi.aijobhunter.core.network

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatResponse(
    val id: String? = null,
    val choices: List<ChatChoice>
)

@Serializable
data class ChatChoice(
    val index: Int? = null,
    val message: ChatMessage,
    val finish_reason: String? = null
)
