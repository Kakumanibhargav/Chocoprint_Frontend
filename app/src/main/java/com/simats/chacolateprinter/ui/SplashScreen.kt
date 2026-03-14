package com.simats.chacolateprinter.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // 3 Second Timer
    LaunchedEffect(key1 = true) {
        delay(3000)
        onSplashFinished()
    }

    // Gradient Background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF5D4037), // Light Chocolate
            Color(0xFF3E2723), // Dark Chocolate
            Color(0xFF1B0000)  // Almost Black
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated 3D Cube
            RotatingCube(modifier = Modifier.size(120.dp))

            Spacer(modifier = Modifier.height(32.dp))

            // Text: ChocoPrint 3D
            Text(
                text = "ChocoPrint 3D",
                color = Color(0xFFFFC107), // Amber/Gold
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Smart Chocolate Printer Controller",
                color = Color(0xFFFFECB3), // Light Amber
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Loading Dots
            LoadingDots()
        }
    }
}

@Composable
fun RotatingCube(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "CubeRotation")
    
    // Rotate around Y axis
    val angleY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AngleY"
    )

    // Rotate around X axis (slower)
    val angleX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AngleX"
    )

    Canvas(modifier = modifier) {
        val size = this.size.minDimension
        val center = Offset(size / 2, size / 2)
        val cubeSize = size * 0.4f // Scale of the cube relative to canvas

        // 8 Vertices of a cube
        // x, y, z in range -1 to 1
        val vertices = listOf(
            Triple(-1f, -1f, -1f), Triple(1f, -1f, -1f), Triple(1f, 1f, -1f), Triple(-1f, 1f, -1f), // Front/Back depending on rotation
            Triple(-1f, -1f, 1f), Triple(1f, -1f, 1f), Triple(1f, 1f, 1f), Triple(-1f, 1f, 1f)
        )

        // Rotation Matrices
        fun rotateX(y: Float, z: Float, angle: Float): Pair<Float, Float> {
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val newY = y * cos(rad) - z * sin(rad)
            val newZ = y * sin(rad) + z * cos(rad)
            return Pair(newY, newZ)
        }

        fun rotateY(x: Float, z: Float, angle: Float): Pair<Float, Float> {
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val newX = x * cos(rad) + z * sin(rad)
            val newZ = -x * sin(rad) + z * cos(rad)
            return Pair(newX, newZ)
        }
        
        // Define edges (indices into vertices list)
        val edges = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 0, // Face 1
            4 to 5, 5 to 6, 6 to 7, 7 to 4, // Face 2
            0 to 4, 1 to 5, 2 to 6, 3 to 7  // Connecting edges
        )

        // Draw Edges
        val path = Path()
        
        edges.forEach { (startIdx, endIdx) ->
            // Project Start Vertex
            var (sx, sy, sz) = vertices[startIdx]
            
            // Rotate X
            val (sy1, sz1) = rotateX(sy, sz, angleX)
            sy = sy1; sz = sz1
            
            // Rotate Y
            val (sx2, sz2) = rotateY(sx, sz, angleY)
            sx = sx2; sz = sz2

            // Simple Perspective Projection (orthogonal for now looks cleaner for icon)
            // To make it look 3D, we can just map x/y directly but Z order matters for faces. 
            // Since it's wireframe, we draw all edges.
            
            val startPoint = Offset(
                center.x + sx * cubeSize,
                center.y + sy * cubeSize
            )
            
             // Project End Vertex
            var (ex, ey, ez) = vertices[endIdx]
             // Rotate X
            val (ey1, ez1) = rotateX(ey, ez, angleX)
            ey = ey1; ez = ez1
            
            // Rotate Y
            val (ex2, ez2) = rotateY(ex, ez, angleY)
            ex = ex2; ez = ez2

            val endPoint = Offset(
                center.x + ex * cubeSize,
                center.y + ey * cubeSize
            )

            drawLine(
                color = Color(0xFFFFC107),
                start = startPoint,
                end = endPoint,
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }
        
        // Inner Cube (Smaller, rotated opposite optional, but let's keep it simple and clean as requested)
    }
}

@Composable
fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "DotsFade")

    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1200; 0.2f at 0; 1f at 300; 0.2f at 600 },
            repeatMode = RepeatMode.Restart
        ), label = "Alpha1"
    )
    
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1200; 0.2f at 300; 1f at 600; 0.2f at 900 },
            repeatMode = RepeatMode.Restart
        ), label = "Alpha2"
    )
    
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1200; 0.2f at 600; 1f at 900; 0.2f at 1200 },
            repeatMode = RepeatMode.Restart
        ), label = "Alpha3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Dot(alpha1)
        Spacer(modifier = Modifier.width(8.dp))
        Dot(alpha2)
        Spacer(modifier = Modifier.width(8.dp))
        Dot(alpha3)
    }
}

@Composable
fun Dot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(Color(0xFFFFC107).copy(alpha = alpha), shape = androidx.compose.foundation.shape.CircleShape)
    )
}
