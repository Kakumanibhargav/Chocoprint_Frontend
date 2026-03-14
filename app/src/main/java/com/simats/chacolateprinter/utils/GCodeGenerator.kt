package com.simats.chacolateprinter.utils

import android.net.Uri
import com.simats.chacolateprinter.PrinterParameters
import com.simats.chacolateprinter.models.Point
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.min
import kotlin.math.max

data class GCodeGenerationResult(val gCode: String, val layerCount: Int)

object GCodeGenerator {

    // Optimized float to string formatting to speed up G-code generation significantly
    private fun fastFmt2(value: Float, sb: StringBuilder) {
        if (value.isNaN() || value.isInfinite()) {
            sb.append("0.00")
            return
        }
        val v = if (value < 0) {
            sb.append('-')
            -value
        } else value
        
        val iPart = v.toInt()
        sb.append(iPart)
        sb.append('.')
        val fPart = ((v - iPart) * 100 + 0.5f).toInt()
        if (fPart >= 100) {
            // Very rare rounding case
            sb.setLength(sb.length - (iPart.toString().length + 1))
            sb.append(iPart + 1)
            sb.append(".00")
        } else {
            if (fPart < 10) sb.append('0')
            sb.append(fPart)
        }
    }

    private fun fastFmt0(value: Float, sb: StringBuilder) {
        sb.append(value.toInt())
    }

    fun generate(
        designName: String,
        imageUri: Uri?,
        printMode: String,
        parameters: PrinterParameters
    ): GCodeGenerationResult {
        return generateSampleGCode(designName, printMode, parameters)
    }

    fun generateGCode(
        paths: List<List<Point>>,
        mode: String,
        params: PrinterParameters,
        bedWidth: Float = 200f,
        bedHeight: Float = 200f
    ): GCodeGenerationResult {
        val layerH = (params.layerHeight.toFloatOrNull() ?: 0.6f).coerceAtLeast(0.1f)
        val printSpeed = (params.printSpeed.toFloatOrNull() ?: 20f)
        val travelSpeed = (params.travelSpeed.toFloatOrNull() ?: 60f)
        val zMax = params.zMax.toFloatOrNull() ?: 150f
        val xMax = params.xMax.toFloatOrNull() ?: 200f
        val yMax = params.yMax.toFloatOrNull() ?: 200f
        
        val layers = params.numLayers.toIntOrNull() ?: (zMax / layerH).toInt().coerceIn(1, 500)
        
        // Pre-allocate StringBuilder capacity to avoid repeated resizing
        val layerBodyBuilder = StringBuilder(paths.size * 50 + paths.sumOf { it.size } * 40)
        
        var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
        
        var hasPoints = false
        for (path in paths) {
            for (p in path) {
                hasPoints = true
                if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y
            }
        }
        
        if (!hasPoints) return GCodeGenerationResult("", 0)
        
        val width = maxX - minX; val height = maxY - minY
        
        val targetW = params.shapeWidth.toFloatOrNull() ?: (xMax - 40f)
        val targetH = params.shapeHeight.toFloatOrNull() ?: (yMax - 40f)
        val scale = min(if (width > 0) targetW / width else 1f, if (height > 0) targetH / height else 1f)
        
        val printF = printSpeed * 60; val travelF = travelSpeed * 60
        
        val sourceCenterX = (minX + maxX) / 2f
        val sourceCenterY = (minY + maxY) / 2f
        
        paths.forEach { path ->
            if (path.isEmpty()) return@forEach
            val startP = path[0]
            val sx = (startP.x - sourceCenterX) * scale
            val sy = (startP.y - sourceCenterY) * scale
            
            layerBodyBuilder.append("G0 X")
            fastFmt2(sx, layerBodyBuilder)
            layerBodyBuilder.append(" Y")
            fastFmt2(sy, layerBodyBuilder)
            layerBodyBuilder.append("\n")
            
            layerBodyBuilder.append("M3 S").append(params.servoAngle).append("; Start drawing\n")
            layerBodyBuilder.append("G4 P200\n")

            for (i in 1 until path.size) {
                val p = path[i]; val prevP = path[i-1]
                val x = (p.x - sourceCenterX) * scale
                val y = (p.y - sourceCenterY) * scale
                
                // Adaptive skip: skip movements that are too small to matter but keep accuracy for detail
                if (kotlin.math.abs(x - (prevP.x - sourceCenterX) * scale) < 0.02f && 
                    kotlin.math.abs(y - (prevP.y - sourceCenterY) * scale) < 0.02f) continue
                
                layerBodyBuilder.append("G1 X")
                fastFmt2(x, layerBodyBuilder)
                layerBodyBuilder.append(" Y")
                fastFmt2(y, layerBodyBuilder)
                layerBodyBuilder.append(" F")
                fastFmt0(printF, layerBodyBuilder)
                layerBodyBuilder.append("\n")
            }
            
            layerBodyBuilder.append("M5\n")
        }
        
        val layerBody = layerBodyBuilder.toString()
        val sb = StringBuilder(layerBody.length * layers + 2000)
        sb.append("G21 ; Set units to mm\nG90 ; Absolute positioning\nG28 ; Home\nM5\nG1 F")
        fastFmt0(travelF, sb)
        sb.append("\n")

        for (layer in 1..layers) {
            val z = layer * layerH
            sb.append("; Layer ").append(layer).append("\n")
            sb.append("G0 Z")
            fastFmt2(z, sb)
            sb.append("\n")
            sb.append(layerBody)
        }

        sb.append("G0 Z")
        fastFmt2(zMax, sb)
        sb.append("\nG28 X0 Y0\nG0 Z0\nM84\n")
        
        return GCodeGenerationResult(sb.toString(), layers)
    }

