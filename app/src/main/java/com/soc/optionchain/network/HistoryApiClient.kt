package com.soc.optionchain.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class HistoryApiClient {
    private val client = OkHttpClient()
    
    // Change this to your local network API URL, e.g., http://192.168.1.100:8080/history
    private val API_URL = "http://10.0.2.2:8080/history"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchHistory(requestJson: String): String? {
        return withContext(Dispatchers.IO) {
            val body = requestJson.toRequestBody(JSON)
            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext "Error: ${response.code}"
                    response.body?.string()
                }
            } catch (e: IOException) {
                "Error: ${e.message}"
            }
        }
    }
}
