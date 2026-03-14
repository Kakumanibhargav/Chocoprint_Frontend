package com.simats.chacolateprinter.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.SupplicantState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.Socket

class WifiViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _scannedNetworks = MutableStateFlow<List<ScanResult>>(emptyList())
    val scannedNetworks = _scannedNetworks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _connectedNetwork = MutableStateFlow<WifiInfo?>(null)
    val connectedNetwork = _connectedNetwork.asStateFlow()

    // Position Tracking
    private val _mPos = MutableStateFlow<Position>(Position(0f, 0f, 0f))
    val mPos = _mPos.asStateFlow()

    private val _wPos = MutableStateFlow<Position>(Position(0f, 0f, 0f))
    val wPos = _wPos.asStateFlow()
    
    private var lastWCO = Position(0f, 0f, 0f)

    private val _responses = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val responses = _responses.asSharedFlow()

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var readerJob: Job? = null
    private var pollerJob: Job? = null

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) scanSuccess()
            _isScanning.value = false
        }
    }

    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) updateConnectedNetworkInfo()
        }
    }

    init {
        updateConnectedNetworkInfo()
        val intentFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        getApplication<Application>().registerReceiver(wifiStateReceiver, intentFilter)
    }

    fun startScan() {
        _isScanning.value = true
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        getApplication<Application>().registerReceiver(wifiScanReceiver, intentFilter)
        wifiManager.startScan()
    }

    private fun scanSuccess() {
        try { _scannedNetworks.value = wifiManager.scanResults } catch (e: SecurityException) { }
    }

    suspend fun sendGCode(gcode: String) {
        withContext(Dispatchers.IO) {
            try {
                val data = if (gcode.length == 1 && (gcode == "!" || gcode == "~" || gcode == "?" || gcode[0].toInt() == 0x18)) {
                    gcode.toByteArray() 
                } else {
                    (gcode.trim() + "\n").toByteArray()
                }
                outputStream?.write(data)
                outputStream?.flush()
            } catch (e: Exception) { 
                disconnect()
            }
        }
    }

    fun jog(axis: String, distance: Float) {
        jog(mapOf(axis to distance))
    }

    fun jog(moves: Map<String, Float>, feedrate: Float = 2000f) {
        CoroutineScope(Dispatchers.IO).launch {
            val moveCmd = moves.entries.joinToString(" ") { "${it.key.uppercase()}${it.value}" }
            // Pre-update UI
            moves.forEach { (axis, dist) ->
                when(axis.uppercase()) {
                    "X" -> _wPos.value = _wPos.value.copy(x = _wPos.value.x + dist)
                    "Y" -> _wPos.value = _wPos.value.copy(y = _wPos.value.y + dist)
                    "Z" -> _wPos.value = _wPos.value.copy(z = _wPos.value.z + dist)
                }
            }
            sendGCode("G91") 
            sendGCode("G1 $moveCmd F$feedrate") 
            sendGCode("G90")
        }
    }

    fun home(axis: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            if (axis == null) {
                sendGCode("G28")
                _mPos.value = Position(0f, 0f, 0f)
                _wPos.value = Position(0f, 0f, 0f)
            } else {
                sendGCode("G92 ${axis.uppercase()}0")
                when(axis.uppercase()) {
                    "X" -> _wPos.value = _wPos.value.copy(x = 0f)
                    "Y" -> _wPos.value = _wPos.value.copy(y = 0f)
                    "Z" -> _wPos.value = _wPos.value.copy(z = 0f)
                }
            }
        }
    }

    fun setWorkCoordinateSystem(system: String) {
        CoroutineScope(Dispatchers.IO).launch { sendGCode(system) }
    }

    fun pause() { 
        CoroutineScope(Dispatchers.IO).launch {
            sendGCode("!") 
            sendGCode("M0") 
        }
    }
    
    fun resume() { 
        CoroutineScope(Dispatchers.IO).launch {
            sendGCode("~")
            sendGCode("M108")
        }
    }
    
    fun stop() { 
        CoroutineScope(Dispatchers.IO).launch {
            sendGCode(0x18.toChar().toString()) 
            sendGCode("M112") 
            sendGCode("M5")
        }
    }

    fun disconnect() {
        readerJob?.cancel()
        pollerJob?.cancel()
        try { socket?.close() } catch (e: Exception) { }
        socket = null
        outputStream = null
        _connectedNetwork.value = null
    }

    private fun updateConnectedNetworkInfo() {
        val connectionInfo = wifiManager.connectionInfo
        if (connectionInfo != null && connectionInfo.supplicantState == SupplicantState.COMPLETED) {
            _connectedNetwork.value = connectionInfo
            connectToPrinter()
        } else {
            _connectedNetwork.value = null
        }
    }

    private fun connectToPrinter() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = Socket("192.168.1.100", 8080)
                outputStream = socket?.getOutputStream()
                startReading()
                startPolling()
            } catch (e: Exception) { }
        }
    }

    private fun startReading() {
        readerJob?.cancel()
        readerJob = viewModelScope.launch(Dispatchers.IO) {
            val inputStream = socket?.getInputStream() ?: return@launch
            val reader = inputStream.bufferedReader()
            try {
                while (true) {
                    val line = reader.readLine() ?: break
                    _responses.emit(line)
                    parsePrinterResponse(line)
                }
            } catch (e: Exception) {
                disconnect()
            }
        }
    }

    private fun startPolling() {
        pollerJob?.cancel()
        pollerJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (outputStream != null) {
                    sendGCode("?") 
                    sendGCode("M114")
                }
                delay(300)
            }
        }
    }

    private fun parsePrinterResponse(line: String) {
        val trimmed = line.trim()
        if (trimmed.startsWith("<") && trimmed.contains("|")) {
            val content = trimmed.substring(1, trimmed.length - 1)
            val parts = content.split("|")
            var mpos: Position? = null
            var wpos: Position? = null
            
            for (part in parts) {
                if (part.startsWith("WCO:")) {
                    val coords = part.substring(4).split(",")
                    if (coords.size >= 3) {
                        lastWCO = Position(coords[0].toFloatOrNull() ?: 0f, coords[1].toFloatOrNull() ?: 0f, coords[2].toFloatOrNull() ?: 0f)
                    }
                } else if (part.startsWith("WPos:")) {
                    val coords = part.substring(5).split(",")
                    if (coords.size >= 3) {
                        wpos = Position(coords[0].toFloatOrNull() ?: 0f, coords[1].toFloatOrNull() ?: 0f, coords[2].toFloatOrNull() ?: 0f)
                    }
                } else if (part.startsWith("MPos:")) {
                    val coords = part.substring(5).split(",")
                    if (coords.size >= 3) {
                        mpos = Position(coords[0].toFloatOrNull() ?: 0f, coords[1].toFloatOrNull() ?: 0f, coords[2].toFloatOrNull() ?: 0f)
                    }
                }
            }
            
            if (wpos != null) {
                _wPos.value = wpos
                _mPos.value = Position(wpos.x + lastWCO.x, wpos.y + lastWCO.y, wpos.z + lastWCO.z)
            } else if (mpos != null) {
                _mPos.value = mpos
                _wPos.value = Position(mpos.x - lastWCO.x, mpos.y - lastWCO.y, mpos.z - lastWCO.z)
            }
        } else if (trimmed.contains("X:") && trimmed.contains("Y:") && trimmed.contains("Z:")) {
            val parts = trimmed.split(" ")
            var nx = _wPos.value.x; var ny = _wPos.value.y; var nz = _wPos.value.z
            for (part in parts) {
                if (part.startsWith("X:")) nx = part.substring(2).toFloatOrNull() ?: nx
                if (part.startsWith("Y:")) ny = part.substring(2).toFloatOrNull() ?: ny
                if (part.startsWith("Z:")) nz = part.substring(2).toFloatOrNull() ?: nz
            }
            _wPos.value = Position(nx, ny, nz)
            _mPos.value = Position(nx, ny, nz)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(wifiScanReceiver)
            getApplication<Application>().unregisterReceiver(wifiStateReceiver)
        } catch (e: Exception) { }
        disconnect()
    }
}
