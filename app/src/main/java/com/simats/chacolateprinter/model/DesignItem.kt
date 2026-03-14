package com.simats.chacolateprinter.model

import androidx.compose.ui.graphics.vector.ImageVector

data class DesignItem(
    val name: String,
    val icon: ImageVector? = null,
    val outlinedIcon: ImageVector? = null,
    val customShape: Boolean = false
)