    fun generateMultiColorGCode(
        shapeName: String,
        numColors: Int,
        baseX: Float,
        baseY: Float,
        baseZ: Float,
        incrementXY: Float,
        mode: String,
        params: PrinterParameters
    ): GCodeGenerationResult {
        val sb = StringBuilder(10000)
        val printF = (params.printSpeed.toFloatOrNull() ?: 20f) * 60

        sb.append("; ===== Multi-Color G-code =====\n")
        sb.append("G21 ; Set units to mm\n")
        sb.append("G90 ; Absolute positioning\n")
        sb.append("G28 ; Home all axes\n")
        sb.append("M5 ; Tool OFF\n\n")

        var totalLayers = 0
        for (i in 0 until numColors) {
            val processNum = i + 1
            val currentTargetW = baseX + (i * incrementXY)
            val currentTargetH = baseY + (i * incrementXY)
            
            val rawPaths = generateSamplePaths(shapeName, mode != "Border", params)
            val filteredPaths = when (mode) {
                "Border" -> rawPaths.take(1)
                "Infill" -> rawPaths.drop(1)
                else -> rawPaths
            }
            
            val z = i * baseZ 
            totalLayers++
            
            sb.append("; Process ").append(processNum).append("\n")
            sb.append("; Layer 1\n")
            sb.append("M117 Color ").append(processNum).append("\n")
            sb.append("G0 Z")
            fastFmt2(z, sb)
            sb.append("\n")
            
            // Calculate scale and center
            var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
            for (path in filteredPaths) {
                for (p in path) {
                    if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x
                    if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y
                }
            }
            val width = maxX - minX; val height = maxY - minY
            val scale = min(if (width > 0) currentTargetW / width else 1f, if (height > 0) currentTargetH / height else 1f)
            val sourceCenterX = (minX + maxX) / 2f
            val sourceCenterY = (minY + maxY) / 2f

            filteredPaths.forEach { path ->
                if (path.isEmpty()) return@forEach
                val startP = path[0]
                val sx = (startP.x - sourceCenterX) * scale
                val sy = (startP.y - sourceCenterY) * scale
                
                sb.append("G0 X")
                fastFmt2(sx, sb)
                sb.append(" Y")
                fastFmt2(sy, sb)
                sb.append("\n")
                
                sb.append("M3 S").append(params.servoAngle).append("; Start drawing\n")
                sb.append("G4 P200\n")

                for (j in 1 until path.size) {
                    val p = path[j]
                    val x = (p.x - sourceCenterX) * scale
                    val y = (p.y - sourceCenterY) * scale
                    sb.append("G1 X")
                    fastFmt2(x, sb)
                    sb.append(" Y")
                    fastFmt2(y, sb)
                    sb.append(" F")
                    fastFmt0(printF, sb)
                    sb.append("\n")
                }
                sb.append("M5\n")
            }
            sb.append("\n")
        }

        sb.append("G0 Z")
        fastFmt2(params.zMax.toFloatOrNull() ?: 150f, sb)
        sb.append("\nG28 X0 Y0\nG0 Z0\nM5\n; ===== End =====\n")

        return GCodeGenerationResult(sb.toString(), totalLayers)
    }

