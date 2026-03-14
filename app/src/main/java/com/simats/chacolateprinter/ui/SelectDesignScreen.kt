package com.simats.chacolateprinter.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Square
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.chacolateprinter.model.DesignItem

// ... (Existing Imports)

@Composable
fun SelectDesignScreen(onBackClick: () -> Unit, onDesignSelected: (String, Uri?) -> Unit) {
    // Gradient Background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3E2723), // Dark Chocolate
            Color(0xFF1B0000)  // Almost Black
        )
    )

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sample, 1 = Upload Custom
    
    // File Picker Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        // When file is selected
        uri?.let {
            // Pass the URI to the callback
            onDesignSelected("Custom Design", it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(16.dp)
    ) {
        // ... (Top Bar and Tab Toggle code remains same) ...
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
                    text = "Select Design",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Step 1 of 5 - Choose or upload your chocolate design",
                    color = Color(0xFFA1887F),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tab Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2E1F1C)), // Darker background for tab container
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selected Tab State helpers
            val isSampleSelected = selectedTab == 0
            val isCustomSelected = selectedTab == 1

            // Sample Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .background(
                        if (isSampleSelected) Color(0xFFFFAB00) else Color.Transparent, 
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { selectedTab = 0 },
                contentAlignment = Alignment.Center
            ) {
                 Text(
                    text = "Sample\nDesigns",
                    color = if (isSampleSelected) Color.Black else Color(0xFFA1887F),
                    fontWeight = if (isSampleSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
            
            // Custom Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .background(
                        if (isCustomSelected) Color(0xFFFFAB00) else Color.Transparent, 
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { selectedTab = 1 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Upload Custom",
                    color = if (isCustomSelected) Color.Black else Color(0xFFA1887F),
                    fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedTab == 0) {
            // Sample Designs Grid
            Text(
                text = "Choose a Pre-made Design",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            val designs = listOf(
                DesignItem("Heart", Icons.Default.Favorite, Icons.Outlined.FavoriteBorder),
                DesignItem("Star", Icons.Default.Star, Icons.Outlined.Star),
                DesignItem("Circle", Icons.Default.Circle, Icons.Outlined.Circle),
                DesignItem("Square", Icons.Default.Square, Icons.Outlined.Square),
                DesignItem("Parallelogram", customShape = true),
                DesignItem("Triangle", customShape = true)
            )
            
            var selectedDesign by remember { mutableStateOf<String?>(null) }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(designs) { design ->
                    DesignCard(
                        design = design,
                        isSelected = selectedDesign == design.name,
                        onClick = { 
                            selectedDesign = design.name 
                            onDesignSelected(design.name, null)
                        }
                    )
                }
            }
        } else {
            // Upload Custom View
            // Launch picker for all file types (filtering can be stricter if needed, e.g. "image/*")
            UploadCustomView(onBrowseClick = { launcher.launch("*/*") })
        }
    }
}

@Composable
fun UploadCustomView(onBrowseClick: () -> Unit) {
    val stroke = Stroke(
        width = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFFFAB00),
                    style = stroke,
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .background(Color(0xFF2E1F1C).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(1.dp), // Avoid clipping border
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = "Upload",
                tint = Color(0xFFFFAB00),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Drop your design here",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            
            Text(
                text = "or click to browse",
                color = Color(0xFFA1887F),
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onBrowseClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFAB00),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Text(
                    text = "Browse Files",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FileTypeChip("PNG")
                FileTypeChip("SVG")
                FileTypeChip("STL")
            }
        }
    }
}

@Composable
fun FileTypeChip(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF3E2723), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFA1887F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DesignCard(design: DesignItem, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFFFFC107) else Color(0xFF5D4037)
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1F1C)),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .height(220.dp) // Taller to fit all elements
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1B0F0D), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (design.customShape) {
                    CustomShape(design.name, isFilled = true, size = 64.dp)
                } else {
                    Icon(
                        imageVector = design.icon!!,
                        contentDescription = design.name,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Icon + Label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (design.customShape) {
                     CustomShape(design.name, isFilled = false, size = 24.dp)
                } else {
                    Icon(
                        imageVector = design.outlinedIcon ?: design.icon!!,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = design.name,
                    color = if (isSelected) Color(0xFFFFC107) else Color(0xFFA1887F),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun CustomShape(name: String, isFilled: Boolean, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val path = Path()
        val w = this.size.width
        val h = this.size.height

        when (name) {
            "Parallelogram" -> {
                path.moveTo(0f, h)
                path.lineTo(w / 4, 0f)
                path.lineTo(w, 0f)
                path.lineTo(w * 3/4, h)
                path.close()
            }
            "Triangle" -> {
                path.moveTo(w / 2, 0f)
                path.lineTo(w, h)
                path.lineTo(0f, h)
                path.close()
            }
        }

        if (isFilled) {
             drawPath(
                path = path,
                color = Color(0xFFFFC107)
            )
        } else {
             drawPath(
                path = path,
                style = Stroke(width = 6f),
                color = Color(0xFFFFC107)
            )
        }
    }
}
