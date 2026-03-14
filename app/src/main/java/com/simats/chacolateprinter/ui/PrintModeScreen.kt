package com.simats.chacolateprinter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrintModeScreen(
    onBackClick: () -> Unit,
    onModeSelected: (String) -> Unit
) {
    var selectedMode by remember { mutableStateOf("Full Fill") } // Default selection

    // Gradient Background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3E2723), // Dark Chocolate
            Color(0xFF1B0000)  // Almost Black
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(16.dp)
    ) {
        // ... (Top Bar - same as before)
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
            RotatingCube(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Print Mode Selection",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Step 3 of 5 - Choose filling style",
                    color = Color(0xFFA1887F),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ... (Description Card - same as before) ...
        // Description Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1F1C)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC107)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Choose Your Print Style",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Select how you want your chocolate design to be printed.\nThis will affect the G-code generation and final result.",
                    color = Color(0xFFA1887F),
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Selection Cards
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrintModeCard(
                title = "Border Only",
                description = "Prints only the outline of your design. Perfect for creating hollow shapes, cookie cutters, or decorative borders.",
                points = listOf("Uses less chocolate", "Faster printing time", "Elegant outline design"),
                isSelected = selectedMode == "Border Only",
                onClick = { selectedMode = "Border Only" },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (selectedMode == "Border Only") Color(0xFFFFC107) else Color(0xFFA1887F),
                        modifier = Modifier.size(40.dp)
                    )
                },
                preview = { HeartPreview(filled = false) }
            )

            PrintModeCard(
                title = "Full Fill",
                description = "Prints the complete shape with chocolate filling inside. Creates solid, full-bodied chocolate pieces with rich texture.",
                points = listOf("Solid chocolate design", "Professional finish", "Maximum chocolate taste"),
                isSelected = selectedMode == "Full Fill",
                onClick = { selectedMode = "Full Fill" },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AspectRatio, // Expanding arrows look
                        contentDescription = null,
                        tint = if (selectedMode == "Full Fill") Color(0xFFFFC107) else Color(0xFFA1887F),
                        modifier = Modifier.size(40.dp)
                    )
                },
                preview = { HeartPreview(filled = true) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Button
        Button(
            onClick = { onModeSelected(selectedMode) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFAB00),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Continue to G-code Generation",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun PrintModeCard(
    title: String,
    description: String,
    points: List<String>,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    preview: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1F1C)),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFC107)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5D4037)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            // Top Section (Details)
            Box(modifier = Modifier.padding(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    
                    // Icon Container
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF3E2723), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = description,
                        color = Color(0xFFA1887F),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Bullet Points
                    Column(
                        horizontalAlignment = Alignment.Start, 
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        points.forEach { point ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFFFC107), RoundedCornerShape(50))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = point,
                                    color = Color(0xFFA1887F),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                
                // Selected Badge (Top Right)
                if (isSelected) {
                    Surface(
                        color = Color(0xFFFFC107),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "Selected",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Bottom Section (Preview Visual)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B0F0D)) // Darker background for preview area
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                preview()
            }
        }
    }
}

@Composable
fun HeartPreview(filled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(60.dp)) {
            val path = Path().apply {
                // Simplified Heart shape
                moveTo(size.width / 2, size.height * 0.9f)
                cubicTo(
                    size.width * 0.1f, size.height * 0.55f,
                    0f, size.height * 0.35f,
                    size.width * 0.25f, size.height * 0.1f
                )
                cubicTo(
                    size.width * 0.4f, 0f,
                    size.width * 0.5f, size.height * 0.15f,
                    size.width / 2, size.height * 0.25f
                )
                cubicTo(
                    size.width * 0.5f, size.height * 0.15f,
                    size.width * 0.6f, 0f,
                    size.width * 0.75f, size.height * 0.1f
                )
                cubicTo(
                    size.width, size.height * 0.35f,
                    size.width * 0.9f, size.height * 0.55f,
                    size.width / 2, size.height * 0.9f
                )
                close()
            }
            
            if (filled) {
                drawPath(path = path, color = Color(0xFFFFC107))
            } else {
                drawPath(
                    path = path, 
                    color = Color(0xFF6D4C41), // Darker outline for unselected/border mode
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (filled) "Filled completely" else "Outline only",
            color = Color(0xFFA1887F),
            fontSize = 10.sp
        )
    }
}
