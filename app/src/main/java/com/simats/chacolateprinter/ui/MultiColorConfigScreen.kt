package com.simats.chacolateprinter.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiColorConfigScreen(
    onBackClick: () -> Unit,
    onGenerateGCodeClick: (shape: String, num: Int, x: Float, y: Float, z: Float, inc: Float, mode: String, servo: Int) -> Unit,
    onColorsUpdate: (List<Color>) -> Unit
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF261815),
            Color(0xFF1B0000)
        )
    )

    var selectedShape by remember { mutableStateOf("Heart") }
    var xAxisWidth by remember { mutableStateOf("10") }
    var yAxisHeight by remember { mutableStateOf("10") }
    var zAxisHeight by remember { mutableStateOf("5") }
    var fillMode by remember { mutableStateOf("Both") }
    var numberOfColors by remember { mutableStateOf("4") }
    var xyIncrement by remember { mutableStateOf("2") }
    var servoAngle by remember { mutableStateOf("90") }

    val colors = listOf(
        Color(0xFFE57373), // Red
        Color(0xFF4DB6AC), // Teal
        Color(0xFFFFF176), // Yellow
        Color(0xFF7986CB), // Indigo
        Color(0xFFFFB74D), // Orange
        Color(0xFFAED581), // Light Green
        Color(0xFF9575CD), // Deep Purple
        Color(0xFF4FC3F7)  // Light Blue
    )

    val num = numberOfColors.toIntOrNull() ?: 1
    val displayColors = colors.take(num)

    LaunchedEffect(displayColors) {
        onColorsUpdate(displayColors)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Multi-Color Configuration",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Step 1 - Setup Parameters",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        RotatingCube(modifier = Modifier.size(32.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(brush = gradientBrush)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Shape Selection
            ConfigSection(title = "Shape Selection", icon = Icons.Default.FilterVintage) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShapeButton("Heart", Icons.Default.Favorite, selectedShape == "Heart") { selectedShape = "Heart" }
                    ShapeButton("Star", Icons.Default.Star, selectedShape == "Star") { selectedShape = "Star" }
                    ShapeButton("Circle", Icons.Default.Circle, selectedShape == "Circle") { selectedShape = "Circle" }
                    ShapeButton("Square", Icons.Default.Square, selectedShape == "Square") { selectedShape = "Square" }
                    ShapeButton("Flower", Icons.Default.FilterVintage, selectedShape == "Flower") { selectedShape = "Flower" }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Axis Configuration
            ConfigSection(title = "Axis Configuration") {
                AxisInput("X-Axis (Width mm)", xAxisWidth, Color.Red) { xAxisWidth = it }
                Spacer(modifier = Modifier.height(8.dp))
                AxisInput("Y-Axis (Height mm)", yAxisHeight, Color.Green) { yAxisHeight = it }
                Spacer(modifier = Modifier.height(8.dp))
                AxisInput("Z-Axis (Step mm)", zAxisHeight, Color.Blue) { zAxisHeight = it }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fill Mode
            ConfigSection(title = "Fill Mode") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton("Border", fillMode == "Border", Modifier.weight(1f)) { fillMode = "Border" }
                    ModeButton("Infill", fillMode == "Infill", Modifier.weight(1f)) { fillMode = "Infill" }
                    ModeButton("Both", fillMode == "Both", Modifier.weight(1f)) { fillMode = "Both" }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-Color Configuration
            ConfigSection(title = "Multi-Color Configuration") {
                Column {
                    Text("Number of Colors (n)", color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = numberOfColors,
                        onValueChange = { numberOfColors = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFC107),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("X/Y Increment per Process (mm)", color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = xyIncrement,
                        onValueChange = { xyIncrement = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFC107),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text("How much to increase X and Y for each process", color = Color.Gray, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Angle for Servo (°)", color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = servoAngle,
                        onValueChange = { servoAngle = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFC107),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF261815), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        InfoRow("Base dimensions: X$xAxisWidth Y$yAxisHeight")
                        InfoRow("Increment: +$xyIncrement mm per process (X & Y)")
                        InfoRow("Z-height Step: $zAxisHeight mm")
                        val rotation = if (num > 0) 360f / num else 0f
                        InfoRow("Motor rotation: ${String.format("%.1f", rotation)}° per color")
                        InfoRow("Total processes: $num")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Stepper Motor Preview
            ConfigSection(title = "Color Stepper Motor Preview", icon = Icons.Default.FilterVintage) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                        Canvas(modifier = Modifier.size(200.dp)) {
                            val sweepAngle = 360f / num
                            displayColors.forEachIndexed { index, color ->
                                drawArc(
                                    color = color.copy(alpha = 0.8f),
                                    startAngle = index * sweepAngle - 90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = true
                                )
                                // Draw border
                                drawArc(
                                    color = Color.White.copy(alpha = 0.3f),
                                    startAngle = index * sweepAngle - 90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                )
                            }
                            
                            // Center circle for angle
                            drawCircle(
                                color = Color(0xFF1B0000),
                                radius = size.minDimension / 6
                            )
                            drawCircle(
                                color = Color(0xFFFFC107),
                                radius = size.minDimension / 6,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                            )
                        }
                        
                        Text(
                            text = "${String.format("%.0f", 360f / num)}°",
                            color = Color(0xFFFFC107),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "each color requires ${String.format("%.1f", 360f / num)}° rotation",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progressive Scaling Preview
            ConfigSection(title = "Progressive Scaling Preview") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..num) {
                        val color = displayColors[(i-1) % displayColors.size]
                        val currentX = (xAxisWidth.toIntOrNull() ?: 10) + (i-1) * (xyIncrement.toIntOrNull() ?: 0)
                        val currentY = (yAxisHeight.toIntOrNull() ?: 10) + (i-1) * (xyIncrement.toIntOrNull() ?: 0)
                        // Process 1 is Z=0, next processes increment by zAxisHeight
                        val currentZ = (zAxisHeight.toIntOrNull() ?: 5) * (i-1)
                        ProcessRow(i, color, "X$currentX Y$currentY Z$currentZ")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Continue Button
            Button(
                onClick = {
                    onGenerateGCodeClick(
                        selectedShape,
                        numberOfColors.toIntOrNull() ?: 1,
                        xAxisWidth.toFloatOrNull() ?: 10f,
                        yAxisHeight.toFloatOrNull() ?: 10f,
                        zAxisHeight.toFloatOrNull() ?: 5f,
                        xyIncrement.toFloatOrNull() ?: 2f,
                        fillMode,
                        servoAngle.toIntOrNull() ?: 90
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Generate Gcode", color = Color.Black, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ConfigSection(title: String, icon: ImageVector? = null, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1F1C).copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, Color.DarkGray),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ShapeButton(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFFFFC107) else Color(0xFF2E1F1C)
    val contentColor = if (isSelected) Color.Black else Color.Gray

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, if (isSelected) Color(0xFFFFC107) else Color.DarkGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = contentColor, fontSize = 12.sp)
        }
    }
}

@Composable
fun AxisInput(label: String, value: String, borderColor: Color, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = borderColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor.copy(alpha = 0.5f)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun ModeButton(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFFFFC107) else Color(0xFF2E1F1C)
    val contentColor = if (isSelected) Color.Black else Color.Gray

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, if (isSelected) Color(0xFFFFC107) else Color.DarkGray, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = contentColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("•", color = Color.White, modifier = Modifier.padding(end = 8.dp))
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun ProcessRow(index: Int, color: Color, dimensions: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF261815), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(16.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Text("Process $index", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(dimensions, color = color, fontSize = 12.sp)
    }
}
