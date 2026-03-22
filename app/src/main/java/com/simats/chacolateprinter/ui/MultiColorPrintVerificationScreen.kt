package com.simats.chacolateprinter.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiColorPrintVerificationScreen(
    onBackClick: () -> Unit,
    onStartPrintClick: () -> Unit,
    shapeName: String,
    numColors: Int,
    baseX: Float,
    baseY: Float,
    baseZ: Float,
    incrementXY: Float,
    mode: String,
    selectedColors: List<Color>,
    isConnected: Boolean
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A120B),
            Color(0xFF000000)
        )
    )

    var showConnectionWarning by remember { mutableStateOf(false) }

    if (showConnectionWarning) {
        AlertDialog(
            onDismissRequest = { showConnectionWarning = false },
            title = { Text("No Device Connected") },
            text = { Text("Please connect to a printer before starting a print.") },
            confirmButton = {
                Button(onClick = { showConnectionWarning = false }) {
                    Text("OK")
                }
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Multi-Color",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Print Dashboard",
                            color = Color(0xFFFFB300),
                            fontSize = 14.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Surface(
                        color = if (isConnected) Color(0xFF81C784).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            if (isConnected) "Ready to Print" else "Not Connected",
                            color = if (isConnected) Color(0xFF81C784) else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(brush = gradientBrush)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Print Configuration
            item {
                VerificationCard(title = "Print Configuration") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ConfigItem(Modifier.weight(1f), "Shape", shapeName)
                            ConfigItem(
                                Modifier.weight(1f), 
                                "Base Dimensions", 
                                "X${baseX.toInt()}×Y${baseY.toInt()}×Z${baseZ.toInt()}", 
                                valueColor = Color(0xFFE57373)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ConfigItem(Modifier.weight(1f), "Number of Colors", numColors.toString())
                            ConfigItem(Modifier.weight(1f), "XY Increment", "+${incrementXY.toInt()}mm", valueColor = Color(0xFFFFB300))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ConfigItem(Modifier.weight(1f), "Fill Mode", mode)
                            ConfigItem(Modifier.weight(1f), "Est. Time", "1m 40s")
                        }
                    }
                }
            }

            // Process Timeline
            item {
                VerificationCard(title = "Process Timeline") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        (0 until numColors).forEach { index ->
                            // Calculate incrementing values for each process to match MultiColorConfigScreen
                            val currentX = baseX + (index * incrementXY)
                            val currentY = baseY + (index * incrementXY)
                            // Process 1 (index 0) is at Z=0, subsequent processes increment by baseZ
                            val currentZ = baseZ * index

                            ProcessTimelineItem(
                                index + 1,
                                selectedColors.getOrElse(index) { Color.Gray },
                                currentX, currentY, currentZ,
                                rotation = (index * (360 / numColors))
                            )
                            if (index < numColors - 1) Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // Color Stepper Motor Wheel
            item {
                VerificationCard(title = "Color Stepper Motor") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        StepperMotorWheel(numColors, selectedColors)
                    }
                }
            }

            // Pre-Print Checklist
            item {
                VerificationCard(title = "Pre-Print Checklist") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ChecklistItem("Printer is connected and powered on", isConnected)
                        ChecklistItem("Build plate is clean and level")
                        ChecklistItem("Chocolate colors are loaded")
                        ChecklistItem("Nozzle temperature set to 45°C")
                        ChecklistItem("G-code file is loaded and verified")
                    }
                }
            }

            // Start Button
            item {
                Button(
                    onClick = { 
                        if (isConnected) {
                            onStartPrintClick() 
                        } else {
                            showConnectionWarning = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Multi-Color Print", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE57373).copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Text(
                        "Safety Notice: Do not open the printer door or touch the build plate during printing. Wait for the process to complete and cool down.",
                        color = Color(0xFFE57373),
                        fontSize = 12.sp
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun VerificationCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            content()
        }
    }
}

@Composable
fun ConfigItem(modifier: Modifier, label: String, value: String, valueColor: Color = Color.White) {
    Column(modifier = modifier) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProcessTimelineItem(number: Int, color: Color, x: Float, y: Float, z: Float, rotation: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            color = color.copy(alpha = 0.2f),
            shape = CircleShape,
            border = BorderStroke(2.dp, color)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Process $number - Color $number", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("X${x.toInt()}  Y${y.toInt()}  Z${z.toInt()}", color = Color.Gray, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Rotation", color = Color.Gray, fontSize = 10.sp)
            Text("$rotation°", color = Color(0xFFFFB300), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ChecklistItem(text: String, isChecked: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isChecked) Icons.Default.CheckCircle else Icons.Default.Warning, 
            contentDescription = null, 
            tint = if (isChecked) Color(0xFF81C784) else Color.Red, 
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun StepperMotorWheel(numColors: Int, colors: List<Color>) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val sweepAngle = 360f / numColors
            for (i in 0 until numColors) {
                drawArc(
                    color = colors.getOrElse(i) { Color.Gray }.copy(alpha = 0.6f),
                    startAngle = i * sweepAngle - 90f,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                drawArc(
                    color = colors.getOrElse(i) { Color.Gray },
                    startAngle = i * sweepAngle - 90f,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            drawCircle(
                color = Color.Black,
                radius = 40.dp.toPx()
            )
            drawCircle(
                color = Color(0xFFFFB300),
                radius = 40.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("0°", color = Color(0xFFFFB300), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
