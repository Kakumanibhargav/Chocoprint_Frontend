package com.simats.chacolateprinter.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.simats.chacolateprinter.models.Point
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max

object ImageUtils {

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.ARGB_8888 
                inSampleSize = 1 
            }
            val original = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            // Reduced to 200 for maximum speed while still being viable for chocolate printing
            original?.let { resizeBitmap(it, 200) } 
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun generateBorderPath(bitmap: Bitmap): List<List<Point>> {
        val width = bitmap.width
        val height = bitmap.height
        val visited = BooleanArray(width * height)
        val paths = mutableListOf<List<Point>>()

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val binaryBitmap = BooleanArray(width * height) { i ->
            val color = pixels[i]
            val a = (color shr 24) and 0xFF
            if (a < 50) return@BooleanArray false
            
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            
            // Optimized luminance
            (r * 30 + g * 59 + b * 11) < 15000
        }

        for (y in 1 until height - 1) {
            val offset = y * width
            for (x in 1 until width - 1) {
                val index = offset + x
                if (binaryBitmap[index] && !visited[index]) {
                    // Check if edge
                    if (!binaryBitmap[index - 1] || !binaryBitmap[index + 1] || 
                        !binaryBitmap[index - width] || !binaryBitmap[index + width]) {
                        val contour = traceContour(binaryBitmap, width, height, x, y, visited)
                        if (contour.size > 5) { 
                            val simplified = simplifyPath(contour, 1.0f) 
                            if (simplified.size > 2) paths.add(simplified)
                        }
                    }
                }
            }
        }
        return paths
    }
    
    private fun traceContour(pixels: BooleanArray, w: Int, h: Int, startX: Int, startY: Int, visited: BooleanArray): List<Point> {
        val contour = mutableListOf<Point>()
        var cx = startX
        var cy = startY
        var bx = startX - 1 
        var by = startY
        
        var iterations = 0
        val maxIterations = 5000 // Hard cap for speed
        
        while (iterations < maxIterations) {
            val res = findNextNeighborFast(pixels, w, h, cx, cy, bx, by)
            if (res[0] == -1) break
            
            val nx = res[0]
            val ny = res[1]
            visited[ny * w + nx] = true
            contour.add(Point(nx.toFloat(), ny.toFloat()))
            
            bx = res[2]
            by = res[3]
            cx = nx
            cy = ny
            
            if (cx == startX && cy == startY) break
            iterations++
        }
        return contour
    }

    private fun findNextNeighborFast(pixels: BooleanArray, w: Int, h: Int, cx: Int, cy: Int, bx: Int, by: Int): IntArray {
        val dx = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        val dy = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
        
        var startIndex = -1
        for (i in 0..7) {
            if (bx == cx + dx[i] && by == cy + dy[i]) { startIndex = i; break }
        }
        if (startIndex == -1) startIndex = 6 
        
        for (i in 0..7) {
            val idx = (startIndex + 1 + i) % 8
            val nx = cx + dx[idx]
            val ny = cy + dy[idx]
            if (nx in 0 until w && ny in 0 until h && pixels[ny * w + nx]) {
                return intArrayOf(nx, ny, cx + dx[(idx + 7) % 8], cy + dy[(idx + 7) % 8])
            }
        }
        return intArrayOf(-1, -1, -1, -1)
    }

    fun generateFillPath(bitmap: Bitmap, params: com.simats.chacolateprinter.PrinterParameters): List<List<Point>> {
        val width = bitmap.width
        val height = bitmap.height
        
        val allPaths = generateBorderPath(bitmap).toMutableList()
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val binaryBitmap = BooleanArray(width * height) { i ->
            val color = pixels[i]
            val a = (color shr 24) and 0xFF
            if (a < 50) return@BooleanArray false
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            (r * 30 + g * 59 + b * 11) < 15000
        }

        val nozzleDia = params.nozzleDiameter.toFloatOrNull() ?: 0.8f
        val infillDensity = (params.infillDensity / 100f).coerceIn(0.05f, 1.0f)
        val mmPerPixel = (params.xMax.toFloatOrNull() ?: 200f) / width
        
        val step = ((nozzleDia / mmPerPixel) / infillDensity).toInt().coerceAtLeast(2)
        
        var reverse = false
        for (y in 0 until height step step) {
            val offset = y * width
            var x = 0
            while (x < width) {
                if (binaryBitmap[offset + x]) {
                    val startX = x
                    while (x < width && binaryBitmap[offset + x]) x++
                    if (x - startX > 2) {
                        val seg = listOf(Point(startX.toFloat(), y.toFloat()), Point((x - 1).toFloat(), y.toFloat()))
                        allPaths.add(if (reverse) seg.reversed() else seg)
                        reverse = !reverse
                    }
                } else x++
            }
        }
        return allPaths
    }

    fun simplifyPath(points: List<Point>, epsilon: Float): List<Point> {
        if (points.size < 4) return points
        val result = mutableListOf<Point>()
        rdpStep(points, 0, points.size - 1, epsilon, result)
        result.add(points.last())
        return result
    }

    private fun rdpStep(points: List<Point>, first: Int, last: Int, epsilon: Float, result: MutableList<Point>) {
        var dmax = 0f
        var index = 0
        for (i in first + 1 until last) {
            val d = perpendicularDistance(points[i], points[first], points[last])
            if (d > dmax) {
                index = i
                dmax = d
            }
        }
        if (dmax > epsilon) {
            rdpStep(points, first, index, epsilon, result)
            rdpStep(points, index, last, epsilon, result)
        } else {
            result.add(points[first])
        }
    }

    private fun perpendicularDistance(p: Point, a: Point, b: Point): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        if (dx == 0f && dy == 0f) return p.dist(a)
        val num = abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x)
        val den = kotlin.math.sqrt(dx * dx + dy * dy)
        return num / den
    }
}
