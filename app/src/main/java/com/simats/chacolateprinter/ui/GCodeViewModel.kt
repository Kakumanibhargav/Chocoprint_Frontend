package com.simats.chacolateprinter.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.chacolateprinter.PrinterParameters
import com.simats.chacolateprinter.utils.GCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GCodeViewModel : ViewModel() {

    private val _gCode = MutableStateFlow("")
    val gCode = _gCode.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _maxLayers = MutableStateFlow(0)
    val maxLayers = _maxLayers.asStateFlow()

    fun generateGCode(designName: String, designUri: Uri?, printMode: String, printerParameters: PrinterParameters) {
        viewModelScope.launch {
            _isGenerating.value = true
            val result = withContext(Dispatchers.IO) {
                GCodeGenerator.generate(
                    designName = designName,
                    imageUri = designUri,
                    printMode = printMode,
                    parameters = printerParameters
                )
            }
            _gCode.value = result.gCode
            _maxLayers.value = result.layerCount
            _isGenerating.value = false
        }
    }

    fun updateGCode(code: String, layers: Int) {
        _gCode.value = code
        _maxLayers.value = layers
    }

    fun generateMultiColorGCode(
        shapeName: String,
        numColors: Int,
        baseX: Float,
        baseY: Float,
        baseZ: Float,
        incrementXY: Float,
        mode: String,
        printerParameters: PrinterParameters
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            val result = withContext(Dispatchers.IO) {
                GCodeGenerator.generateMultiColorGCode(
                    shapeName, numColors, baseX, baseY, baseZ, incrementXY, mode, printerParameters
                )
            }
            _gCode.value = result.gCode
            _maxLayers.value = result.layerCount
            _isGenerating.value = false
        }
    }
}
