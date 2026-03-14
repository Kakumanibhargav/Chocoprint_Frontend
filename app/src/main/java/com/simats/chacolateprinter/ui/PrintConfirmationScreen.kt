package com.simats.chacolateprinter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.chacolateprinter.PrinterParameters
import com.simats.chacolateprinter.utils.GCodeParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintConfirmationScreen(
    onBackClick: () -> Unit,
    onStartPrintClick: () -> Unit,
    gCodeString: String,
    parameters: PrinterParameters,
    maxLayers: Int,
    isConnected: Boolean
) {
    val stats = remember(gCodeString) { GCodeParser.calculateStats(gCodeString) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Print Confirmation", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Review before printing", color = Color.Gray, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ViewInAr, contentDescription = "Logo", tint = Color(0xFFFFC107))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A120B))
            )
        },
        containerColor = Color(0xFF1A120B)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Ready to Print Warning
                item {
                    StatusCard(
                        icon = Icons.Default.Warning,
                        title = "Ready to Print",
                        message = "Please ensure the printer is properly calibrated and chocolate is loaded before proceeding.",
                        backgroundColor = Color(0xFF2D2416),
                        accentColor = Color(0xFFFFC107)
                    )
                }

                // Connection Status
                item {
                    SectionCard(title = "Connection") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF140F0A), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF2D2416), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFFFFC107))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Connection Type", color = Color.Gray, modifier = Modifier.weight(1f))
                            if (isConnected) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFF4CAF50))
                            } else {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                            }
                        }
                    }
                }

                // Parameter Profile
                item {
                    SectionCard(title = "Selected Parameter Profile") {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ParameterItem(label = "Layer Height", value = "${parameters.layerHeight} mm", modifier = Modifier.weight(1.0f))
                            Spacer(modifier = Modifier.width(8.dp))
                            ParameterItem(label = "Print Speed", value = "${parameters.printSpeed} mm/s", modifier = Modifier.weight(1.0f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ParameterItem(label = "Infill Density", value = "${parameters.infillDensity.toInt()}%", modifier = Modifier.weight(1.0f))
                            Spacer(modifier = Modifier.width(8.dp))
                            ParameterItem(label = "Nozzle Diameter", value = "${parameters.nozzleDiameter} mm", modifier = Modifier.weight(1.0f))
                        }
                    }
                }

                // Print Estimates
                item {
                    SectionCard(title = "Print Estimates") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EstimateRow("Estimated Time", formatTimeLong(stats.timeSeconds))
                            EstimateRow("Total Layers", maxLayers.toString())
                            EstimateRow("Material Required", "${stats.materialWeightGrams.toInt()}g")
                        }
                    }
                }

                // Firmware Compatibility
                item {
                    StatusCard(
                        icon = Icons.Default.CheckCircleOutline,
                        title = "Firmware Compatibility",
                        message = "Printer firmware v2.1.0 is compatible with this G-code profile.",
                        backgroundColor = Color(0xFF1B2616),
                        accentColor = Color(0xFF81C784)
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // Footer Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }
                Button(
                    onClick = onStartPrintClick,
                    enabled = isConnected,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Start Print", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun StatusCard(icon: ImageVector, title: String, message: String, backgroundColor: Color, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(message, color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF251A10)),
            border = BorderStroke(1.dp, Color(0xFF3B2A1A))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ParameterItem(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF140F0A), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = Color.Gray, fontSize = 10.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun EstimateRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF140F0A), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun formatTimeLong(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
