package com.simats.chacolateprinter.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MultiColorSendingToHardwareScreen(
    onComplete: () -> Unit,
    isConnected: Boolean,
    gCode: String,
    onConnectionLost: () -> Unit,
    bluetoothViewModel: BluetoothViewModel,
    wifiViewModel: WifiViewModel
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "ProgressAnimation")
    var connectionLost by remember { mutableStateOf(false) }

    val connectedBluetoothDevice by bluetoothViewModel.connectedDevice.collectAsState()
    val connectedWifiNetwork by wifiViewModel.connectedNetwork.collectAsState()

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            connectionLost = true
        }
    }

    LaunchedEffect(Unit) {
        if (isConnected) {
            val lines = gCode.lines().filter { it.isNotBlank() }
            val totalLines = lines.size
            if (totalLines > 0) {
                // Target total transmission time of ~1.5 seconds
                val targetDurationMs = 1500L
                val delayPerLine = (targetDurationMs.toFloat() / totalLines).toLong().coerceAtLeast(0)
                val batchSize = if (delayPerLine < 1) (totalLines / (targetDurationMs / 1)).toInt().coerceAtLeast(1) else 1

                for (i in lines.indices step batchSize) {
                    val end = (i + batchSize).coerceAtMost(totalLines)
                    for (j in i until end) {
                        val line = lines[j] + "\n"
                        if (connectedBluetoothDevice != null) {
                            bluetoothViewModel.sendGCode(line)
                        } else if (connectedWifiNetwork != null) {
                            wifiViewModel.sendGCode(line)
                        }
                    }
                    
                    if (delayPerLine >= 1) {
                        delay(delayPerLine)
                    } else {
                        delay(1) // Minimal delay to keep UI responsive
                    }
                    progress = end / totalLines.toFloat()
                }
            }
            progress = 1f
            delay(300) // Brief pause at 100%
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0B08)), // Darkest brown/black
        contentAlignment = Alignment.Center
    ) {
        if (connectionLost) {
            ConnectionLostWarning(onConfirm = onConnectionLost)
        } else {
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E160A)), // Dark wood/card brown
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3E2D1A))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wifi",
                        tint = Color(0xFFFFAB00),
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Sending to Hardware",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Transmitting Multi-Color G-code...",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { animatedProgress.coerceAtMost(1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFFFFAB00),
                        trackColor = Color(0xFF2D1E0A),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "${(animatedProgress.coerceAtMost(1f) * 100).toInt()}% transmitted",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
