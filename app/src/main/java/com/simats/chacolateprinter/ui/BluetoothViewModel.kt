package com.simats.chacolateprinter.ui

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice

    private val _connectionError = MutableSharedFlow<String>()
    val connectionError = _connectionError.asSharedFlow()

    private val _connectionSuccess = MutableSharedFlow<Unit>()
    val connectionSuccess = _connectionSuccess.asSharedFlow()

    // Position Tracking
    private val _mPos = MutableStateFlow<Position>(Position(0f, 0f, 0f))
    val mPos: StateFlow<Position> = _mPos.asStateFlow()

    private val _wPos = MutableStateFlow<Position>(Position(0f, 0f, 0f))
    val wPos: StateFlow<Position> = _wPos.asStateFlow()
    
    private var lastWCO = Position(0f, 0f, 0f)

    private val _responses = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 128)
    val responses = _responses.asSharedFlow()

    private var bluetoothSocket: BluetoothSocket? = null
    private var readerJob: Job? = null
    private var pollerJob: Job? = null
    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action ?: return
            if (action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                device?.let { newDevice ->
                    if (_scannedDevices.value.none { it.address == newDevice.address }) {
                        _scannedDevices.value = _scannedDevices.value + newDevice
                    }
                }
            } else if (action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                _isScanning.value = false
            }
        }
    }

    fun getPairedDevices() {
        if (!hasConnectPermission()) return
        try {
            bluetoothAdapter?.bondedDevices?.let { _pairedDevices.value = it.toList() }
        } catch (e: SecurityException) {
            _pairedDevices.value = emptyList()
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun startScan() {
        if (!hasScanPermission()) return
        if (bluetoothAdapter == null) return
        
        stopScan()
        _scannedDevices.value = emptyList()
        
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            getApplication<Application>().registerReceiver(receiver, filter)
            isReceiverRegistered = true
            
            if (bluetoothAdapter?.startDiscovery() == true) {
                _isScanning.value = true
            } else {
                _isScanning.value = false
            }
        } catch (e: SecurityException) {
            _isScanning.value = false
        }
    }

    fun stopScan() {
        if (bluetoothAdapter?.isDiscovering == true) {
            try {
                bluetoothAdapter?.cancelDiscovery()
            } catch (e: SecurityException) {}
        }
        _isScanning.value = false
        if (isReceiverRegistered) {
            try {
                getApplication<Application>().unregisterReceiver(receiver)
            } catch (e: Exception) {}
            isReceiverRegistered = false
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (!hasConnectPermission()) return
        
        viewModelScope.launch {
            if (_isConnecting.value) return@launch
            _isConnecting.value = true
            stopScan()
            
            val result = viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Standard SPP UUID
                    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    var socket: BluetoothSocket? = null
                    
                    // Try different connection methods
                    val methods = listOf(
                        { device.createRfcommSocketToServiceRecord(uuid) },
                        { device.createInsecureRfcommSocketToServiceRecord(uuid) },
                        { 
                            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                            m.invoke(device, 1) as BluetoothSocket
                        }
                    )
                    
                    var lastException: Exception? = null
                    for (method in methods) {
                        try {
                            socket = method()
                            socket?.connect()
                            if (socket?.isConnected == true) break
                        } catch (e: Exception) {
                            lastException = e
                            try { socket?.close() } catch (ex: Exception) {}
                            socket = null
                        }
                    }
                    
                    if (socket != null && socket.isConnected) {
                        bluetoothSocket = socket
                        _connectedDevice.postValue(device)
                        startReading()
                        startPolling()
                        _connectionSuccess.emit(Unit)
                    } else {
                        _connectionError.emit(lastException?.message ?: "Failed to connect")
                        _connectedDevice.postValue(null)
                    }
                } catch (e: Exception) {
                    _connectionError.emit(e.message ?: "Unknown error")
                    _connectedDevice.postValue(null)
                } finally {
                    _isConnecting.value = false
                }
            }
        }
    }
    
    private fun <T> MutableStateFlow<T>.postValue(value: T) {
        viewModelScope.launch { this@postValue.value = value }
    }

    private fun startReading() {
        readerJob?.cancel()
        readerJob = viewModelScope.launch(Dispatchers.IO) {
            val inputStream = bluetoothSocket?.inputStream ?: return@launch
            val reader = inputStream.bufferedReader()
            try {
                while (true) {
                    val line = reader.readLine() ?: break
                    _responses.emit(line)
                    parsePrinterResponse(line)
                }
            } catch (e: IOException) {
                disconnect()
            }
        }
    }

    private fun startPolling() {
        pollerJob?.cancel()
        pollerJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (_connectedDevice.value != null && bluetoothSocket?.isConnected == true) {
                    sendGCode("?") // GRBL
                    sendGCode("M114") // Marlin
                }
                delay(1000)
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

    fun sendGCode(gcode: String) {
        bluetoothSocket?.let {
            try {
                if (!it.isConnected) {
                    disconnect()
                    return@let
                }
                val data = if (gcode.length == 1 && (gcode == "!" || gcode == "~" || gcode == "?" || gcode[0].toInt() == 0x18)) {
                    gcode.toByteArray()
                } else {
                    (gcode.trim() + "\n").toByteArray()
                }
                it.outputStream.write(data)
                it.outputStream.flush()
            } catch (e: IOException) {
                disconnect()
            }
        }
    }

    fun jog(axis: String, distance: Float) {
        jog(mapOf(axis to distance))
    }

    fun jog(moves: Map<String, Float>, feedrate: Float = 2000f) {
        val moveCmd = moves.entries.joinToString(" ") { "${it.key.uppercase()}${it.value}" }
        moves.forEach { (axis, dist) ->
            when(axis.uppercase()) {
                "X" -> {
                    _wPos.value = _wPos.value.copy(x = _wPos.value.x + dist)
                    _mPos.value = _mPos.value.copy(x = _mPos.value.x + dist)
                }
                "Y" -> {
                    _wPos.value = _wPos.value.copy(y = _wPos.value.y + dist)
                    _mPos.value = _mPos.value.copy(y = _mPos.value.y + dist)
                }
                "Z" -> {
                    _wPos.value = _wPos.value.copy(z = _wPos.value.z + dist)
                    _mPos.value = _mPos.value.copy(z = _mPos.value.z + dist)
                }
            }
        }
        sendGCode("G91")
        sendGCode("G1 $moveCmd F$feedrate") 
        sendGCode("G90")
    }

    fun home(axis: String?) {
        if (axis == null) {
            sendGCode("G28") // Machine Home
            _mPos.value = Position(0f, 0f, 0f)
            _wPos.value = Position(0f, 0f, 0f)
        } else {
            sendGCode("G92 ${axis.uppercase()}0") // Set Work Zero for axis
            when(axis.uppercase()) {
                "X" -> _wPos.value = _wPos.value.copy(x = 0f)
                "Y" -> _wPos.value = _wPos.value.copy(y = 0f)
                "Z" -> _wPos.value = _wPos.value.copy(z = 0f)
            }
        }
    }

    fun setWorkCoordinateSystem(system: String) { sendGCode(system) }
    
    fun pause() { 
        sendGCode("!")
        sendGCode("M0")
    }
    
    fun resume() { 
        sendGCode("~")
        sendGCode("M108")
    }
    
    fun stop() {
        sendGCode(0x18.toChar().toString())
        sendGCode("M112")
        sendGCode("M5")
    }

    fun disconnect() {
        readerJob?.cancel()
        pollerJob?.cancel()
        try { bluetoothSocket?.close() } catch (e: Exception) {}
        bluetoothSocket = null
        _connectedDevice.value = null
        _isConnecting.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
        disconnect()
    }
}
