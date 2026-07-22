package com.soc.optionchain.network

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.soc.optionchain.BuildConfig

data class TokenResponse(
    val access_token: String?,
    val expires_in: Long?,
    val expires_on: Long?,
    val token_type: String?
)

class TokenManager(context: Context, private val client: OkHttpClient) {
    private val prefs: SharedPreferences = context.getSharedPreferences("soc_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun getValidToken(): String? {
        val token = prefs.getString("access_token", null)
        val expiresOn = prefs.getLong("expires_on", 0)
        
        // Check if token is valid (adding 60s buffer)
        val currentTime = System.currentTimeMillis() / 1000
        if (token != null && expiresOn > currentTime + 60) {
            return token
        }

        // Token expired or missing, fetch new one
        return fetchNewToken()
    }

    private suspend fun fetchNewToken(): String? = withContext(Dispatchers.IO) {
        if (BuildConfig.FIVE_URL.isEmpty()) {
            return@withContext null
        }
        
        val request = Request.Builder()
            .url(BuildConfig.FIVE_URL)
            .post("".toRequestBody())
            .addHeader("authority", BuildConfig.FIVE_AUTH)
            .addHeader("accept", "*/*")
            .addHeader("accept-language", "en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7")
            .addHeader("ocp-apim-subscription-key", "d396f0ca354f4a08b4c226343ae3ed1b")
            .addHeader("origin", BuildConfig.FIVE_ORIGIN)
            .addHeader("referer", BuildConfig.FIVE_REFER)
            .addHeader("sec-ch-ua", "\"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"")
            .addHeader("sec-ch-ua-mobile", "?1")
            .addHeader("sec-ch-ua-platform", "\"Android\"")
            .addHeader("sec-fetch-dest", "empty")
            .addHeader("sec-fetch-mode", "cors")
            .addHeader("sec-fetch-site", "same-site")
            .addHeader("user-agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    android.util.Log.d("TokenManager", "Response: $bodyStr")
                    try {
                        val tokenResp = gson.fromJson(bodyStr, TokenResponse::class.java)
                        if (tokenResp?.access_token != null) {
                            prefs.edit()
                                .putString("access_token", tokenResp.access_token)
                                .putLong("expires_on", tokenResp.expires_on ?: 0)
                                .apply()
                            tokenResp.access_token
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TokenManager", "JSON Parse Error", e)
                        null
                    }
                } else {
                    android.util.Log.e("TokenManager", "HTTP Error: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
