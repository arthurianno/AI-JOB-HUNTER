package com.multi.aijobhunter.core.common

interface JobSearchCredentialsProvider {
    fun getHhAccessToken(): String
    fun getHhContactEmail(): String
}
