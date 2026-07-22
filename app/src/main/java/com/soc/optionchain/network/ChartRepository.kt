package com.soc.optionchain.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class ChartDataResponse(
    val Status: Int,
    val Message: String?,
    val data: ChartData?,
    val ScripCode: Long
)

data class ChartData(
    val candles: List<List<Any>>
)

class ChartRepository(private val client: OkHttpClient, private val tokenManager: TokenManager) {
    private val gson = Gson()

    suspend fun fetchChartData(scripCode: Long, timeFrame: String, lastDate: String = "0"): List<Candle>? = withContext(Dispatchers.IO) {
        val token = tokenManager.getValidToken() ?: return@withContext null
        
        val url = "https://chartstt.5paisa.com/chart/historicalintradayV1/N/C/$scripCode/$timeFrame/$lastDate"
        
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("authority", "chartstt.5paisa.com")
            .addHeader("accept", "*/*")
            .addHeader("accept-language", "en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7")
            .addHeader("authorization", "Bearer $token")
            .addHeader("content-type", "application/json")
            .addHeader("origin", "https://tradechart.5paisa.com")
            .addHeader("referer", "https://tradechart.5paisa.com/")
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
                    val bodyStr = response.body?.string()
                    val chartResp = gson.fromJson(bodyStr, ChartDataResponse::class.java)
                    
                    chartResp.data?.candles?.mapNotNull { list ->
                        try {
                            Candle(
                                timestamp = list[0].toString(),
                                open = (list[1] as Number).toDouble(),
                                high = (list[2] as Number).toDouble(),
                                low = (list[3] as Number).toDouble(),
                                close = (list[4] as Number).toDouble(),
                                volume = (list[5] as Number).toLong()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class Candle(
    val timestamp: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)