    fun generateSamplePaths(name: String, fullFill: Boolean, params: PrinterParameters): List<List<Point>> {
        val paths = mutableListOf<List<Point>>()
        val borderPath = mutableListOf<Point>()

        when (name) {
            "Heart" -> {
                for (i in 0..360 step 2) {
                    val t = Math.toRadians(i.toDouble())
                    val x = 0.5f + 0.4f * (16 * sin(t).pow(3)) / 17
                    val y = 0.5f - 0.4f * (13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)) / 17
                    borderPath.add(Point(x.toFloat(), y.toFloat()))
                }
            }
            "Star" -> {
                val points = 5; val outerRadius = 0.45f; val innerRadius = 0.22f
                for (i in 0 until points * 2) {
                    val isOuter = i % 2 == 0
                    val radius = if (isOuter) outerRadius else innerRadius
                    val angle = (Math.PI * 2 / (points * 2)) * i - (Math.PI / 2)
                    borderPath.add(Point(0.5f + (radius * cos(angle)).toFloat(), 0.5f + (radius * sin(angle)).toFloat()))
                }
                borderPath.add(borderPath.first())
            }
            "Circle" -> {
                for (i in 0..360 step 5) {
                    val angle = Math.toRadians(i.toDouble())
                    borderPath.add(Point(0.5f + 0.4f * cos(angle).toFloat(), 0.5f + 0.4f * sin(angle).toFloat()))
                }
            }
            else -> {
                borderPath.add(Point(0.1f, 0.1f)); borderPath.add(Point(0.9f, 0.1f))
                borderPath.add(Point(0.9f, 0.9f)); borderPath.add(Point(0.1f, 0.9f))
                borderPath.add(Point(0.1f, 0.1f))
            }
        }
        paths.add(borderPath)

        if (fullFill) {
            val bounds = getBounds(borderPath)
            val nozzleDia = (params.nozzleDiameter.toFloatOrNull() ?: 0.8f).coerceAtLeast(0.1f)
            val infillDensity = (params.infillDensity / 100f).coerceIn(0.1f, 1.0f)
            val density = (nozzleDia / 100f) / infillDensity 

            var y = bounds.minY + density
            while (y < bounds.maxY) {
                val intersections = mutableListOf<Float>()
                for (i in 0 until borderPath.size - 1) {
                    val p1 = borderPath[i]; val p2 = borderPath[i+1]
                    if (y > min(p1.y, p2.y) && y <= max(p1.y, p2.y)) {
                         if (p2.y != p1.y) {
                            intersections.add(p1.x + (y - p1.y) * (p2.x - p1.x) / (p2.y - p1.y))
                         }
                    }
                }
                intersections.sort()
                for (i in 0 until intersections.size step 2) {
                    if (i + 1 < intersections.size) {
                        paths.add(listOf(Point(intersections[i], y), Point(intersections[i+1], y)))
                    }
                }
                y += density
            }
        }
        return paths
    }

    fun generateSampleGCode(shapeName: String, mode: String, params: PrinterParameters): GCodeGenerationResult {
        val fullFill = mode == "Full Fill" || mode == "Both"
        val paths = generateSamplePaths(shapeName, fullFill, params)
        return generateGCode(paths, mode, params)
    }
    
    private fun getBounds(path: List<Point>): Bounds {
        var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
        path.forEach { 
            if (it.x < minX) minX = it.x; if (it.x > maxX) maxX = it.x
            if (it.y < minY) minY = it.y; if (it.y > maxY) maxY = it.y
        }
        return Bounds(minX, maxX, minY, maxY)
    }
    private data class Bounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)
}
