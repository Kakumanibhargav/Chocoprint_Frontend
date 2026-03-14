package com.simats.chacolateprinter.ui

import android.bluetooth.BluetoothDevice
import android.net.wifi.WifiInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    user: String,
    onNewPrintClick: () -> Unit,
    onMultiColorDashboardClick: () -> Unit,
    onConnectClick: () -> Unit,
    onWifiConnectClick: () -> Unit,
    onUsbConnectClick: () -> Unit,
    onLogoutClick: () -> Unit,
    connectedDeviceName: String,
    isConnected: Boolean,
    bluetoothViewModel: BluetoothViewModel,
    wifiViewModel: WifiViewModel
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF3E2723), Color(0xFF1B0000))
    )

    var selectedConnection by remember { mutableStateOf("WiFi") }
    var jogStep by remember { mutableFloatStateOf(1.0f) }
    var selectedCoordinateSystem by remember { mutableStateOf("G54") }

    // Live positions from ViewModels
    val mPosBluetooth by bluetoothViewModel.mPos.collectAsState()
    val wPosBluetooth by bluetoothViewModel.wPos.collectAsState()
    val mPosWifi by wifiViewModel.mPos.collectAsState()
    val wPosWifi by wifiViewModel.wPos.collectAsState()

    // Determine current positions based on active connection
    val currentMPos = if (connectedDeviceName.contains("BT", ignoreCase = true) || !connectedDeviceName.contains("WiFi", ignoreCase = true)) mPosBluetooth else mPosWifi
    val currentWPos = if (connectedDeviceName.contains("BT", ignoreCase = true) || !connectedDeviceName.contains("WiFi", ignoreCase = true)) wPosBluetooth else wPosWifi

    val onJog: (Map<String, Float>) -> Unit = {
        if (isConnected) {
            bluetoothViewModel.jog(it)
            wifiViewModel.jog(it)
        }
    }

    val onHome: (String?) -> Unit = { axis ->
        if (isConnected) {
            bluetoothViewModel.home(axis)
            wifiViewModel.home(axis)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome, $user",
                    color = Color(0xFFA1887F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "ChocoPrint 3D",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
            
            ConnectionStatusIndicator(deviceName = connectedDeviceName, isConnected = isConnected)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // MACHINE CONTROL CARD - Updated to Professional UI Style
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1E1B)),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsSuggest,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MACHINE CONTROL",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Position Displays
                Text("WORK POSITION (WPOS)", color = Color(0xFFA1887F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PositionItem("X", currentWPos.x, modifier = Modifier.weight(1f))
                    PositionItem("Y", currentWPos.y, modifier = Modifier.weight(1f))
                    PositionItem("Z", currentWPos.z, modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("MACHINE POSITION (MPOS)", color = Color(0xFFA1887F).copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PositionItem("X", currentMPos.x, isMachine = true, modifier = Modifier.weight(1f))
                    PositionItem("Y", currentMPos.y, isMachine = true, modifier = Modifier.weight(1f))
                    PositionItem("Z", currentMPos.z, isMachine = true, modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Coordinate Systems
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("G54", "G55", "G56", "G57").forEach { g ->
                        CoordinateSystemButton(
                            label = g,
                            isSelected = selectedCoordinateSystem == g,
                            onClick = { 
                                selectedCoordinateSystem = g
                                if (isConnected) {
                                    bluetoothViewModel.setWorkCoordinateSystem(g)
                                    wifiViewModel.setWorkCoordinateSystem(g)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Jog Control Section
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Jog Step Selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("JOG STEP", color = Color(0xFFA1887F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { 
                                    jogStep = when(jogStep) {
                                        0.1f -> 1.0f
                                        1.0f -> 10.0f
                                        10.0f -> 100.0f
                                        else -> 0.1f
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "$jogStep mm", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // X-Y Navigation
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row {
                            JogButton(Icons.Default.NorthWest) { onJog(mapOf("X" to -jogStep, "Y" to jogStep)) }
                            JogButton(Icons.Default.ArrowUpward) { onJog(mapOf("Y" to jogStep)) }
                            JogButton(Icons.Default.NorthEast) { onJog(mapOf("X" to jogStep, "Y" to jogStep)) }
                        }
                        Row {
                            JogButton(Icons.Default.ArrowBack) { onJog(mapOf("X" to -jogStep)) }
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC107))
                                    .clickable { onHome(null) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = "Home All", tint = Color.Black, modifier = Modifier.size(22.dp))
                            }
                            JogButton(Icons.Default.ArrowForward) { onJog(mapOf("X" to jogStep)) }
                        }
                        Row {
                            JogButton(Icons.Default.SouthWest) { onJog(mapOf("X" to -jogStep, "Y" to -jogStep)) }
                            JogButton(Icons.Default.ArrowDownward) { onJog(mapOf("Y" to -jogStep)) }
                            JogButton(Icons.Default.SouthEast) { onJog(mapOf("X" to jogStep, "Y" to -jogStep)) }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Z Navigation
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        JogButton(Icons.Default.KeyboardDoubleArrowUp) { onJog(mapOf("Z" to jogStep)) }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Z AXIS", color = Color(0xFFA1887F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        JogButton(Icons.Default.KeyboardDoubleArrowDown) { onJog(mapOf("Z" to -jogStep)) }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Axis Homing
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeButton("X HOME", modifier = Modifier.weight(1f)) { onHome("X") }
                    HomeButton("Y HOME", modifier = Modifier.weight(1f)) { onHome("Y") }
                    HomeButton("Z HOME", modifier = Modifier.weight(1f)) { onHome("Z") }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        MainActionCard(title = "New Creation", subtitle = "Upload and print custom designs", icon = Icons.Default.Upload, color = Color(0xFFFFC107), onClick = onNewPrintClick)
        Spacer(modifier = Modifier.height(16.dp))
        MainActionCard(title = "Multi-Color Art", subtitle = "Design layered chocolate masterpieces", icon = Icons.Default.ColorLens, color = Color(0xFF8D6E63), onClick = onMultiColorDashboardClick)
        
        Spacer(modifier = Modifier.height(24.dp))

        // Connections
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ConnectionCard(icon = Icons.Default.Bluetooth, label = "Bluetooth", isSelected = selectedConnection == "Bluetooth", onClick = { selectedConnection = "Bluetooth"; onConnectClick() }, modifier = Modifier.weight(1f))
            ConnectionCard(icon = Icons.Default.Wifi, label = "WiFi", isSelected = selectedConnection == "WiFi", onClick = { selectedConnection = "WiFi"; onWifiConnectClick() }, modifier = Modifier.weight(1f))
            ConnectionCard(icon = Icons.Default.Usb, label = "USB", isSelected = selectedConnection == "USB", onClick = { selectedConnection = "USB"; onUsbConnectClick() }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun HomeButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFC107)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PositionItem(label: String, value: Float, isMachine: Boolean = false, modifier: Modifier = Modifier) {
    val bgColor = if (isMachine) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.05f)
    val textColor = if (isMachine) Color.White.copy(alpha = 0.4f) else Color.White
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = if (isMachine) Color.Gray else Color(0xFFFFC107), fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(text = String.format("%.2f", value), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun JogButton(icon: ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .size(44.dp)
            .background(Color.White.copy(alpha = 0.05f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun CoordinateSystemButton(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFFFC107).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFC107) else Color.White.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label, 
                color = if (isSelected) Color(0xFFFFC107) else Color.Gray, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ConnectionStatusIndicator(deviceName: String, isConnected: Boolean) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = if (isConnected) deviceName else "Not Connected",
            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFE57373),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = if (isConnected) "Active Connection" else "Offline",
            color = Color(0xFFA1887F),
            fontSize = 11.sp
        )
    }
}

@Composable
fun ConnectionCard(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = if (isSelected) Color(0xFF3E2723) else Color(0xFF2E1F1C)
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFFFFC107) else Color(0xFF5D4037)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = label, tint = if (isSelected) Color.White else Color(0xFFA1887F), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, color = if (isSelected) Color.White else Color(0xFFA1887F), fontSize = 12.sp)
        }
    }
}

@Composable
fun MainActionCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1F1C)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = subtitle, color = Color(0xFFA1887F), fontSize = 12.sp)
            }
        }
    }
}
