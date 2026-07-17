package com.soc.optionchain.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soc.optionchain.network.WebsocketClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(wsClient: WebsocketClient) {
    val messages = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    
    DisposableEffect(Unit) {
        wsClient.connect()
        val job = scope.launch {
            wsClient.messages.collect { msg ->
                messages.add(0, msg)
                if (messages.size > 50) {
                    messages.removeLast() // Keep last 50 messages
                }
            }
        }
        onDispose {
            job.cancel()
            wsClient.disconnect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Option Chain (WebSocket)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
