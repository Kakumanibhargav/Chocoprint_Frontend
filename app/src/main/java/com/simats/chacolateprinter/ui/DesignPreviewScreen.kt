package com.simats.chacolateprinter.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DesignPreviewScreen(
    designName: String,
    imageUri: Uri? = null,
    onBackClick: () -> Unit,
    onProceedClick: () -> Unit
) {
    // Gradient Background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3E2723), // Dark Chocolate
            Color(0xFF1B0000)  // Almost Black
        )
    )

    // Load Bitmap if URI is present
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                    bitmap = BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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

        // Preview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1F1C)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5D4037)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Design Preview",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Large Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFF1B0F0D), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                   if (bitmap != null) {
                       Image(
                           bitmap = bitmap!!.asImageBitmap(),
                           contentDescription = "Uploaded Design",
                           modifier = Modifier.fillMaxSize().padding(16.dp)
                       )
                   } else {
                       PreviewShape(name = designName, size = 180.dp)
                   }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Metadata
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "File Type",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (imageUri != null) "Custom Upload" else "$designName Design",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (imageUri != null) imageUri.path ?: "Unknown file" else "sample/svg",
                            color = Color(0xFFA1887F),
                            fontSize = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Ready",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp).offset(x = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Path data ready for G-code generation",
                        color = Color(0xFFFFC107),
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action Buttons Row
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
                    text = "Choose\nDifferent\nDesign",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
            
            Button(
                onClick = onProceedClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFAB00),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Proceed\nto\nParameters",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PreviewShape(name: String, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val path = Path()
        val w = this.size.width
        val h = this.size.height
        
        val color = Color(0xFFFFAB00) 
        
        when (name) {
            "Heart" -> {
                path.moveTo(w * 0.5f, h * 0.85f)
                path.cubicTo(w*0.1f, h*0.5f, w*0.1f, h*0.1f, w*0.5f, h*0.3f)
                path.cubicTo(w*0.9f, h*0.1f, w*0.9f, h*0.5f, w*0.5f, h*0.85f)
                path.close()
            }
            "Star" -> {
                 path.moveTo(w * 0.5f, h * 0.05f)
                 path.lineTo(w * 0.65f, h * 0.35f)
                 path.lineTo(w * 1.0f, h * 0.35f)
                 path.lineTo(w * 0.75f, h * 0.55f)
                 path.lineTo(w * 0.85f, h * 0.9f)
                 path.lineTo(w * 0.5f, h * 0.7f)
                 path.lineTo(w * 0.15f, h * 0.9f)
                 path.lineTo(w * 0.25f, h * 0.55f)
                 path.lineTo(w * 0.0f, h * 0.35f)
                 path.lineTo(w * 0.35f, h * 0.35f)
                 path.close()
            }
            "Circle" -> {
                drawCircle(color, radius = w/2)
                return@Canvas
            }
            "Square" -> {
                drawRect(color)
                return@Canvas
            }
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
                path.moveTo(w * 0.3f, h * 0.3f)
                path.lineTo(w * 0.4f, h * 0.4f)
                path.lineTo(w * 0.6f, h * 0.4f)
                path.lineTo(w * 0.7f, h * 0.3f)
                path.lineTo(w * 0.8f, h * 0.5f)
                path.lineTo(w * 0.7f, h * 0.8f)
                path.lineTo(w * 0.3f, h * 0.8f)
                path.lineTo(w * 0.2f, h * 0.5f)
                path.close()
            }
            "Bird" -> {
                path.moveTo(w * 0.2f, h * 0.5f)
                path.lineTo(w * 0.4f, h * 0.4f)
                path.lineTo(w * 0.5f, h * 0.5f)
                path.lineTo(w * 0.6f, h * 0.4f)
                path.lineTo(w * 0.8f, h * 0.5f)
                path.lineTo(w * 0.5f, h * 0.6f)
                path.close()
            }
            "Butterfly" -> {
                path.moveTo(w * 0.5f, h * 0.5f)
                path.lineTo(w * 0.7f, h * 0.2f)
                path.lineTo(w * 0.9f, h * 0.5f)
                path.lineTo(w * 0.7f, h * 0.8f)
                path.lineTo(w * 0.5f, h * 0.6f)
                path.lineTo(w * 0.3f, h * 0.8f)
                path.lineTo(w * 0.1f, h * 0.5f)
                path.lineTo(w * 0.3f, h * 0.2f)
                path.close()
            }
            "Fish" -> {
                path.moveTo(w * 0.1f, h * 0.5f)
                path.lineTo(w * 0.3f, h * 0.4f)
                path.lineTo(w * 0.6f, h * 0.3f)
                path.lineTo(w * 0.9f, h * 0.5f)
                path.lineTo(w * 0.6f, h * 0.7f)
                path.lineTo(w * 0.3f, h * 0.6f)
                path.close()
            }
            else -> {
                drawCircle(color, radius = w/2)
                return@Canvas
            }
        }
        drawPath(path, color)
    }
}
