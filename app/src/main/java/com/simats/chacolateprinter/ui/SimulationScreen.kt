package com.simats.chacolateprinter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import kotlin.math.cos
import kotlin.math.sin

data class Point3D(val x: Float, val y: Float, val z: Float, val isExtruding: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    onBackClick: () -> Unit,
    gCodeString: String,
    maxLayers: Int,
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
    var rotationXAxis by remember { mutableFloatStateOf(-30f) }
    var rotationYAxis by remember { mutableFloatStateOf(45f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var isSolidView by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    // Use a more robust parser that correctly handles Tool ON/OFF states
    val fullPath = remember(gCodeString) { 
        val points = mutableListOf<Point3D>()
        var cx = 0f; var cy = 0f; var cz = 0f
        var extruding = false
        
        gCodeString.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.startsWith("M3")) {
                extruding = true
            } else if (t.startsWith("M5")) {
                extruding = false
            } else if (t.startsWith("G0") || t.startsWith("G1") || t.startsWith("G2") || t.startsWith("G3")) {
                var nx = cx; var ny = cy; var nz = cz
                var hasCoord = false
                var hasE = false
                var eVal = 0f
                
                t.split("\\s+".toRegex()).forEach { part ->
                    if (part.length < 2) return@forEach
                    val value = part.substring(1).toFloatOrNull() ?: return@forEach
                    when (part[0].uppercaseChar()) {
                        'X' -> { nx = value; hasCoord = true }
                        'Y' -> { ny = value; hasCoord = true }
                        'Z' -> { nz = value; hasCoord = true }
                        'E' -> { eVal = value; hasE = true }
                    }
                }
                
                val isG1 = t.startsWith("G1")
                val isMovingExtruding = isG1 && (extruding || (hasE && eVal > 0.0001f))
                
                cx = nx; cy = ny; cz = nz
                // Map to 3D space: X=X, Y=Z (Vertical), Z=Y (Depth)
                if (hasCoord) points.add(Point3D(cx, cz, cy, isMovingExtruding))
            }
        }
        points
    }

    val currentLayer = (progress * maxLayers).toInt().coerceAtLeast(1)
    val printStats = remember(gCodeString) { GCodeParser.calculateStats(gCodeString) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3D Simulation", color = Color.White, fontWeight = FontWeight.Bold) },
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
                        rotationXAxis = -30f; rotationYAxis = 45f; zoom = 1.0f; panX = 0f; panY = 0f
                    }) {
                        Icon(Icons.Default.Replay, contentDescription = "Reset Camera", tint = Color.White)
                    }
                    IconButton(onClick = { isSolidView = !isSolidView }) {
                        Icon(if (isSolidView) Icons.Default.ViewInAr else Icons.Default.Fullscreen, contentDescription = null, tint = if (isSolidView) Color(0xFFFFC107) else Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.8f))
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
                            rotationXAxis -= pan.y * 0.4f
                        } else {
                            zoom = (zoom * zoomChange).coerceIn(0.1f, 10f)
                            panX += pan.x
                            panY += pan.y
                        }
                    }
                }
        ) {
            val rotYRad = Math.toRadians(rotationYAxis.toDouble()).toFloat()
            val rotXRad = Math.toRadians(rotationXAxis.toDouble()).toFloat()

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2 + panX, size.height / 2 + panY)
                val scale = (kotlin.math.min(size.width, size.height) / kotlin.math.max(bedWidth, bedDepth).coerceAtLeast(10f)) * 0.8f * zoom

                drawGrid(center, rotYRad, rotXRad, scale, bedWidth, bedDepth)
                drawAxes(center, rotYRad, rotXRad, scale, bedWidth, bedDepth)

                val pointsToDrawCount = (fullPath.size * progress).toInt().coerceIn(0, fullPath.size)
                if (pointsToDrawCount > 1) {
                    drawPath3D(fullPath.take(pointsToDrawCount), center, rotYRad, rotXRad, scale, isSolidView)
                }

                if (pointsToDrawCount > 0) {
                    val p = fullPath[pointsToDrawCount - 1]
                    val proj = project(p.x, p.y, p.z, scale, center, rotYRad, rotXRad)
                    drawCircle(Color.Red, radius = 6.dp.toPx(), center = proj)
                    drawCircle(Color.White, radius = 3.dp.toPx(), center = proj)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Layer $currentLayer / $maxLayers", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${(progress * 100).toInt()}%", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = progress,
                    onValueChange = { onProgressChange(it); onPlayingChange(false) },
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFC107), activeTrackColor = Color(0xFFFFC107))
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White)
                        Slider(value = playbackSpeed, onValueChange = onPlaybackSpeedChange, valueRange = 0.1f..10f, modifier = Modifier.padding(horizontal = 8.dp))
                        Text("${"%.1f".format(playbackSpeed)}x", color = Color(0xFFFFC107))
                    }
                    IconButton(onClick = { onPlayingChange(!isPlaying) }) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { onPlayingChange(false); onProgressChange(0f) }) {
                        Icon(Icons.Default.Replay, contentDescription = null, tint = Color.White)
                    }
                }
                if (progress >= 1f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onLiveDataClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                            Text("Live Data", color = Color.White)
                        }
                        Button(onClick = onProceedToPrint, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))) {
                            Text("Proceed to Print", color = Color.Black)
                        }
                    }
                }
            }
        }

        if (showStatsDialog && printStats != null) {
            AlertDialog(
                onDismissRequest = { showStatsDialog = false },
                containerColor = Color(0xFF1E1E1E),
                title = { Text("Print Statistics", color = Color.White) },
                text = {
                    Column {
                        DetailRow("Est. Print Time", formatTime(printStats.timeSeconds))
                        DetailRow("Material Length", String.format("%.2f m", printStats.materialLengthMm / 1000f))
                        DetailRow("Est. Weight", String.format("%.1f g", printStats.materialWeightGrams))
                    }
                },
                confirmButton = { Button(onClick = { showStatsDialog = false }) { Text("Close") } }
            )
        }
    }
}

