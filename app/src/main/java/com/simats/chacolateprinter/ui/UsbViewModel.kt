package com.simats.chacolateprinter.ui

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class UsbViewModel(application: Application) : AndroidViewModel(application), SerialInputOutputManager.Listener {

    private val usbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager
    private val ACTION_USB_PERMISSION = "com.simats.chacolateprinter.USB_PERMISSION"

    private val _availableDevices = MutableStateFlow<List<UsbDevice>>(emptyList())
    val availableDevices = _availableDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<UsbDevice?>(null)
    val connectedDevice = _connectedDevice.asStateFlow()

    private val _responses = MutableSharedFlow<String>(extraBufferCapacity = 128)
    val responses = _responses.asSharedFlow()

    private var serialPort: UsbSerialPort? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var ioManager: SerialInputOutputManager? = null
    
    private var buffer = StringBuilder()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { connect(it) }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                if (device?.deviceName == _connectedDevice.value?.deviceName) {
                    disconnect()
                }
            }
        }
    }

    init {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        ContextCompat.registerReceiver(application, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun scanDevices() {
        _availableDevices.value = usbManager.deviceList.values.toList()
    }

    fun requestPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        val permissionIntent = PendingIntent.getBroadcast(
            getApplication(), 0, Intent(ACTION_USB_PERMISSION), flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun connect(device: UsbDevice) {
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: return
        val connection = usbManager.openDevice(driver.device) ?: return
        
        val port = driver.ports[0]
        try {
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            
            serialPort = port
            usbConnection = connection
            _connectedDevice.value = device
            
            buffer.clear()
            ioManager = SerialInputOutputManager(port, this)
            ioManager?.start()
            
        } catch (e: IOException) {
            disconnect()
        }
    }

    fun sendGCode(gcode: String) {
        serialPort?.let {
            try {
                val data = (gcode.trim() + "\n").toByteArray()
                it.write(data, 2000)
            } catch (e: IOException) {
                disconnect()
            }
        }
    }

    override fun onNewData(data: ByteArray) {
        val message = String(data)
        buffer.append(message)
        
        while (buffer.contains("\n")) {
            val index = buffer.indexOf("\n")
            val line = buffer.substring(0, index).trim()
            buffer.delete(0, index + 1)
            
            if (line.isNotEmpty()) {
                viewModelScope.launch {
                    _responses.emit(line)
                }
            }
        }
    }

    override fun onRunError(e: Exception) {
        disconnect()
    }

    fun disconnect() {
        ioManager?.stop()
        ioManager = null
        try { serialPort?.close() } catch (e: Exception) {}
        serialPort = null
        usbConnection?.close()
        usbConnection = null
        _connectedDevice.value = null
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
        getApplication<Application>().unregisterReceiver(usbReceiver)
    }
}
