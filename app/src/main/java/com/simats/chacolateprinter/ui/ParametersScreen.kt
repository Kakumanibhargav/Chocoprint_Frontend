package com.simats.chacolateprinter.ui

import com.simats.chacolateprinter.PrinterParameters
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ParametersScreen(
    onBackClick: () -> Unit,
    onGenerateGCodeClick: (PrinterParameters) -> Unit,
    initialParameters: PrinterParameters
) {
    val scrollState = rememberScrollState()

    // Gradient Background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3E2723), // Dark Chocolate
            Color(0xFF1B0000)  // Almost Black
        )
    )

    // State for inputs - Initialize from initialParameters
    var layerHeight by remember { mutableStateOf(initialParameters.layerHeight) }
    var printSpeed by remember { mutableStateOf(initialParameters.printSpeed) }
    var travelSpeed by remember { mutableStateOf(initialParameters.travelSpeed) }
    var flowRate by remember { mutableStateOf(initialParameters.flowRate) }
    var infillDensity by remember { mutableFloatStateOf(initialParameters.infillDensity) }
    var wallThickness by remember { mutableStateOf(initialParameters.wallThickness) }
    var nozzleDiameter by remember { mutableStateOf(initialParameters.nozzleDiameter) }
    var firstLayerHeight by remember { mutableStateOf(initialParameters.firstLayerHeight) }
    var retractionDistance by remember { mutableStateOf(initialParameters.retractionDistance) }
    
    var acceleration by remember { mutableStateOf(initialParameters.acceleration) }
    var jerk by remember { mutableStateOf(initialParameters.jerk) }
    var servoAngle by remember { mutableStateOf(initialParameters.servoAngle) }

    // New parameters
    var shapeWidth by remember { mutableStateOf(initialParameters.shapeWidth) }
    var shapeHeight by remember { mutableStateOf(initialParameters.shapeHeight) }
    var numLayers by remember { mutableStateOf(initialParameters.numLayers) }
    
    var isAdvancedExpanded by remember { mutableStateOf(true) }

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
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Print Parameters",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Configure Slicing Settings",
                    color = Color(0xFFA1887F),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Shape Dimensions Card
            ParameterCard(title = "Design Dimensions") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ParameterInput(label = "Width (mm)", value = shapeWidth, onValueChange = { shapeWidth = it })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ParameterInput(label = "Height (mm)", value = shapeHeight, onValueChange = { shapeHeight = it })
                    }
                }
                ParameterInput(label = "Number of Layers", value = numLayers, onValueChange = { numLayers = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Print Settings Card
            ParameterCard(title = "Print Settings") {
                ParameterInput(label = "Layer Height (mm)", value = layerHeight, onValueChange = { layerHeight = it })
                ParameterInput(label = "Print Speed (mm/s)", value = printSpeed, onValueChange = { printSpeed = it })
                ParameterInput(label = "Travel Speed (mm/s)", value = travelSpeed, onValueChange = { travelSpeed = it })
                
                // Infill Slider
                Text(
                    text = "Infill Density (%)",
                    color = Color(0xFFA1887F),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0%", color = Color(0xFFA1887F), fontSize = 12.sp)
                    Slider(
                        value = infillDensity,
                        onValueChange = { infillDensity = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFC107),
                            activeTrackColor = Color(0xFFFFC107),
                            inactiveTrackColor = Color(0xFF5D4037)
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("100%", color = Color(0xFFA1887F), fontSize = 12.sp)
                }
                Text(
                    text = "${infillDensity.toInt()}%",
                    color = Color(0xFFFFC107),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                ParameterInput(label = "Nozzle Diameter (mm)", value = nozzleDiameter, onValueChange = { nozzleDiameter = it })
                ParameterInput(label = "Angle for Servo (°)", value = servoAngle, onValueChange = { servoAngle = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced Settings
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1F1C)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5D4037)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAdvancedExpanded = !isAdvancedExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Advanced Settings",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = Color(0xFFFFC107)
                        )
                    }
                    
                    if (isAdvancedExpanded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ParameterInput(label = "Acceleration (mm/s²)", value = acceleration, onValueChange = { acceleration = it })
                        ParameterInput(label = "Jerk (mm/s)", value = jerk, onValueChange = { jerk = it })
                        ParameterInput(label = "Retraction (mm)", value = retractionDistance, onValueChange = { retractionDistance = it })
                        ParameterInput(label = "Flow Rate (%)", value = flowRate, onValueChange = { flowRate = it })
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5D4037)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "Back",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Button(
                onClick = { 
                    onGenerateGCodeClick(
                        PrinterParameters(
                            layerHeight, printSpeed, travelSpeed, flowRate, infillDensity,
                            wallThickness, nozzleDiameter, firstLayerHeight, retractionDistance,
                            initialParameters.xMax, initialParameters.yMax, initialParameters.zMax, acceleration, jerk, servoAngle,
                            shapeWidth, shapeHeight, numLayers
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFAB00),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "Generate G-code",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ParameterCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1F1C)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5D4037)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Color(0xFFFFC107),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun ParameterInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, color = Color(0xFFA1887F), fontSize = 12.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFC107),
                unfocusedBorderColor = Color(0xFF5D4037),
                cursorColor = Color(0xFFFFC107)
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}
