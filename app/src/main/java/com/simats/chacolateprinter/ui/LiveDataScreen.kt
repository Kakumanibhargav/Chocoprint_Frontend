package com.simats.chacolateprinter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.chacolateprinter.utils.GCodeParser

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
    
    // Positions from G-code if hardware reporting is lagging or for simulation
    val lastGCodePos = remember(currentCommandIndex, commands) {
        var x = 0f; var y = 0f; var z = 0f
        if (commands.isNotEmpty()) {
            // Find the most recent X, Y, Z in the commands up to currentCommandIndex
            for (i in 0..currentCommandIndex) {
                val c = commands[i]
                if (c.x != null) x = c.x
                if (c.y != null) y = c.y
                if (c.z != null) z = c.z
            }
        }
        Triple(x, y, z)
    }

    // Use currentX/Y/Z if provided (from real hardware), otherwise fallback to G-code calculated position
    // If it's a simulation, we should prioritize the G-code position to see movement
    val isSimulation = !isPrinting
    
    val xPosValue = if (isSimulation || currentX == null || currentX == 0f) {
        "%.2f".format(lastGCodePos.first)
    } else {
        "%.2f".format(currentX)
    }

    val yPosValue = if (isSimulation || currentY == null || currentY == 0f) {
        "%.2f".format(lastGCodePos.second)
    } else {
        "%.2f".format(currentY)
    }

    val zPosValue = if (isSimulation || currentZ == null || currentZ == 0f) {
        "%.2f".format(lastGCodePos.third)
    } else {
        "%.2f".format(currentZ)
    }
    
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
        verticalAlignment = Alignment.CenterVertically) {
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
