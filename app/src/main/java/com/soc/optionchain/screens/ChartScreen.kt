package com.soc.optionchain.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
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

    var selectedScrip by remember { mutableStateOf(999920000L) }
    var selectedTimeframe by remember { mutableStateOf(prefs.getString("default_tf", "5m") ?: "5m") }
    var candles by remember { mutableStateOf<List<Candle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastDate by remember { mutableStateOf("0") }

    var showBottomSheet by remember { mutableStateOf(false) }
    var showVGrid by remember { mutableStateOf(true) }
    var showHGrid by remember { mutableStateOf(true) }
    var gridOpacity by remember { mutableStateOf(0.3f) }
    var bullishColorHex by remember { mutableStateOf(prefs.getString("bullish_color", "#00FF00") ?: "#00FF00") }
    var bearishColorHex by remember { mutableStateOf(prefs.getString("bearish_color", "#FF0000") ?: "#FF0000") }

    val scope = rememberCoroutineScope()

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

    Scaffold(
        topBar = {
            ChartTopBar(
                selectedScrip = selectedScrip,
                onScripChange = { selectedScrip = it },
                selectedTimeframe = selectedTimeframe,
                onTimeframeChange = { selectedTimeframe = it }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chart Settings")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF131722)) 
        ) {
            if (isLoading) {
                Text("Loading...", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Center), color = Color.White)
            } else if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else if (candles.isNotEmpty()) {
                InteractiveCandlestickChart(
                    candles = candles,
                    bullishColorHex = bullishColorHex,
                    bearishColorHex = bearishColorHex,
                    showVGrid = showVGrid,
                    showHGrid = showHGrid,
                    gridOpacity = gridOpacity,
                    onLoadMore = { loadData(append = true) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("No data available", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            SettingsPanel(
                showVGrid = showVGrid, onShowVGridChange = { showVGrid = it },
                showHGrid = showHGrid, onShowHGridChange = { showHGrid = it },
                gridOpacity = gridOpacity, onGridOpacityChange = { gridOpacity = it },
                bullishColorHex = bullishColorHex, onBullishChange = {
                    bullishColorHex = it
                    prefs.edit().putString("bullish_color", it).apply()
                },
                bearishColorHex = bearishColorHex, onBearishChange = {
                    bearishColorHex = it
                    prefs.edit().putString("bearish_color", it).apply()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartTopBar(
    selectedScrip: Long, onScripChange: (Long) -> Unit,
    selectedTimeframe: String, onTimeframeChange: (String) -> Unit
) {
    var expandedScrip by remember { mutableStateOf(false) }
    var expandedTf by remember { mutableStateOf(false) }
    val timeframes = listOf("3m", "5m", "15m", "30m", "1h", "2h", "4h", "1d")

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Text(
                    text = if (selectedScrip == 999920000L) "NIFTY" else "BANKNIFTY",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { expandedScrip = true }
                        .padding(8.dp)
                )
                DropdownMenu(expanded = expandedScrip, onDismissRequest = { expandedScrip = false }) {
                    DropdownMenuItem(text = { Text("NIFTY") }, onClick = { onScripChange(999920000L); expandedScrip = false })
                    DropdownMenuItem(text = { Text("BANKNIFTY") }, onClick = { onScripChange(999920005L); expandedScrip = false })
                }
            }

            Box {
                Text(
                    text = selectedTimeframe,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { expandedTf = true }
                        .padding(8.dp)
                )
                DropdownMenu(expanded = expandedTf, onDismissRequest = { expandedTf = false }) {
                    timeframes.forEach { tf ->
                        DropdownMenuItem(text = { Text(tf) }, onClick = { onTimeframeChange(tf); expandedTf = false })
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveCandlestickChart(
    candles: List<Candle>,
    bullishColorHex: String,
    bearishColorHex: String,
    showVGrid: Boolean,
    showHGrid: Boolean,
    gridOpacity: Float,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var scaleX by remember { mutableStateOf(1f) } 
    var scaleY by remember { mutableStateOf(1f) } 
    
    var crosshairPos by remember { mutableStateOf<Offset?>(null) }
    
    val baseCandleWidth = 14f
    val spacing = 2f
    val rightAxisWidth = 140f

    val bullishColor = try { Color(android.graphics.Color.parseColor(bullishColorHex)) } catch(e: Exception) { Color.Green }
    val bearishColor = try { Color(android.graphics.Color.parseColor(bearishColorHex)) } catch(e: Exception) { Color.Red }
    val gridColor = Color.LightGray.copy(alpha = gridOpacity)
    
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#787B86")
            textSize = 26f
            isAntiAlias = true
        }
    }
    
    val dateTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#787B86")
            textSize = 24f
            isAntiAlias = true
        }
    }
    
    val tagTextPaint = remember {
        android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 26f; isAntiAlias = true }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (centroid.x > size.width - rightAxisWidth) {
                        scaleY = max(0.1f, min(scaleY * (1f - pan.y * 0.005f), 10f))
                    } else {
                        if (zoom != 1f) {
                            scaleX = max(0.1f, min(scaleX * zoom, 10f))
                        }
                        offsetX += pan.x
                        if (offsetX < 0f) offsetX = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { crosshairPos = it },
                    onDrag = { change, _ -> crosshairPos = change.position },
                    onDragEnd = { crosshairPos = null },
                    onDragCancel = { crosshairPos = null }
                )
            }
    ) {
        val currentCandleWidth = baseCandleWidth * scaleX
        val currentSpacing = spacing * scaleX
        val itemWidth = currentCandleWidth + currentSpacing
        
        val drawingAreaWidth = size.width - rightAxisWidth
        
        val visibleCandlesCount = (drawingAreaWidth / itemWidth).toInt() + 2
        val startIndex = max(0, (offsetX / itemWidth).toInt())
        val endIndex = min(candles.size - 1, startIndex + visibleCandlesCount)

        if (startIndex > candles.size - 1) return@Canvas
        
        if (endIndex >= candles.size - 5) {
            onLoadMore()
        }

        val visibleCandles = candles.subList(startIndex, endIndex + 1)
        
        val rawMaxPrice = visibleCandles.maxOfOrNull { it.high }?.toFloat() ?: 1f
        val rawMinPrice = visibleCandles.minOfOrNull { it.low }?.toFloat() ?: 0f
        val rawRange = max(rawMaxPrice - rawMinPrice, 1f)
        
        val midPrice = rawMinPrice + (rawRange / 2f)
        val zoomedRange = rawRange / scaleY
        val maxPrice = midPrice + (zoomedRange / 2f)
        val minPrice = midPrice - (zoomedRange / 2f)
        val priceRange = maxPrice - minPrice

        fun getY(price: Float): Float {
            return size.height - ((price - minPrice) / priceRange) * size.height
        }
        
        fun getPriceFromY(y: Float): Float {
            return minPrice + ((size.height - y) / size.height) * priceRange
        }

        if (showHGrid) {
            // Simplified grid line rendering to avoid Kotlin Double/Float operator ambiguities completely
            val linesCount = 8
            val priceStep = priceRange / linesCount
            for (i in 0..linesCount) {
                val currentGridPrice = minPrice + (i * priceStep)
                val y = getY(currentGridPrice)
                drawLine(gridColor, Offset(0f, y), Offset(drawingAreaWidth, y), strokeWidth = 1f)
                
                val label = String.format("%.1f", currentGridPrice)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    drawingAreaWidth + 10f,
                    y - ((textPaint.descent() + textPaint.ascent()) / 2),
                    textPaint
                )
            }
        }
        
        drawLine(Color.Gray, Offset(drawingAreaWidth, 0f), Offset(drawingAreaWidth, size.height), strokeWidth = 2f)

        visibleCandles.forEachIndexed { i, candle ->
            val indexInList = startIndex + i
            val xCenter = drawingAreaWidth - ((indexInList * itemWidth) - offsetX) - (currentCandleWidth / 2)

            if (showVGrid && i % 5 == 0) {
                drawLine(gridColor, Offset(xCenter, 0f), Offset(xCenter, size.height), strokeWidth = 1f)
            }

            if (i % 10 == 0) {
                try {
                    val dtParts = candle.timestamp.split(" ")
                    if (dtParts.size >= 2) {
                        val dateParts = dtParts[0].split("-")
                        val timeParts = dtParts[1].split(":")
                        if (dateParts.size == 3 && timeParts.size >= 2) {
                            val displayStr = "${dateParts[2]}/${dateParts[1]} ${timeParts[0]}:${timeParts[1]}"
                            drawContext.canvas.nativeCanvas.drawText(
                                displayStr,
                                xCenter - 30f,
                                size.height - 5f,
                                dateTextPaint
                            )
                        }
                    }
                } catch(e: Exception) {}
            }

            val yOpen = getY(candle.open.toFloat())
            val yClose = getY(candle.close.toFloat())
            val yHigh = getY(candle.high.toFloat())
            val yLow = getY(candle.low.toFloat())

            val color = if (candle.close >= candle.open) bullishColor else bearishColor

            drawLine(color, Offset(xCenter, yHigh), Offset(xCenter, yLow), strokeWidth = 2f * scaleX)
            val top = min(yOpen, yClose)
            val bottom = max(yOpen, yClose)
            drawRect(color, topLeft = Offset(xCenter - (currentCandleWidth / 2), top), size = Size(currentCandleWidth, max(bottom - top, 2f)))
        }

        crosshairPos?.let { pos ->
            val chX = min(pos.x, drawingAreaWidth)
            val chY = max(0f, min(pos.y, size.height))
            
            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            
            drawLine(Color.White, Offset(chX, 0f), Offset(chX, size.height), strokeWidth = 2f, pathEffect = dashedEffect)
            drawLine(Color.White, Offset(0f, chY), Offset(size.width, chY), strokeWidth = 2f, pathEffect = dashedEffect)
            
            val priceAtCrosshair = getPriceFromY(chY)
            val tagText = String.format("%.2f", priceAtCrosshair)
            
            val tagHeight = 44f
            drawRect(
                color = Color(0xFF2A2E39),
                topLeft = Offset(drawingAreaWidth, chY - tagHeight / 2),
                size = Size(rightAxisWidth, tagHeight)
            )
            drawContext.canvas.nativeCanvas.drawText(
                tagText,
                drawingAreaWidth + 10f,
                chY - ((tagTextPaint.descent() + tagTextPaint.ascent()) / 2),
                tagTextPaint
            )
        }
    }
}

@Composable
fun SettingsPanel(
    showVGrid: Boolean, onShowVGridChange: (Boolean) -> Unit,
    showHGrid: Boolean, onShowHGridChange: (Boolean) -> Unit,
    gridOpacity: Float, onGridOpacityChange: (Float) -> Unit,
    bullishColorHex: String, onBullishChange: (String) -> Unit,
    bearishColorHex: String, onBearishChange: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        item {
            Text("Candle Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = bullishColorHex, onValueChange = onBullishChange,
                label = { Text("Bullish Color (Hex)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = bearishColorHex, onValueChange = onBearishChange,
                label = { Text("Bearish Color (Hex)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("Axis / Grid Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Show Vertical Grids", modifier = Modifier.weight(1f))
                Switch(checked = showVGrid, onCheckedChange = onShowVGridChange)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Show Horizontal Grids", modifier = Modifier.weight(1f))
                Switch(checked = showHGrid, onCheckedChange = onShowHGridChange)
            }
            Text("Grid Opacity: ${(gridOpacity * 100).toInt()}%")
            Slider(value = gridOpacity, onValueChange = onGridOpacityChange, valueRange = 0f..1f)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
