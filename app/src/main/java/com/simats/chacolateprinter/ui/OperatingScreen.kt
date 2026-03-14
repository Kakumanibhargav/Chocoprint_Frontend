package com.simats.chacolateprinter.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.chacolateprinter.utils.GCodeParser
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatingScreen(
    onBackClick: () -> Unit,
    gCodeString: String,
    isConnected: Boolean,
    deviceName: String,
    isPrinting: Boolean,
    isPaused: Boolean,
    progress: Float,
    lastSentIndex: Int,
    consoleLogs: List<String>,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "ProgressAnimation")
    
    val commands = remember(gCodeString) { 
        GCodeParser.parse(gCodeString) 
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A120B),
            Color(0xFF000000)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Machine Control", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isConnected) "Connected: $deviceName" else "Disconnected",
                            color = if (isConnected) Color(0xFF81C784) else Color(0xFFE57373),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(brush = gradientBrush)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Progress Section
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFFB300),
                    strokeWidth = 12.dp,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Complete",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current Command Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Line:", color = Color.Gray, fontSize = 14.sp)
                        Text("${lastSentIndex + 1} / ${commands.size}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Command:", color = Color.Gray, fontSize = 14.sp)
                    val currentCmd = if (lastSentIndex >= 0 && lastSentIndex < commands.size) commands[lastSentIndex].originalLine else "None"
                    Text(
                        text = currentCmd,
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isPrinting || progress >= 1f) {
                    // Start Button
                    ControlIconButton(
                        icon = Icons.Default.PlayArrow,
                        label = "START",
                        color = Color(0xFF4CAF50),
                        onClick = onStartClick,
                        enabled = isConnected
                    )
                } else {
                    // Pause/Resume Button
                    ControlIconButton(
                        icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        label = if (isPaused) "RESUME" else "PAUSE",
                        color = Color(0xFFFFB300),
                        onClick = if (isPaused) onResumeClick else onPauseClick,
                        enabled = isConnected
                    )

                    // Stop Button
                    ControlIconButton(
                        icon = Icons.Default.Stop,
                        label = "STOP",
                        color = Color(0xFFD32F2F),
                        onClick = onStopClick,
                        enabled = isConnected
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Console Log
            Text(
                "CONSOLE LOG",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                val scrollState = rememberScrollState()
                LaunchedEffect(consoleLogs.size) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(scrollState)
                ) {
                    consoleLogs.forEach { log ->
                        Text(
                            text = log,
                            color = if (log.startsWith(">>")) Color(0xFF81C784) else if (log.startsWith("<<")) Color(0xFF64B5F6) else Color.White,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (consoleLogs.isEmpty()) {
                        Text("Terminal idle...", color = Color.DarkGray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(64.dp)
                .background(if (enabled) color.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, if (enabled) color.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = if (enabled) color else Color.Gray, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = if (enabled) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
