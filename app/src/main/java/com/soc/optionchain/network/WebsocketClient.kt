package com.soc.optionchain.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response

class WebsocketClient {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val messages: SharedFlow<String> = _messages
    
    // Change this to your local network websocket URL, e.g., ws://192.168.1.100:8080/live
    private val WS_URL = "ws://10.0.2.2:8080/live" 

    fun connect() {
        val request = Request.Builder().url(WS_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                _messages.tryEmit("Connected to Live WebSocket")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                _messages.tryEmit(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                webSocket.close(1000, null)
                _messages.tryEmit("Disconnected")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                _messages.tryEmit("Error: ${t.message}")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
    }
}
