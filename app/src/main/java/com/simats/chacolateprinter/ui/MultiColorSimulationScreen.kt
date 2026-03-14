package com.simats.chacolateprinter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.chacolateprinter.utils.GCodeParser
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

data class MultiColorPoint3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val isExtruding: Boolean = false,
    val colorIndex: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiColorSimulationScreen(
    onBackClick: () -> Unit,
    gCodeString: String,
    maxLayers: Int,
    colors: List<Color>,
    bedWidth: Float = 200f,
    bedDepth: Float = 200f,
    onLiveDataClick: () -> Unit,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    onProceedToPrint: () -> Unit
) {
    // State for camera/view
    var rotationXAxis by remember { mutableFloatStateOf(-30f) }
    var rotationYAxis by remember { mutableFloatStateOf(45f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var isSolidView by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    // Parse G-code with color awareness and interpolation
    val fullPath = remember(gCodeString) { parseMultiColorGCode(gCodeString) }
    
    val pointsToDrawCount = remember(progress, fullPath.size) {
        (fullPath.size * progress).toInt().coerceIn(0, fullPath.size)
    }
    
    val currentPoint = if (pointsToDrawCount > 0) fullPath[pointsToDrawCount - 1] else null
    val currentProcessIndex = (currentPoint?.colorIndex ?: 0) + 1
    val currentLayer = (progress * maxLayers).toInt().coerceAtLeast(1)

    // Calculate Print Stats
    val printStats = remember(gCodeString) {
        GCodeParser.calculateStats(gCodeString)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Multi-Color Simulation", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onLiveDataClick) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Live Data", tint = Color(0xFFFFC107))
                    }
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Print Info", tint = Color.White)
                    }
                    IconButton(onClick = {
                        rotationXAxis = -30f
                        rotationYAxis = 45f
                        zoom = 1.0f
                        panX = 0f
                        panY = 0f
                    }) {
                        Icon(Icons.Default.Replay, contentDescription = "Reset Camera", tint = Color.White)
                    }
                    IconButton(onClick = { isSolidView = !isSolidView }) {
                        val icon = if (isSolidView) Icons.Default.ViewInAr else Icons.Default.Fullscreen
                        Icon(icon, contentDescription = "Toggle View Mode", tint = if (isSolidView) Color(0xFFFFC107) else Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1E1E1E).copy(alpha = 0.8f)
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        if (zoomChange == 1f) {
                            rotationYAxis += pan.x * 0.4f
                            rotationXAxis -= pan.y * 0.4f // Full 360 rotation
                        } else {
                            zoom = (zoom * zoomChange).coerceIn(0.01f, 100.0f) // Drastically increased zoom range
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
                val scale = (minScreenDim / maxBedDim) * 0.8f * zoom

                // Draw Grid & Axes centered at (0,0)
                drawMultiColorGrid(this, center, rotYRad, rotXRad, scale, bedWidth, bedDepth)
                drawMultiColorAxes(this, center, rotYRad, rotXRad, scale, bedWidth, bedDepth)

                // Draw Simulation Path
                if (pointsToDrawCount > 1) {
                    val path3D = fullPath.take(pointsToDrawCount).map { p ->
                        MultiColorPoint3D(p.x, p.z, p.y, p.isExtruding, p.colorIndex)
                    }
                    drawMultiColor3DPath(this, path3D, center, rotYRad, rotXRad, scale, isSolidView, bedWidth, bedDepth, colors)
                }

                // Nozzle Indicator
                if (pointsToDrawCount > 0 && pointsToDrawCount < fullPath.size) {
                    val lastP = fullPath[pointsToDrawCount - 1]
                    val projected = projectAndTranslate(lastP.x, lastP.z, lastP.y, scale, center, rotYRad, rotXRad)
                    drawCircle(Color.Red, radius = 6.dp.toPx(), center = projected)
                    drawCircle(Color.White, radius = 3.dp.toPx(), center = projected)
                }
            }

            // Controls & Info Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        val colorName = if (currentProcessIndex <= colors.size) "Color $currentProcessIndex" else "Unknown"
                        Text(colorName, color = colors.getOrElse(currentProcessIndex-1) { Color(0xFFFFC107) }, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${(progress * 100).toInt()}%", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Slider(
                    value = progress,
                    onValueChange = { onProgressChange(it); onPlayingChange(false) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFC107),
                        activeTrackColor = Color(0xFFFFC107),
                        inactiveTrackColor = Color.DarkGray
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White)
                        Slider(
                            value = playbackSpeed,
                            onValueChange = { onPlaybackSpeedChange(it) },
                            valueRange = 0.1f..10f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFC107),
                                activeTrackColor = Color(0xFFFFC107),
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text("${"%.1f".format(playbackSpeed)}x", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold)
                    }

                    Row {
                        IconButton(onClick = { onPlayingChange(!isPlaying) }) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = { onPlayingChange(false); onProgressChange(0f) }) {
                            Icon(Icons.Default.Replay, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                if (progress >= 1f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onLiveDataClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Live Data", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onProceedToPrint,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                        ) {
                            Text("Proceed to Print", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Stats Dialog
        if (showStatsDialog && printStats != null) {
            AlertDialog(
                onDismissRequest = { showStatsDialog = false },
                containerColor = Color(0xFF1E1E1E),
                title = { Text("Multi-Color Print Info", color = Color.White) },
                text = {
                    Column {
                        DetailRow("Est. Time", formatMultiColorTime(printStats.timeSeconds))
                        DetailRow("Material", String.format("%.2f m", printStats.materialLengthMm / 1000f))
                        DetailRow("Weight", String.format("%.1f g", printStats.materialWeightGrams))
                        DetailRow("Colors", colors.size.toString())
                        DetailRow("Processes", colors.size.toString())
                    }
                },
                confirmButton = {
                    Button(onClick = { showStatsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))) {
                        Text("Close", color = Color.Black)
                    }
                }
            )
        }
    }
}

private fun formatMultiColorTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

private fun drawMultiColorGrid(scope: DrawScope, center: Offset, rotY: Float, rotX: Float, scale: Float, width: Float, depth: Float) {
    scope.run {
        val step = 20f
        val color = Color.White.copy(alpha = 0.2f)
        val halfW = width / 2f
        val halfD = depth / 2f
        
        for (i in -(depth / (2 * step)).toInt()..(depth / (2 * step)).toInt()) {
            val zPos = i * step
            val p1 = projectAndTranslate(-halfW, 0f, zPos, scale, center, rotY, rotX)
            val p2 = projectAndTranslate(halfW, 0f, zPos, scale, center, rotY, rotX)
            drawLine(color, p1, p2, 1.dp.toPx())
        }
        for (i in -(width / (2 * step)).toInt()..(width / (2 * step)).toInt()) {
            val xPos = i * step
            val p1 = projectAndTranslate(xPos, 0f, -halfD, scale, center, rotY, rotX)
            val p2 = projectAndTranslate(xPos, 0f, halfD, scale, center, rotY, rotX)
            drawLine(color, p1, p2, 1.dp.toPx())
        }
    }
}

private fun drawMultiColorAxes(scope: DrawScope, center: Offset, rotY: Float, rotX: Float, scale: Float, width: Float, depth: Float) {
    scope.run {
        val pO = projectAndTranslate(0f, 0f, 0f, scale, center, rotY, rotX)
        drawLine(Color.Red, pO, projectAndTranslate(50f, 0f, 0f, scale, center, rotY, rotX), 2.dp.toPx())
        drawLine(Color.Green, pO, projectAndTranslate(0f, 50f, 0f, scale, center, rotY, rotX), 2.dp.toPx())
        drawLine(Color.Blue, pO, projectAndTranslate(0f, 0f, 50f, scale, center, rotY, rotX), 2.dp.toPx())
    }
}

private fun drawMultiColor3DPath(
    scope: DrawScope,
    points: List<MultiColorPoint3D>,
    center: Offset,
    rotY: Float,
    rotX: Float,
    scale: Float,
    isSolid: Boolean,
    width: Float,
    depth: Float,
    colors: List<Color>
) {
    scope.run {
        if (points.size < 2) return

        var lastProj = projectAndTranslate(points[0].x, points[0].y, points[0].z, scale, center, rotY, rotX)
        
        for (i in 1 until points.size) {
            val p = points[i]
            val proj = projectAndTranslate(p.x, p.y, p.z, scale, center, rotY, rotX)
            
            if (p.isExtruding) {
                val color = colors.getOrElse(p.colorIndex) { Color(0xFF5D4037) }
                drawLine(
                    color = if (isSolid) color.copy(alpha = 0.8f) else color,
                    start = lastProj,
                    end = proj,
                    strokeWidth = if (isSolid) 4.dp.toPx() else 1.5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            lastProj = proj
        }
    }
}

private fun projectAndTranslate(
    x: Float, y: Float, z: Float,
    scale: Float,
    center: Offset,
    rotY: Float,
    rotX: Float
): Offset {
    val rX1 = x * cos(rotY) - z * sin(rotY)
    val rZ1 = x * sin(rotY) + z * cos(rotY)
    val rY2 = y * cos(rotX) - rZ1 * sin(rotX)

    return Offset(center.x + rX1 * scale, center.y - rY2 * scale)
}

private fun parseMultiColorGCode(gcode: String): List<MultiColorPoint3D> {
    val points = mutableListOf<MultiColorPoint3D>()
    var x = 0f; var y = 0f; var z = 0f
    var colorIndex = 0
    var extruding = false
    
    gcode.lineSequence().forEach { line ->
        val trimmed = line.trim()
        
        if (trimmed.startsWith("M3")) {
            extruding = true
            return@forEach
        } else if (trimmed.startsWith("M5")) {
            extruding = false
            return@forEach
        }

        if (trimmed.startsWith("; Process")) {
            val parts = trimmed.split("\\s+".toRegex())
            val idx = parts.indexOf("Process")
            if (idx != -1 && idx + 1 < parts.size) {
                 colorIndex = (parts[idx + 1].toIntOrNull() ?: 1) - 1
            }
            return@forEach
        }
        
        if (trimmed.isEmpty() || trimmed.startsWith(";")) return@forEach

        if (trimmed.startsWith("G1") || trimmed.startsWith("G0")) {
            var newX = x; var newY = y; var newZ = z
            var hasCoord = false
            var hasE = false
            var eVal = 0f
            
            trimmed.split("\\s+".toRegex()).forEach { part ->
                if (part.length < 2) return@forEach
                val value = part.substring(1).toFloatOrNull() ?: return@forEach
                when (part[0].uppercaseChar()) {
                    'X' -> { newX = value; hasCoord = true }
                    'Y' -> { newY = value; hasCoord = true }
                    'Z' -> { newZ = value; hasCoord = true }
                    'E' -> { eVal = value; hasE = true }
                }
            }
            
            val isG1 = trimmed.startsWith("G1")
            val isExtruding = (isG1 && extruding) || (isG1 && hasE && eVal > 0.0001f)
            
            x = newX; y = newY; z = newZ
            if (hasCoord) points.add(MultiColorPoint3D(x, y, z, isExtruding, colorIndex))
        }
    }
    return points
}
