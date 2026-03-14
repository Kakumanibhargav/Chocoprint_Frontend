package com.simats.chacolateprinter.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiDeviceListScreen(
    onDeviceSelected: (ScanResult) -> Unit,
    viewModel: WifiViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current
    val scannedNetworks by viewModel.scannedNetworks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectedNetwork by viewModel.connectedNetwork.collectAsState()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.startScan()
        }
    }

    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            viewModel.startScan()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3E2723), // Dark Chocolate
            Color(0xFF1B0000)  // Almost Black
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
    ) {
        TopAppBar(
            title = { Text("WiFi Networks", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val currentConnectedNetwork = connectedNetwork
            if (currentConnectedNetwork != null) {
                DeviceCard(wifiInfo = currentConnectedNetwork, isConnected = true) {
                    viewModel.disconnect()
                }
            } else {
                if (isScanning) {
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
                    if (scannedNetworks.isNotEmpty()) {
                        item {
                            SectionTitle("Available Networks")
                        }
                        items(scannedNetworks) { network ->
                            DeviceCard(scanResult = network, isConnected = false, onClick = { onDeviceSelected(network) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(scanResult: ScanResult, isConnected: Boolean, onClick: () -> Unit) {
    DeviceCard(ssid = scanResult.SSID, bssid = scanResult.BSSID, isConnected = isConnected, onClick = onClick)
}

@Composable
fun DeviceCard(wifiInfo: WifiInfo, isConnected: Boolean, onClick: () -> Unit) {
    DeviceCard(ssid = wifiInfo.ssid.removeSurrounding("\""), bssid = wifiInfo.bssid, isConnected = isConnected, onClick = onClick)
}

@Composable
private fun DeviceCard(ssid: String, bssid: String, isConnected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnected, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF5D4037) else Color(0xFF2E1F1C)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.Wifi,
                contentDescription = "WiFi Icon",
                tint = if (isConnected) Color.Green else Color(0xFFFFC107),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ssid,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = bssid,
                    color = Color.Gray
                )
            }
            if (isConnected) {
                Button(onClick = onClick) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
