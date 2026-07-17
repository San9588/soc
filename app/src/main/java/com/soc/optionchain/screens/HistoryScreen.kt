package com.soc.optionchain.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soc.optionchain.network.HistoryApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(apiClient: HistoryApiClient) {
    var historyData by remember { mutableStateOf("No data fetched yet.") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History Option Chain (API)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    isLoading = true
                    // Example dummy JSON request
                    val requestJson = """{"symbol": "NIFTY", "date": "2023-10-01"}"""
                    val result = apiClient.fetchHistory(requestJson)
                    historyData = result ?: "Failed to fetch data"
                    isLoading = false
                }
            }) {
                Text("Fetch")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(
                    text = historyData,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
