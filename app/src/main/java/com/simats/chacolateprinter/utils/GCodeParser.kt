package com.simats.chacolateprinter.utils

import android.util.Log
import com.simats.chacolateprinter.ui.Position
import kotlin.math.sqrt

data class GCodeCommand(
    val originalLine: String,
    val command: String,
    val x: Float?,
    val y: Float?,
    val z: Float?,
    val e: Float?,
    val f: Float?,
    val p: Long? = null,
    var cumulativeDist: Float = 0f,
    var processNum: Int = 1,
    var layerNum: Int = 1,
    var isExtruding: Boolean = false
)

object GCodeParser {
    
    data class PrintStats(
        val timeSeconds: Long,
        val materialLengthMm: Float,
        val materialWeightGrams: Float,
        val totalPathDist: Float
    )

    fun parse(gcode: String): List<GCodeCommand> {
        val commands = mutableListOf<GCodeCommand>()
        var currentX = 0f; var currentY = 0f; var currentZ = 0f
        var totalDist = 0f
        var currentProcess = 1
        var currentLayer = 1
        var extruding = false

        gcode.lineSequence().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach
            
            // Detection for Multi-Color Process / Layer
            if (trimmedLine.startsWith(";")) {
                if (trimmedLine.contains("Process") || trimmedLine.contains("Layer")) {
                    val pNum = trimmedLine.substringAfter("Process").substringAfter("Layer").trim().takeWhile { it.isDigit() }.toIntOrNull()
                    if (pNum != null) {
                        if (trimmedLine.contains("Process")) currentProcess = pNum
                        if (trimmedLine.contains("Layer")) currentLayer = pNum
                    }
                }
                commands.add(GCodeCommand(line, "", null, null, null, null, null, null, totalDist, currentProcess, currentLayer, extruding))
                return@forEach
            }
            
            val parts = trimmedLine.split(";")[0].trim()
            if (parts.isNotEmpty()) {
                val commandParts = parts.split("\\s+".toRegex())
                val command = commandParts[0].uppercase()
                var x: Float? = null; var y: Float? = null; var z: Float? = null
                var e: Float? = null; var f: Float? = null; var p: Long? = null

                commandParts.forEach {
                    if (it.length > 1) {
                        try {
                            when (it[0].uppercaseChar()) {
                                'X' -> x = it.substring(1).toFloatOrNull()
                                'Y' -> y = it.substring(1).toFloatOrNull()
                                'Z' -> z = it.substring(1).toFloatOrNull()
                                'E' -> e = it.substring(1).toFloatOrNull()
                                'F' -> f = it.substring(1).toFloatOrNull()
                                'P' -> p = it.substring(1).toLongOrNull()
                            }
                        } catch (ex: Exception) { }
                    }
                }
                
                if (command == "M3") extruding = true
                if (command == "M5") extruding = false
                
                if (command == "G0" || command == "G1") {
                    val nextX = x ?: currentX
                    val nextY = y ?: currentY
                    val nextZ = z ?: currentZ
                    
                    val d = sqrt((nextX - currentX) * (nextX - currentX) + 
                                 (nextY - currentY) * (nextY - currentY) + 
                                 (nextZ - currentZ) * (nextZ - currentZ))
                    totalDist += d
                    
                    currentX = nextX; currentY = nextY; currentZ = nextZ
                    
                    val isG1 = command == "G1"
                    val hasE = e != null && e!! > 0.0001f
                    val lineExtruding = extruding || (isG1 && hasE) || (isG1 && !hasE && extruding)
                    
                    commands.add(GCodeCommand(line, command, x, y, z, e, f, p, totalDist, currentProcess, currentLayer, lineExtruding))
                } else {
                    commands.add(GCodeCommand(line, command, x, y, z, e, f, p, totalDist, currentProcess, currentLayer, extruding))
                }
            } else {
                commands.add(GCodeCommand(line, "", null, null, null, null, null, null, totalDist, currentProcess, currentLayer, extruding))
            }
        }
        return commands
    }

    fun calculateStats(gcode: String, nozzleDia: Float = 0.8f, layerH: Float = 0.6f): PrintStats {
        var totalE = 0f
        var totalTime = 0f
        var currentX = 0f
        var currentY = 0f
        var currentZ = 0f
        var currentF = 1200f
        var lastDist = 0f
        var extruding = false

        gcode.lineSequence().forEach { line ->
            val trimmedLine = line.trim()
            val parts = trimmedLine.split(";")[0].trim()
            if (parts.isEmpty()) return@forEach
            
            val commandParts = parts.split("\\s+".toRegex())
            val command = commandParts[0].uppercase()
            
            if (command == "M3") extruding = true
            if (command == "M5") extruding = false
            
            if (command == "G0" || command == "G1") {
                var nx = currentX; var ny = currentY; var nz = currentZ; var ne: Float? = null
                commandParts.forEach {
                    if (it.length > 1) {
                        val v = it.substring(1).toFloatOrNull() ?: return@forEach
                        when (it[0].uppercaseChar()) {
                            'X' -> nx = v
                            'Y' -> ny = v
                            'Z' -> nz = v
                            'E' -> ne = v
                            'F' -> currentF = v
                        }
                    }
                }
                
                val dist = sqrt((nx - currentX) * (nx - currentX) + 
                                (ny - currentY) * (ny - currentY) + 
                                (nz - currentZ) * (nz - currentZ))
                
                if (currentF > 0) {
                    totalTime += dist / (currentF / 60f)
                }
                
                if (extruding || ne != null) {
                    // If E is provided use it, otherwise estimate based on distance if extruding
                    totalE += ne ?: (if (extruding) dist * 0.1f else 0f)
                }

                currentX = nx; currentY = ny; currentZ = nz
                lastDist += dist
            } else if (command == "G4") {
                commandParts.forEach {
                    if (it.startsWith("P") || it.startsWith("p")) {
                        totalTime += (it.substring(1).toLongOrNull() ?: 0L) / 1000f
                    }
                }
            }
        }

        val materialWeight = totalE * 0.12f 
        
        return PrintStats(
            timeSeconds = totalTime.toLong(),
            materialLengthMm = totalE,
            materialWeightGrams = materialWeight,
            totalPathDist = lastDist
        )
    }

    fun getProgressAt(commands: List<GCodeCommand>, pos: Position, lastSentIndex: Int): Float {
        if (commands.isEmpty()) return 0f
        val totalCount = commands.size.toFloat()
        return (lastSentIndex.toFloat() / totalCount).coerceIn(0f, 1f)
    }
}
