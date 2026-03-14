package com.simats.chacolateprinter.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.chacolateprinter.PrinterParameters
import com.simats.chacolateprinter.models.Point
import com.simats.chacolateprinter.utils.GCodeGenerator
import com.simats.chacolateprinter.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

@Composable
fun GCodeGenerationScreen(
    onBackClick: () -> Unit,
    onRunSimulationClick: (String, Int) -> Unit,
    designName: String,
    designUri: Uri?,
    printMode: String,
    printerParameters: PrinterParameters
) {
    val context = LocalContext.current
    var gCodeOutput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(true) }
    var printStats by remember { mutableStateOf<com.simats.chacolateprinter.utils.GCodeParser.PrintStats?>(null) }
    var maxLayers by remember { mutableStateOf(0) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(gCodeOutput.toByteArray())
                }
                Toast.makeText(context, "G-code text file saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Gradient Background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3E2723), // Dark Chocolate
            Color(0xFF1B0000)  // Almost Black
        )
    )

    // Trigger Generation
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
             val points: List<List<Point>> = if (designUri != null) {
                val bitmap = ImageUtils.loadBitmapFromUri(context, designUri)
                if (bitmap != null) {
                    if (printMode == "Border Only") {
                        ImageUtils.generateBorderPath(bitmap)
                    } else {
                        ImageUtils.generateFillPath(bitmap, printerParameters)
                    }
                } else {
                    emptyList()
                }
            } else {
                 emptyList() 
            }
            
            val result = if (designUri != null && points.isNotEmpty()) {
                 GCodeGenerator.generateGCode(points, printMode, printerParameters)
            } else {
                 GCodeGenerator.generateSampleGCode(designName, printMode, printerParameters)
            }
            
            gCodeOutput = result.gCode
            maxLayers = result.layerCount
            
            val nozzle = printerParameters.nozzleDiameter.toFloatOrNull() ?: 0.8f
            val layerH = printerParameters.layerHeight.toFloatOrNull() ?: 0.6f
            printStats = com.simats.chacolateprinter.utils.GCodeParser.calculateStats(result.gCode, nozzle, layerH)

            isGenerating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "G-code Generation",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = if (isGenerating) "Generating..." else "Generation Complete",
                    color = Color(0xFFA1887F),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (!isGenerating) {
                IconButton(onClick = { createDocumentLauncher.launch("${designName.ifEmpty { "design" }}.txt") }) {
                    Icon(Icons.Default.Download, contentDescription = "Download G-code", tint = Color(0xFFFFC107))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Run Simulation Button
        Button(
            onClick = { onRunSimulationClick(gCodeOutput, maxLayers) },
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFAB00),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF5D4037),
                disabledContentColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isGenerating) {
                     CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Text(
                        text = "Run Simulation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Print Summary Row
        if (!isGenerating && printStats != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(
                    label = "Time",
                    value = formatTime(printStats?.timeSeconds ?: 0),
                    unit = "",
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                StatCard(
                    label = "Material",
                    value = String.format("%.1f", (printStats?.materialLengthMm ?: 0f) / 1000f),
                    unit = "m",
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                StatCard(
                    label = "Weight",
                    value = String.format("%.1f", printStats?.materialWeightGrams ?: 0f),
                    unit = "g",
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // G-code Preview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B0F0D)),
            border = BorderStroke(1.dp, Color(0xFF5D4037)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "G-code Preview",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF3E2723), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val displayText = remember(gCodeOutput) {
                        if (gCodeOutput.length > 30000) {
                            val head = gCodeOutput.take(15000)
                            val tail = gCodeOutput.takeLast(5000)
                            """${head}

... [ ${gCodeOutput.length / 1024} KB of G-code ] ...

${tail}"""
                        } else {
                            gCodeOutput
                        }
                    }
                    Text(
                        text = displayText,
                        color = Color(0xFF4CAF50),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
