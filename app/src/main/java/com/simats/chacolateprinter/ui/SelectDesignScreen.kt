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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SelectDesignScreen(onBackClick: () -> Unit, onDesignSelected: (String, Uri?) -> Unit) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3E2723), 
            Color(0xFF1B0000)
        )
    )

    var selectedTab by remember { mutableIntStateOf(0) } 
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onDesignSelected("Custom Design", it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(16.dp)
    ) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2E1F1C)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isSampleSelected = selectedTab == 0
            val isCustomSelected = selectedTab == 1

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
                DesignItem("Triangle", customShape = true),
                DesignItem("Hexagon", customShape = true),
                DesignItem("Pentagon", customShape = true),
                DesignItem("Diamond", customShape = true),
                DesignItem("Moon", customShape = true),
                DesignItem("Cat", customShape = true),
                DesignItem("Bird", customShape = true),
                DesignItem("Butterfly", customShape = true),
                DesignItem("Fish", customShape = true)
            )
            
            var selectedDesign by remember { mutableStateOf<String?>(null) }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
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
            UploadCustomView(onBrowseClick = { launcher.launch("image/*") })
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
            .padding(1.dp), 
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
                FileTypeChip("JPG")
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
            .height(220.dp) 
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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
                path.moveTo(w * 0.2f, h * 0.8f)
                path.lineTo(w * 0.4f, h * 0.2f)
                path.lineTo(w * 0.8f, h * 0.2f)
                path.lineTo(w * 0.6f, h * 0.8f)
                path.close()
            }
            "Triangle" -> {
                path.moveTo(w / 2, h * 0.2f)
                path.lineTo(w * 0.8f, h * 0.8f)
                path.lineTo(w * 0.2f, h * 0.8f)
                path.close()
            }
            "Hexagon" -> {
                for (i in 0 until 6) {
                    val angle = Math.toRadians(60.0 * i)
                    val x = (w/2 + (w/2.5 * cos(angle))).toFloat()
                    val y = (h/2 + (h/2.5 * sin(angle))).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            "Pentagon" -> {
                for (i in 0 until 5) {
                    val angle = Math.toRadians(72.0 * i - 90.0)
                    val x = (w/2 + (w/2.5 * cos(angle))).toFloat()
                    val y = (h/2 + (h/2.5 * sin(angle))).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            "Diamond" -> {
                path.moveTo(w/2, h * 0.1f)
                path.lineTo(w * 0.9f, h/2)
                path.lineTo(w/2, h * 0.9f)
                path.lineTo(w * 0.1f, h/2)
                path.close()
            }
            "Moon" -> {
                for (i in -90..90 step 5) {
                    val angle = Math.toRadians(i.toDouble())
                    val x = (w/2 + (w/2.5 * cos(angle))).toFloat()
                    val y = (h/2 + (h/2.5 * sin(angle))).toFloat()
                    if (i == -90) path.moveTo(x, y) else path.lineTo(x, y)
                }
                for (i in 90 downTo -90 step 5) {
                    val angle = Math.toRadians(i.toDouble())
                    val x = (w * 0.65f + (w * 0.3 * cos(angle))).toFloat()
                    val y = (h/2 + (h/2.5 * sin(angle))).toFloat()
                    path.lineTo(x, y)
                }
                path.close()
            }
            "Cat" -> {
                path.moveTo(w * 0.35f, h * 0.85f)
                path.lineTo(w * 0.65f, h * 0.85f)
                path.lineTo(w * 0.75f, h * 0.70f)
                path.lineTo(w * 0.70f, h * 0.45f)
                path.lineTo(w * 0.80f, h * 0.25f)
                path.lineTo(w * 0.65f, h * 0.35f)
                path.lineTo(w * 0.50f, h * 0.40f)
                path.lineTo(w * 0.35f, h * 0.35f)
                path.lineTo(w * 0.20f, h * 0.25f)
                path.lineTo(w * 0.30f, h * 0.45f)
                path.lineTo(w * 0.25f, h * 0.70f)
                path.close()
            }
            "Bird" -> {
                path.moveTo(w * 0.10f, h * 0.40f)
                path.lineTo(w * 0.30f, h * 0.35f)
                path.lineTo(w * 0.45f, h * 0.45f)
                path.lineTo(w * 0.55f, h * 0.40f)
                path.lineTo(w * 0.60f, h * 0.35f)
                path.lineTo(w * 0.55f, h * 0.45f)
                path.lineTo(w * 0.70f, h * 0.35f)
                path.lineTo(w * 0.90f, h * 0.40f)
                path.lineTo(w * 0.70f, h * 0.55f)
                path.lineTo(w * 0.50f, h * 0.65f)
                path.lineTo(w * 0.30f, h * 0.55f)
                path.close()
            }
            "Butterfly" -> {
                path.moveTo(w * 0.50f, h * 0.40f)
                path.lineTo(w * 0.50f, h * 0.70f)
                path.lineTo(w * 0.65f, h * 0.85f)
                path.lineTo(w * 0.85f, h * 0.65f)
                path.lineTo(w * 0.75f, h * 0.55f)
                path.lineTo(w * 0.90f, h * 0.35f)
                path.lineTo(w * 0.70f, h * 0.15f)
                path.lineTo(w * 0.50f, h * 0.40f)
                path.lineTo(w * 0.30f, h * 0.15f)
                path.lineTo(w * 0.10f, h * 0.35f)
                path.lineTo(w * 0.25f, h * 0.55f)
                path.lineTo(w * 0.15f, h * 0.65f)
                path.lineTo(w * 0.35f, h * 0.85f)
                path.close()
            }
            "Fish" -> {
                path.moveTo(w * 0.10f, h * 0.35f)
                path.lineTo(w * 0.10f, h * 0.65f)
                path.lineTo(w * 0.30f, h * 0.50f)
                path.lineTo(w * 0.50f, h * 0.30f)
                path.lineTo(w * 0.80f, h * 0.35f)
                path.lineTo(w * 0.95f, h * 0.50f)
                path.lineTo(w * 0.80f, h * 0.65f)
                path.lineTo(w * 0.50f, h * 0.70f)
                path.lineTo(w * 0.30f, h * 0.50f)
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
                style = Stroke(width = 4f),
                color = Color(0xFFFFC107)
            )
        }
    }
}
