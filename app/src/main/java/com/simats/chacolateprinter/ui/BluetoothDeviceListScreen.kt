package com.simats.chacolateprinter.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceListScreen(
    onDeviceSelected: (BluetoothDevice) -> Unit,
    viewModel: BluetoothViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startScan()
            viewModel.getPairedDevices()
        }
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            viewModel.startScan()
            viewModel.getPairedDevices()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    LaunchedEffect(viewModel.connectionError) {
        viewModel.connectionError.collectLatest { error ->
            snackbarHostState.showSnackbar(
                message = "Connection failed: $error",
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(viewModel.connectionSuccess) {
        viewModel.connectionSuccess.collectLatest {
            snackbarHostState.showSnackbar(
                message = "Connected successfully!",
                duration = SnackbarDuration.Short
            )
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3E2723), // Dark Chocolate
            Color(0xFF1B0000)  // Almost Black
        )
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Devices", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(brush = gradientBrush)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isConnecting) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFFC107))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Connecting...", color = Color.White)
                    }
                }
            }

            val currentConnectedDevice = connectedDevice
            if (currentConnectedDevice != null) {
                SectionTitle("Connected Device")
                DeviceCard(device = currentConnectedDevice, isConnected = true, isConnecting = false) {
                    viewModel.disconnect()
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isScanning && !isConnecting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFFFC107))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning...", color = Color.White)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pairedDevices.isNotEmpty()) {
                    item {
                        SectionTitle("Paired Devices")
                    }
                    items(pairedDevices) { device ->
                        if (device.address != connectedDevice?.address) {
                            DeviceCard(
                                device = device,
                                isConnected = false,
                                isConnecting = isConnecting,
                                onClick = { if (!isConnecting) onDeviceSelected(device) }
                            )
                        }
                    }
                }

                if (scannedDevices.isNotEmpty()) {
                    item {
                        SectionTitle("Available Devices")
                    }
                    items(scannedDevices) { device ->
                        if (device.address != connectedDevice?.address && pairedDevices.none { it.address == device.address }) {
                            DeviceCard(
                                device = device,
                                isConnected = false,
                                isConnecting = isConnecting,
                                onClick = { if (!isConnecting) onDeviceSelected(device) }
                            )
                        }
                    }
                }
            }
            
            if (!isScanning && !isConnecting && scannedDevices.isEmpty() && pairedDevices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No devices found", color = Color.Gray)
                        Button(
                            onClick = { viewModel.startScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                        ) {
                            Text("Retry Scan", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: BluetoothDevice, isConnected: Boolean, isConnecting: Boolean, onClick: () -> Unit) {
    val deviceName = try {
        device.name ?: "Unknown Device"
    } catch (e: SecurityException) {
        "Unknown Device"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF5D4037) else Color(0xFF2E1F1C)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = "Bluetooth Icon",
                tint = if (isConnected) Color.Green else Color(0xFFFFC107),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.address,
                    color = Color.Gray
                )
            }
            if (isConnected) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                ) {
                    Text("Disconnect", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFFFFC107),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
