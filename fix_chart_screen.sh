#!/bin/bash

# We will completely replace InteractiveCandlestickChart in ChartScreen.kt to apply all grid, crosshair, text centering, and dynamic axis fixes.
# First, extract everything before InteractiveCandlestickChart
sed -n '1,/fun InteractiveCandlestickChart/p' app/src/main/java/com/soc/optionchain/screens/ChartScreen.kt | sed '$d' > ChartScreen_new.kt

# Append the new InteractiveCandlestickChart implementation
cat << 'INNER_EOF' >> ChartScreen_new.kt
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

        fun getY(price: Double): Float {
            return size.height - ((price.toFloat() - minPrice) / priceRange) * size.height
        }
        
        fun getPriceFromY(y: Float): Float {
            return minPrice + ((size.height - y) / size.height) * priceRange
        }

        if (showHGrid) {
            val roughInterval = priceRange / 8.0
            val magnitude = kotlin.math.pow(10.0, kotlin.math.floor(kotlin.math.log10(roughInterval)))
            val normalized = roughInterval / magnitude
            val niceNormalized = when {
                normalized < 1.5 -> 1.0
                normalized < 3.5 -> 2.0
                normalized < 7.5 -> 5.0
                else -> 10.0
            }
            val interval = niceNormalized * magnitude

            val firstGridPrice = kotlin.math.ceil(minPrice / interval) * interval
            var currentGridPrice = firstGridPrice
            while (currentGridPrice <= maxPrice) {
                val y = getY(currentGridPrice)
                drawLine(gridColor, Offset(0f, y), Offset(drawingAreaWidth, y), strokeWidth = 1f)
                
                val label = if (interval < 1.0) String.format("%.2f", currentGridPrice) else String.format("%.1f", currentGridPrice)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    drawingAreaWidth + 10f,
                    y - ((textPaint.descent() + textPaint.ascent()) / 2),
                    textPaint
                )
                currentGridPrice += interval
            }
        }
        
        drawLine(Color.Gray, Offset(drawingAreaWidth, 0f), Offset(drawingAreaWidth, size.height), strokeWidth = 2f)

        visibleCandles.forEachIndexed { i, candle ->
            val indexInList = startIndex + i
            val xCenter = drawingAreaWidth - ((indexInList * itemWidth) - offsetX) - (currentCandleWidth / 2)

            if (showVGrid && i % 5 == 0) {
                drawLine(gridColor, Offset(xCenter, 0f), Offset(xCenter, size.height), strokeWidth = 1f)
            }

            val yOpen = getY(candle.open)
            val yClose = getY(candle.close)
            val yHigh = getY(candle.high)
            val yLow = getY(candle.low)

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
INNER_EOF

# Finally, append the rest of the file (SettingsPanel onwards)
sed -n '/@Composable/,$p' app/src/main/java/com/soc/optionchain/screens/ChartScreen.kt | grep -A 1000 "fun SettingsPanel" >> ChartScreen_new.kt

mv ChartScreen_new.kt app/src/main/java/com/soc/optionchain/screens/ChartScreen.kt
