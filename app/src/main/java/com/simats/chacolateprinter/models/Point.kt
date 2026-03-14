package com.simats.chacolateprinter.models

import kotlin.math.sqrt

data class Point(val x: Float, val y: Float, val z: Float = 0f) {
    fun dist(other: Point): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
