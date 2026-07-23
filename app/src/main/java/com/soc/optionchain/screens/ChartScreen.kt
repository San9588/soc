package com.soc.optionchain.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.soc.optionchain.network.Candle
import com.soc.optionchain.network.ChartRepository
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(chartRepo: ChartRepository) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("soc_prefs", Context.MODE_PRIVATE)

    var selectedScrip by remember { mutableStateOf(999920000L) } // Nifty Default
    var selectedTimeframe by remember { mutableStateOf(prefs.getString("default_tf", "5m") ?: "5m") }
    var candles by remember { mutableStateOf<List<Candle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastDate by remember { mutableStateOf("0") }
    
    val scope = rememberCoroutineScope()

    // Colors
    val bullishColorHex = prefs.getString("bullish_color", "#00FF00") ?: "#00FF00"
    val bearishColorHex = prefs.getString("bearish_color", "#FF0000") ?: "#FF0000"
    val bullishColor = Color(android.graphics.Color.parseColor(bullishColorHex))
    val bearishColor = Color(android.graphics.Color.parseColor(bearishColorHex))

    fun loadData(append: Boolean = false) {
        scope.launch {
            if (!append) {
                isLoading = true
                errorMessage = null
            }
            val (newData, error) = chartRepo.fetchChartData(selectedScrip, selectedTimeframe, if (append) lastDate else "0")
            if (newData != null) {
                if (append) {
                    candles = candles + newData
                } else {
                    candles = newData
                }
                if (newData.isNotEmpty()) {
                    val oldestTimestamp = newData.last().timestamp
                    try {
                        val parts = oldestTimestamp.split(" ")[0].split("-")
                        if (parts.size == 3) {
                            lastDate = "${parts[0]}${parts[1]}${parts[2]}"
                        }
                    } catch (e: Exception) {}
                }
            } else {
                if (!append) errorMessage = error ?: "Unknown API Error"
            }
            if (!append) isLoading = false
        }
    }

    LaunchedEffect(selectedScrip, selectedTimeframe) {
        lastDate = "0"
        loadData(append = false)
    }

    var expandedScrip by remember { mutableStateOf(false) }
    var expandedTf by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedScrip,
                onExpandedChange = { expandedScrip = !expandedScrip }
            ) {
                OutlinedTextField(
                    value = if (selectedScrip == 999920000L) "Nifty" else "BankNifty",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .weight(1f)
                        .padding(end = 4.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedScrip) }
                )
                ExposedDropdownMenu(
                    expanded = expandedScrip,
                    onDismissRequest = { expandedScrip = false }
                ) {
                    DropdownMenuItem(text = { Text("Nifty") }, onClick = { selectedScrip = 999920000L; expandedScrip = false })
                    DropdownMenuItem(text = { Text("BankNifty") }, onClick = { selectedScrip = 999920005L; expandedScrip = false })
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedTf,
                onExpandedChange = { expandedTf = !expandedTf }
            ) {
                OutlinedTextField(
                    value = selectedTimeframe,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .weight(1f)
                        .padding(start = 4.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTf) }
                )
                ExposedDropdownMenu(
                    expanded = expandedTf,
                    onDismissRequest = { expandedTf = false }
                ) {
                    val tfs = listOf("1m", "5m", "15m", "30m", "1h", "4h", "1d")
                    tfs.forEach { tf ->
                        DropdownMenuItem(text = { Text(tf) }, onClick = { selectedTimeframe = tf; expandedTf = false })
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                // Fixed Material 3 Compose bug on Android 14+ by skipping animation/CircularProgressIndicator
                Text("Loading...", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (candles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    text = "No Chart Data Available",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            CandlestickChart(
                candles = candles,
                bullishColor = bullishColor,
                bearishColor = bearishColor,
                onLoadMore = { loadData(append = true) },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    bullishColor: Color,
    bearishColor: Color,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) return

    var offsetX by remember { mutableStateOf(0f) }
    val candleWidth = 20f
    val spacing = 4f

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    // Check if we swiped past the end to load more
                    val totalWidth = candles.size * (candleWidth + spacing)
                    if (offsetX > totalWidth - size.width) {
                        onLoadMore()
                    }
                }
            ) { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
                // Prevent scrolling too far right (into the future)
                if (offsetX < 0f) offsetX = 0f
            }
        }
    ) {
        val visibleCandlesCount = (size.width / (candleWidth + spacing)).toInt() + 2
        val startIndex = max(0, (offsetX / (candleWidth + spacing)).toInt())
        val endIndex = min(candles.size - 1, startIndex + visibleCandlesCount)

        if (startIndex > candles.size - 1) return@Canvas

        val visibleCandles = candles.subList(startIndex, endIndex + 1)
        
        val maxPrice = visibleCandles.maxOfOrNull { it.high }?.toFloat() ?: 1f
        val minPrice = visibleCandles.minOfOrNull { it.low }?.toFloat() ?: 0f
        val priceRange = max(maxPrice - minPrice, 1f)

        fun getY(price: Double): Float {
            return size.height - ((price.toFloat() - minPrice) / priceRange) * size.height
        }

        visibleCandles.forEachIndexed { i, candle ->
            val indexInList = startIndex + i
            // X goes from right to left: Newest candle (index 0) is at the right edge
            val xCenter = size.width - ((indexInList * (candleWidth + spacing)) - offsetX) - (candleWidth / 2)

            val yOpen = getY(candle.open)
            val yClose = getY(candle.close)
            val yHigh = getY(candle.high)
            val yLow = getY(candle.low)

            val color = if (candle.close >= candle.open) bullishColor else bearishColor

            // Draw wick
            drawLine(
                color = color,
                start = Offset(xCenter, yHigh),
                end = Offset(xCenter, yLow),
                strokeWidth = 2f
            )

            // Draw body
            val top = min(yOpen, yClose)
            val bottom = max(yOpen, yClose)
            
            drawRect(
                color = color,
                topLeft = Offset(xCenter - (candleWidth / 2), top),
                size = Size(candleWidth, max(bottom - top, 2f)) // Ensure min height of 2px
            )
        }
    }
}