private fun DrawScope.drawPath3D(points: List<Point3D>, center: Offset, rotY: Float, rotX: Float, scale: Float, isSolid: Boolean) {
    if (points.size < 2) return
    var lastP = points[0]
    var lastProj = project(lastP.x, lastP.y, lastP.z, scale, center, rotY, rotX)
    for (i in 1 until points.size) {
        val p = points[i]
        val proj = project(p.x, p.y, p.z, scale, center, rotY, rotX)
        if (p.isExtruding) {
            drawLine(
                color = if (isSolid) Color(0xFF5D4037) else Color(0xFFFFD700),
                start = lastProj,
                end = proj,
                strokeWidth = if (isSolid) 4.dp.toPx() else 1.5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        lastProj = proj
    }
}

private fun project(x: Float, y: Float, z: Float, scale: Float, center: Offset, rotY: Float, rotX: Float): Offset {
    val rX1 = x * cos(rotY) - z * sin(rotY)
    val rZ1 = x * sin(rotY) + z * cos(rotY)
    val rY2 = y * cos(rotX) - rZ1 * sin(rotX)
    return Offset(center.x + rX1 * scale, center.y - rY2 * scale)
}

private fun DrawScope.drawGrid(center: Offset, rotY: Float, rotX: Float, scale: Float, width: Float, depth: Float) {
    val step = 20f; val halfW = width / 2f; val halfD = depth / 2f
    for (i in -(depth / (2 * step)).toInt()..(depth / (2 * step)).toInt()) {
        val z = i * step
        drawLine(Color.White.copy(0.2f), project(-halfW, 0f, z, scale, center, rotY, rotX), project(halfW, 0f, z, scale, center, rotY, rotX), 1.dp.toPx())
    }
    for (i in -(width / (2 * step)).toInt()..(width / (2 * step)).toInt()) {
        val x = i * step
        drawLine(Color.White.copy(0.2f), project(x, 0f, -halfD, scale, center, rotY, rotX), project(x, 0f, halfD, scale, center, rotY, rotX), 1.dp.toPx())
    }
}

private fun DrawScope.drawAxes(center: Offset, rotY: Float, rotX: Float, scale: Float, width: Float, depth: Float) {
    val pO = project(0f, 0f, 0f, scale, center, rotY, rotX)
    drawLine(Color.Red, pO, project(50f, 0f, 0f, scale, center, rotY, rotX), 2.dp.toPx())
    drawLine(Color.Green, pO, project(0f, 50f, 0f, scale, center, rotY, rotX), 2.dp.toPx())
    drawLine(Color.Blue, pO, project(0f, 0f, 50f, scale, center, rotY, rotX), 2.dp.toPx())
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
