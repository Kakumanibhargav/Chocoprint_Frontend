package com.simats.chacolateprinter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.chacolateprinter.utils.GCodeParser
import com.simats.chacolateprinter.utils.GCodeCommand
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDataScreen(
    onBackClick: () -> Unit,
    gCodeString: String,
    progress: Float,
    maxLayers: Int,
    currentX: Float? = null,
    currentY: Float? = null,
    currentZ: Float? = null,
    isPrinting: Boolean = false,
    isPaused: Boolean = false,
    onStopClick: () -> Unit = {},
    onPauseClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    bedWidth: Float = 200f,
    bedDepth: Float = 200f,
    colors: List<Color> = emptyList()
) {
    // State for camera/view
    var rotationXAxis by remember { mutableFloatStateOf(-30f) }
    var rotationYAxis by remember { mutableFloatStateOf(45f) }
    var zoom by remember { mutableFloatStateOf(0.8f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Parse G-code for live tracking
    val commands = remember(gCodeString) { 
        if (gCodeString.isBlank()) emptyList() else GCodeParser.parse(gCodeString) 
    }
    
    val totalLinesCount = commands.size
    
    // Improved command index calculation
    val currentCommandIndex = remember(progress, commands) {
        if (commands.isEmpty()) 0
        else {
            (progress * (commands.size - 1)).toInt().coerceIn(0, commands.size - 1)
        }
    }
    
    val currentCmd = if (commands.isNotEmpty()) commands[currentCommandIndex] else null
    
    // Positions from G-code if hardware reporting is lagging
    val lastGCodePos = remember(currentCommandIndex, commands) {
        var x = 0f; var y = 0f; var z = 0f
        if (commands.isNotEmpty()) {
            for (i in 0..currentCommandIndex) {
                val c = commands[i]
                if (c.x != null) x = c.x
                if (c.y != null) y = c.y
                if (c.z != null) z = c.z
            }
        }
        Triple(x, y, z)
    }

    val xPosValue = "%.2f".format(currentX ?: lastGCodePos.first)
    val yPosValue = "%.2f".format(currentY ?: lastGCodePos.second)
    val zPosValue = "%.2f".format(currentZ ?: lastGCodePos.third)
    
    // Detect Multi-Color mode
    val isMultiColor = remember(commands) { commands.any { it.processNum > 1 } }
    val currentProcessValue = currentCmd?.processNum ?: 1
    
    val displayLayer = if (isMultiColor) {
        currentProcessValue
    } else {
        (progress * maxLayers).toInt().coerceIn(1, maxLayers)
    }
    
    val remainingValue = (maxLayers - displayLayer).coerceAtLeast(0)
    
    val layerLabel = if (isMultiColor) "PROCESS" else "ACTIVE LAYER"
    val remainingLabel = if (isMultiColor) "STEPS LEFT" else "LAYERS LEFT"
    
    val currentCommandString = currentCmd?.let { 
        if (it.command.isBlank()) {
            if (it.originalLine.startsWith(";")) it.originalLine.take(40).trim() else "Transmitting..."
        } 
        else "${it.command}${it.x?.let { " X$it" } ?: ""}${it.y?.let { " Y$it" } ?: ""}${it.z?.let { " Z$it" } ?: ""}${it.f?.let { " F$it" } ?: ""}"
    } ?: "Waiting..."

    val printStats = remember(gCodeString) {
        if (gCodeString.isBlank()) null else GCodeParser.calculateStats(gCodeString)
    }
    val estimatedTimeValue = printStats?.let { formatTimeShort(it.timeSeconds) } ?: "00:00"
    val chocolateUsageValue = "${printStats?.materialWeightGrams?.toInt() ?: 0}g"

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF261815),
            Color(0xFF1B0000)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = Color(0xFFFFCC00),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Live Print Tracking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(brush = gradientBrush)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 3D LIVE VIEW CARD
            LiveDataCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoomChange, _ ->
                                if (zoomChange == 1f) {
                                    rotationYAxis += pan.x * 0.4f
                                    rotationXAxis -= pan.y * 0.4f
                                } else {
                                    zoom = (zoom * zoomChange).coerceIn(0.1f, 5.0f)
                                    panX += pan.x
                                    panY += pan.y
                                }
                            }
                        }
                ) {
                    val rotYRad = Math.toRadians(rotationYAxis.toDouble()).toFloat()
                    val rotXRad = Math.toRadians(rotationXAxis.toDouble()).toFloat()

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val (w, h) = size.width to size.height
                        val center = Offset(w / 2 + panX, h / 2 + panY)
                        val maxBedDim = kotlin.math.max(bedWidth, bedDepth).coerceAtLeast(10f)
                        val minScreenDim = kotlin.math.min(w, h)
                        val scale = (minScreenDim / maxBedDim) * 0.7f * zoom

                        // Draw Grid
                        drawLiveGrid(this, center, rotYRad, rotXRad, scale, bedWidth, bedDepth)

                        // Draw Path (Simulation style)
                        if (commands.isNotEmpty() && currentCommandIndex > 0) {
                            drawLive3DPath(this, commands, currentCommandIndex, center, rotYRad, rotXRad, scale, colors)
                        }

                        // Nozzle Indicator
                        val lastX = currentX ?: lastGCodePos.first
                        val lastY = currentY ?: lastGCodePos.second
                        val lastZ = currentZ ?: lastGCodePos.third
                        val projected = projectLive(lastX, lastY, lastZ, scale, center, rotYRad, rotXRad)
                        drawCircle(Color.Red, radius = 5.dp.toPx(), center = projected)
                        drawCircle(Color.White, radius = 2.dp.toPx(), center = projected)
                    }
                    
                    Text(
                        "3D LIVE VIEW",
                        modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = {
                            rotationXAxis = -30f
                            rotationYAxis = 45f
                            zoom = 0.8f
                            panX = 0f
                            panY = 0f
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Control Panel
            if (isPrinting || isPaused) {
                LiveDataCard {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (isPaused) onResumeClick() else onPauseClick() },
                            modifier = Modifier.size(48.dp).background(Color(0xFFFFB300), CircleShape)
                        ) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                        
                        Button(
                            onClick = onStopClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(48.dp).padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("STOP PRINT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // COORDINATES Card
            LiveDataCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("REAL-TIME COORDINATES", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    CoordinateLine("X Axis", xPosValue, Color(0xFFE57373))
                    Spacer(modifier = Modifier.height(12.dp))
                    CoordinateLine("Y Axis", yPosValue, Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.height(12.dp))
                    CoordinateLine("Z Axis", zPosValue, Color(0xFF2196F3))
                }
            }

            // LAYER / PROCESS Status Row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LiveDataCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(layerLabel, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(displayLayer.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                LiveDataCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(remainingLabel, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(remainingValue.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // PROGRESS Card
            LiveDataCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PRINT PROGRESS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${(progress * 100).format(1)}%", color = Color(0xFFFFB300), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${currentCommandIndex + 1} / $totalLinesCount cmd", color = Color.Gray, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFFFB300),
                        trackColor = Color(0xFF1E1E1E),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // TRANSMISSION Card
            LiveDataCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LATEST TRANSMISSION", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(currentCommandString, color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // SUMMARY Card
            LiveDataCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("JOB SUMMARY", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatItem("Estimated Time:", estimatedTimeValue)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatItem("Chocolate Usage:", chocolateUsageValue)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun drawLiveGrid(scope: DrawScope, center: Offset, rotY: Float, rotX: Float, scale: Float, width: Float, depth: Float) {
    scope.run {
        val step = 20f
        val color = Color.White.copy(alpha = 0.15f)
        val halfW = width / 2f
        val halfD = depth / 2f
        for (i in -(depth / (2 * step)).toInt()..(depth / (2 * step)).toInt()) {
            val z = i * step
            val p1 = projectLive(-halfW, 0f, z, scale, center, rotY, rotX)
            val p2 = projectLive(halfW, 0f, z, scale, center, rotY, rotX)
            drawLine(color, p1, p2, 0.5.dp.toPx())
        }
        for (i in -(width / (2 * step)).toInt()..(width / (2 * step)).toInt()) {
            val x = i * step
            val p1 = projectLive(x, 0f, -halfD, scale, center, rotY, rotX)
            val p2 = projectLive(x, 0f, halfD, scale, center, rotY, rotX)
            drawLine(color, p1, p2, 0.5.dp.toPx())
        }
    }
}

private fun drawLive3DPath(
    scope: DrawScope,
    commands: List<GCodeCommand>,
    count: Int,
    center: Offset,
    rotY: Float,
    rotX: Float,
    scale: Float,
    colors: List<Color>
) {
    scope.run {
        val cosY = cos(rotY); val sinY = sin(rotY)
        val cosX = cos(rotX); val sinX = sin(rotX)
        
        var lastX = 0f; var lastY = 0f; var lastZ = 0f
        var started = false
        var currentLX = 0f; var currentLY = 0f

        for (i in 0 until count) {
            val cmd = commands[i]
            val nx = cmd.x ?: lastX
            val ny = cmd.y ?: lastY
            val nz = cmd.z ?: lastZ
            
            val rX1 = nx * cosY - nz * sinY
            val rZ1 = nx * sinY + nz * cosY
            val rY2 = ny * cosX - rZ1 * sinX
            val px = center.x + rX1 * scale
            val py = center.y - rY2 * scale
            
            if (cmd.isExtruding) {
                if (started) {
                    val color = if (colors.isNotEmpty()) {
                        colors.getOrElse(cmd.processNum - 1) { Color(0xFFFFD700) }
                    } else Color(0xFFFFD700)

                    drawLine(
                        color = color,
                        start = Offset(currentLX, currentLY),
                        end = Offset(px, py),
                        strokeWidth = 1.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
                started = true
            } else {
                started = false
            }
            lastX = nx; lastY = ny; lastZ = nz
            currentLX = px; currentLY = py
        }
    }
}

private fun projectLive(x: Float, y: Float, z: Float, scale: Float, center: Offset, rotY: Float, rotX: Float): Offset {
    val rX1 = x * cos(rotY) - z * sin(rotY)
    val rZ1 = x * sin(rotY) + z * cos(rotY)
    val rY2 = y * cos(rotX) - rZ1 * sin(rotX)
    return Offset(center.x + rX1 * scale, center.y - rY2 * scale)
}

@Composable
fun LiveDataCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.25f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        content()
    }
}

@Composable
fun CoordinateLine(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text("mm", color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

private fun formatTimeShort(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}m ${s}s"
}

private fun Float.format(digits: Int) = "%.${digits}f".format(this)
