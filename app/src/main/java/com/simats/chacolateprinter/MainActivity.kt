package com.simats.chacolateprinter

import android.bluetooth.BluetoothDevice
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.simats.chacolateprinter.ui.*
import com.simats.chacolateprinter.ui.theme.ChacolatePrinterTheme
import com.simats.chacolateprinter.utils.GCodeParser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.sqrt

enum class AppState {
    Splash,
    SignIn,
    Home,
    SelectDesign,
    DesignPreview,
    Parameters,
    PrintMode,
    GCodeGeneration,
    Simulation,
    MultiColorConfig,
    MultiColorGCodeGeneration,
    MultiColorSimulation,
    LiveData,
    Operating,
    PrintConfirmation,
    MultiColorPrintVerification,
    SignUp,
    ForgotPassword,
    VerificationCode,
    ResetPassword,
    BluetoothDeviceList,
    WifiDeviceList
}

class MainActivity : ComponentActivity() {
    
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private val wifiViewModel: WifiViewModel by viewModels()
    private val usbViewModel: UsbViewModel by viewModels()
    private val gcodeViewModel: GCodeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChacolatePrinterTheme {
                var appState by remember { mutableStateOf(AppState.Splash) }
                val backStack = remember { mutableStateListOf(AppState.Splash) }

                val navigateTo: (AppState) -> Unit = { newState ->
                    if (newState != appState) {
                        if (newState == AppState.Home) {
                            backStack.clear()
                            backStack.add(newState)
                        } else if (!backStack.contains(newState)) {
                            backStack.add(newState)
                        }
                        appState = newState
                    }
                }

                val handleBackButton = {
                    if (backStack.size > 1) {
                        backStack.removeLast()
                        appState = backStack.last()
                    } else {
                        finish()
                    }
                }

                onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (appState == AppState.Home) {
                            finish()
                        } else {
                            handleBackButton()
                        }
                    }
                })

                var selectedDesignName by remember { mutableStateOf("") }
                var selectedDesignUri by remember { mutableStateOf<Uri?>(null) }
                var userEmailForReset by remember { mutableStateOf("") }
                
                var printerParameters by remember { mutableStateOf(PrinterParameters()) }
                var printMode by remember { mutableStateOf("Border Only") }
                val generatedGCode by gcodeViewModel.gCode.collectAsState()
                val maxLayers by gcodeViewModel.maxLayers.collectAsState()

                var isPrinting by remember { mutableStateOf(false) }
                var isPaused by remember { mutableStateOf(false) }
                var lastSentCommandIndex by remember { mutableIntStateOf(0) }
                var isMultiColorFlow by remember { mutableStateOf(false) }
                val consoleLogs = remember { mutableStateListOf<String>() }

                // Multi-Color Config State
                var multiColorShape by remember { mutableStateOf("Heart") }
                var multiColorNumColors by remember { mutableIntStateOf(4) }
                var multiColorBaseX by remember { mutableFloatStateOf(10f) }
                var multiColorBaseY by remember { mutableFloatStateOf(10f) }
                var multiColorBaseZ by remember { mutableFloatStateOf(5f) }
                var multiColorIncrementXY by remember { mutableFloatStateOf(2f) }
                var multiColorMode by remember { mutableStateOf("Both") }
                var multiColorSelectedColors by remember { mutableStateOf<List<Color>>(emptyList()) }

                // Hoisted Simulation State
                var simulationProgress by remember { mutableFloatStateOf(0f) }
                var isSimulationPlaying by remember { mutableStateOf(false) }
                var simulationPlaybackSpeed by remember { mutableFloatStateOf(1.0f) }

                // Connection States
                val connectedBT by bluetoothViewModel.connectedDevice.collectAsState()
                val connectedWiFi by wifiViewModel.connectedNetwork.collectAsState()
                val connectedUSB by usbViewModel.connectedDevice.collectAsState()
                val isConnected = connectedBT != null || connectedWiFi != null || connectedUSB != null
                val deviceName = when {
                    connectedBT != null -> connectedBT?.name ?: "Bluetooth Device"
                    connectedWiFi != null -> connectedWiFi?.ssid ?: "WiFi Machine"
                    connectedUSB != null -> "USB Machine"
                    else -> "None"
                }

                // G-code parsing
                val gcodeCommands = remember(generatedGCode) { 
                    if (generatedGCode.isBlank()) emptyList() else GCodeParser.parse(generatedGCode) 
                }

                // Improved Simulation background loop
                LaunchedEffect(isSimulationPlaying, simulationPlaybackSpeed, generatedGCode) {
                    if (isSimulationPlaying && generatedGCode.isNotBlank()) {
                        val commands = generatedGCode.lines().filter { it.isNotBlank() }
                        val totalCommands = commands.size
                        if (totalCommands > 0) {
                            var currentCommandIndex = (simulationProgress * totalCommands).toInt().coerceIn(0, totalCommands - 1)
                            val baseCommandsPerTick = 5 
                            while (isSimulationPlaying && currentCommandIndex < totalCommands) {
                                val commandsThisTick = (baseCommandsPerTick * simulationPlaybackSpeed).toInt().coerceAtLeast(1)
                                currentCommandIndex += commandsThisTick
                                simulationProgress = (currentCommandIndex.toFloat() / totalCommands).coerceAtMost(1f)
                                delay(33) 
                            }
                            if (simulationProgress >= 1f) isSimulationPlaying = false
                        }
                    }
                }

                // Print Progress Calculation
                val printProgress = if (gcodeCommands.isEmpty()) 0f else (lastSentCommandIndex.toFloat() / gcodeCommands.size).coerceIn(0f, 1f)

                // Printing & Transmission loop
                LaunchedEffect(isPrinting, isPaused, generatedGCode) {
                    if (isPrinting && !isPaused && generatedGCode.isNotBlank()) {
                        Log.d("PrintLoop", "Starting perfect line-by-line print loop.")
                        
                        val responses = merge(bluetoothViewModel.responses, wifiViewModel.responses, usbViewModel.responses)
                        val okChannel = Channel<String>(Channel.UNLIMITED)

                        // Persistent listener to avoid missing fast ACKs
                        val listenerJob = launch {
                            responses.collect { resp ->
                                val r = resp.trim().lowercase()
                                if (r == "ok" || r.startsWith("ok") || r.contains("ok ") || r.contains("done") || r.contains("finished")) {
                                    okChannel.trySend(resp)
                                }
                            }
                        }

                        try {
                            while (isPrinting && !isPaused && lastSentCommandIndex < gcodeCommands.size) {
                                val cmd = gcodeCommands[lastSentCommandIndex]
                                val line = cmd.originalLine
                                
                                if (line.trim().isEmpty() || line.startsWith(";")) {
                                    lastSentCommandIndex++
                                    continue
                                }

                                // Clear any stale OKs from channel before sending
                                while (!okChannel.isEmpty) { okChannel.tryReceive() }

                                Log.d("PrintLoop", "Sending [$lastSentCommandIndex]: $line")
                                consoleLogs.add(">> $line")
                                if (consoleLogs.size > 100) consoleLogs.removeAt(0)
                                
                                when {
                                    connectedBT != null -> bluetoothViewModel.sendGCode(line)
                                    connectedWiFi != null -> wifiViewModel.sendGCode(line)
                                    connectedUSB != null -> usbViewModel.sendGCode(line)
                                }
                                
                                val isSlowCmd = line.contains("G28") || line.contains("G29") || line.contains("M600") || line.contains("G4")
                                val timeoutMs = if (isSlowCmd) 60000L else 5000L 
                                
                                val okReceived = withTimeoutOrNull(timeoutMs) {
                                    okChannel.receive()
                                }

                                if (okReceived == null) {
                                    Log.w("PrintLoop", "No ACK at index $lastSentCommandIndex. Checking connectivity...")
                                    if (!isConnected) {
                                        consoleLogs.add("!! Connection Lost")
                                        isPrinting = false
                                        break
                                    }
                                } else {
                                    Log.d("PrintLoop", "Received ack: $okReceived")
                                    consoleLogs.add("<< $okReceived")
                                    if (consoleLogs.size > 100) consoleLogs.removeAt(0)
                                }

                                lastSentCommandIndex++
                                
                                if (lastSentCommandIndex % 25 == 0) {
                                    when {
                                        connectedBT != null -> bluetoothViewModel.sendGCode("?")
                                        connectedWiFi != null -> wifiViewModel.sendGCode("?")
                                        connectedUSB != null -> usbViewModel.sendGCode("?")
                                    }
                                }
                            }
                        } finally {
                            listenerJob.cancel()
                            if (lastSentCommandIndex >= gcodeCommands.size) {
                                isPrinting = false
                                consoleLogs.add("System: Print Complete")
                            }
                        }
                    }
                }

                val authState by authViewModel.authState.collectAsState()
                val loggedInUser by authViewModel.user.collectAsState()

                LaunchedEffect(authState) {
                    when (val state = authState) {
                        is AuthState.Success -> {
                            Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_SHORT).show()
                            when (appState) {
                                AppState.SignUp -> navigateTo(AppState.SignIn)
                                AppState.SignIn -> navigateTo(AppState.Home)
                                AppState.ForgotPassword -> navigateTo(AppState.VerificationCode)
                                AppState.VerificationCode -> navigateTo(AppState.ResetPassword)
                                AppState.ResetPassword -> {
                                    backStack.clear()
                                    backStack.add(AppState.SignIn)
                                    appState = AppState.SignIn
                                }
                                else -> {}
                            }
                            authViewModel.resetAuthState()
                        }
                        is AuthState.Error -> {
                            Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                            authViewModel.resetAuthState()
                        }
                        else -> {}
                    }
                }

                Crossfade(targetState = appState, animationSpec = tween(700), label = "AppNavigation") { state ->
                    when (state) {
                        AppState.Splash -> {
                            SplashScreen(onSplashFinished = { navigateTo(AppState.SignIn) })
                        }
                        AppState.SignIn -> {
                            SignInScreen(
                                onSignInSuccess = { email, pass -> authViewModel.login(email, pass) },
                                onCreateAccountClick = { navigateTo(AppState.SignUp) },
                                onForgotPasswordClick = { navigateTo(AppState.ForgotPassword) }
                            )
                        }
                        AppState.ForgotPassword -> {
                            ForgotPasswordScreen(
                                onBackClick = { handleBackButton() },
                                onSendCodeClick = { email -> 
                                    userEmailForReset = email
                                    authViewModel.forgotPassword(email) 
                                }
                            )
                        }
                        AppState.VerificationCode -> {
                            VerificationCodeScreen(
                                email = userEmailForReset,
                                onBackClick = { handleBackButton() },
                                onVerifyClick = { otp -> authViewModel.verifyOtp(userEmailForReset, otp) },
                                onResendClick = { authViewModel.forgotPassword(userEmailForReset) }
                            )
                        }
                        AppState.ResetPassword -> {
                            ResetPasswordScreen(
                                authState = authState,
                                onBackClick = { handleBackButton() },
                                onResetPasswordClick = { pass, confirm -> authViewModel.resetPassword(pass, confirm) }
                            )
                        }
                        AppState.SignUp -> {
                            SignUpScreen(
                                onBackClick = { handleBackButton() },
                                onSignUpClick = { name, email, pass, confirm -> 
                                    authViewModel.signup(name, email, pass, confirm) 
                                }
                            )
                        }
                        AppState.Home -> {
                            HomeScreen(
                                user = loggedInUser?.fullName ?: "Guest",
                                onNewPrintClick = { navigateTo(AppState.SelectDesign) },
                                onMultiColorDashboardClick = { navigateTo(AppState.MultiColorConfig) },
                                onConnectClick = { navigateTo(AppState.BluetoothDeviceList) },
                                onWifiConnectClick = { navigateTo(AppState.WifiDeviceList) },
                                onUsbConnectClick = { 
                                    usbViewModel.scanDevices()
                                    val devices = usbViewModel.availableDevices.value
                                    if (devices.isNotEmpty()) {
                                        usbViewModel.requestPermission(devices[0])
                                    } else {
                                        Toast.makeText(this@MainActivity, "No USB devices found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onLogoutClick = {
                                    bluetoothViewModel.disconnect()
                                    wifiViewModel.disconnect()
                                    usbViewModel.disconnect()
                                    navigateTo(AppState.SignIn)
                                },
                                connectedDeviceName = deviceName,
                                isConnected = isConnected,
                                bluetoothViewModel = bluetoothViewModel,
                                wifiViewModel = wifiViewModel
                            )
                        }
                        AppState.SelectDesign -> {
                            SelectDesignScreen(
                                onBackClick = { handleBackButton() },
                                onDesignSelected = { designName, uri ->
                                    selectedDesignName = designName
                                    selectedDesignUri = uri
                                    navigateTo(AppState.DesignPreview)
                                }
                            )
                        }
                        AppState.DesignPreview -> {
                            DesignPreviewScreen(
                                designName = selectedDesignName,
                                imageUri = selectedDesignUri,
                                onBackClick = { handleBackButton() },
                                onProceedClick = { navigateTo(AppState.Parameters) }
                            )
                        }
                        AppState.Parameters -> {
                            ParametersScreen(
                                onBackClick = { handleBackButton() },
                                onGenerateGCodeClick = { params -> 
                                    printerParameters = params
                                    navigateTo(AppState.PrintMode) 
                                },
                                initialParameters = printerParameters
                            )
                        }
                        AppState.PrintMode -> {
                            PrintModeScreen(
                                onBackClick = { handleBackButton() },
                                onModeSelected = { mode ->
                                    printMode = mode
                                    gcodeViewModel.generateGCode(selectedDesignName, selectedDesignUri, mode, printerParameters)
                                    navigateTo(AppState.GCodeGeneration)
                                }
                            )
                        }
                        AppState.GCodeGeneration -> {
                            GCodeGenerationScreen(
                                onBackClick = { handleBackButton() },
                                onRunSimulationClick = { gcode, layers ->
                                    gcodeViewModel.updateGCode(gcode, layers)
                                    simulationProgress = 0f
                                    isSimulationPlaying = true
                                    navigateTo(AppState.Simulation) 
                                },
                                designName = selectedDesignName,
                                designUri = selectedDesignUri,
                                printMode = printMode,
                                printerParameters = printerParameters
                            )
                        }
                        AppState.Simulation -> {
                            SimulationScreen(
                                onBackClick = { handleBackButton() },
                                onLiveDataClick = {
                                    navigateTo(AppState.LiveData)
                                },
                                gCodeString = generatedGCode,
                                maxLayers = maxLayers,
                                bedWidth = printerParameters.xMax.toFloatOrNull() ?: 200f,
                                bedDepth = printerParameters.yMax.toFloatOrNull() ?: 200f,
                                progress = simulationProgress,
                                onProgressChange = { simulationProgress = it },
                                isPlaying = isSimulationPlaying,
                                onPlayingChange = { isSimulationPlaying = it },
                                playbackSpeed = simulationPlaybackSpeed,
                                onPlaybackSpeedChange = { simulationPlaybackSpeed = it },
                                onProceedToPrint = { navigateTo(AppState.PrintConfirmation) }
                            )
                        }
                        AppState.PrintConfirmation -> {
                            PrintConfirmationScreen(
                                onBackClick = { handleBackButton() },
                                onStartPrintClick = { 
                                    lastSentCommandIndex = 0
                                    consoleLogs.clear()
                                    isPrinting = true
                                    isPaused = false
                                    isMultiColorFlow = false
                                    navigateTo(AppState.Operating) 
                                },
                                gCodeString = generatedGCode,
                                parameters = printerParameters,
                                maxLayers = maxLayers,
                                isConnected = isConnected
                            )
                        }
                        AppState.MultiColorConfig -> {
                            MultiColorConfigScreen(
                                onBackClick = { handleBackButton() },
                                onGenerateGCodeClick = { shape, num, x, y, z, inc, mode, servo ->
                                    multiColorShape = shape
                                    multiColorNumColors = num
                                    multiColorBaseX = x
                                    multiColorBaseY = y
                                    multiColorBaseZ = z
                                    multiColorIncrementXY = inc
                                    multiColorMode = mode
                                    printerParameters = printerParameters.copy(
                                        servoAngle = servo.toString(),
                                        shapeWidth = x.toString(),
                                        shapeHeight = y.toString(),
                                        numLayers = "1" 
                                    )
                                    navigateTo(AppState.MultiColorGCodeGeneration)
                                },
                                onColorsUpdate = { multiColorSelectedColors = it }
                            )
                        }
                        AppState.MultiColorGCodeGeneration -> {
                            com.simats.chacolateprinter.ui.MultiColorGCodeGenerationScreen(
                                onBackClick = { handleBackButton() },
                                onRunSimulationClick = { gcode, layers -> 
                                    gcodeViewModel.updateGCode(gcode, layers)
                                    simulationProgress = 0f
                                    isSimulationPlaying = true
                                    navigateTo(AppState.MultiColorSimulation)
                                },
                                shapeName = multiColorShape,
                                numColors = multiColorNumColors,
                                baseX = multiColorBaseX,
                                baseY = multiColorBaseY,
                                baseZ = multiColorBaseZ,
                                incrementXY = multiColorIncrementXY,
                                mode = multiColorMode,
                                printerParameters = printerParameters
                            )
                        }
                        AppState.MultiColorSimulation -> {
                            com.simats.chacolateprinter.ui.MultiColorSimulationScreen(
                                onBackClick = { handleBackButton() },
                                onLiveDataClick = {
                                    navigateTo(AppState.LiveData)
                                },
                                gCodeString = generatedGCode,
                                maxLayers = maxLayers,
                                colors = multiColorSelectedColors,
                                bedWidth = printerParameters.xMax.toFloatOrNull() ?: 200f,
                                bedDepth = printerParameters.yMax.toFloatOrNull() ?: 200f,
                                progress = simulationProgress,
                                onProgressChange = { simulationProgress = it },
                                isPlaying = isSimulationPlaying,
                                onPlayingChange = { isSimulationPlaying = it },
                                playbackSpeed = simulationPlaybackSpeed,
                                onPlaybackSpeedChange = { simulationPlaybackSpeed = it },
                                onProceedToPrint = { navigateTo(AppState.MultiColorPrintVerification) }
                            )
                        }
                        AppState.MultiColorPrintVerification -> {
                            com.simats.chacolateprinter.ui.MultiColorPrintVerificationScreen(
                                onBackClick = { handleBackButton() },
                                onStartPrintClick = { 
                                    lastSentCommandIndex = 0
                                    consoleLogs.clear()
                                    isPrinting = true
                                    isPaused = false
                                    isMultiColorFlow = true
                                    navigateTo(AppState.Operating) 
                                },
                                shapeName = multiColorShape,
                                numColors = multiColorNumColors,
                                baseX = multiColorBaseX,
                                baseY = multiColorBaseY,
                                baseZ = multiColorBaseZ,
                                incrementXY = multiColorIncrementXY,
                                mode = multiColorMode,
                                selectedColors = multiColorSelectedColors,
                                isConnected = isConnected
                            )
                        }
                        AppState.Operating -> {
                            OperatingScreen(
                                onBackClick = { handleBackButton() },
                                gCodeString = generatedGCode,
                                isConnected = isConnected,
                                deviceName = deviceName,
                                isPrinting = isPrinting,
                                isPaused = isPaused,
                                progress = printProgress,
                                lastSentIndex = lastSentCommandIndex,
                                consoleLogs = consoleLogs,
                                onStartClick = { 
                                    if (!isPrinting) {
                                        lastSentCommandIndex = 0
                                        consoleLogs.clear()
                                        isPrinting = true
                                        isPaused = false
                                    }
                                },
                                onPauseClick = { 
                                    isPaused = true
                                    bluetoothViewModel.pause()
                                    wifiViewModel.pause()
                                },
                                onResumeClick = { 
                                    isPaused = false
                                    bluetoothViewModel.resume()
                                    wifiViewModel.resume()
                                },
                                onStopClick = {
                                    isPrinting = false
                                    isPaused = false
                                    bluetoothViewModel.stop()
                                    wifiViewModel.stop()
                                    usbViewModel.sendGCode("M112") 
                                    navigateTo(AppState.Home)
                                }
                            )
                        }
                        AppState.LiveData -> {
                            val currentWPosBT by bluetoothViewModel.wPos.collectAsState()
                            val currentWPosWiFi by wifiViewModel.wPos.collectAsState()
                            val activePos = if (connectedBT != null) currentWPosBT else currentWPosWiFi
                            LiveDataScreen(
                                onBackClick = { handleBackButton() },
                                gCodeString = generatedGCode,
                                progress = if (isPrinting) printProgress else simulationProgress,
                                maxLayers = maxLayers,
                                currentX = activePos.x,
                                currentY = activePos.y,
                                currentZ = activePos.z,
                                isPrinting = isPrinting,
                                isPaused = isPaused,
                                onStopClick = { 
                                    isPrinting = false
                                    isPaused = false
                                    bluetoothViewModel.stop()
                                    wifiViewModel.stop()
                                    navigateTo(AppState.Home)
                                },
                                onPauseClick = { isPaused = true },
                                onResumeClick = { isPaused = false },
                                bedWidth = printerParameters.xMax.toFloatOrNull() ?: 200f,
                                bedDepth = printerParameters.yMax.toFloatOrNull() ?: 200f,
                                colors = multiColorSelectedColors
                            )
                        }
                        AppState.BluetoothDeviceList -> {
                            BluetoothDeviceListScreen(
                                onDeviceSelected = { device ->
                                    bluetoothViewModel.connectToDevice(device)
                                },
                                viewModel = bluetoothViewModel,
                                onBackClick = { handleBackButton() }
                            )
                        }
                        AppState.WifiDeviceList -> {
                            WifiDeviceListScreen(
                                onDeviceSelected = { network ->
                                    handleBackButton()
                                },
                                viewModel = wifiViewModel,
                                onBackClick = { handleBackButton() }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PrinterParameters(
    val layerHeight: String = "0.6",
    val printSpeed: String = "20",
    val travelSpeed: String = "60",
    val flowRate: String = "100",
    val infillDensity: Float = 20f,
    val wallThickness: String = "1.6",
    val nozzleDiameter: String = "0.8",
    val firstLayerHeight: String = "1",
    val retractionDistance: String = "2",
    val xMax: String = "200",
    val yMax: String = "200",
    val zMax: String = "150",
    val acceleration: String = "500",
    val jerk: String = "10",
    val servoAngle: String = "90",
    val shapeWidth: String = "50",
    val shapeHeight: String = "50",
    val numLayers: String = "5"
)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ChacolatePrinterTheme {
        HomeScreen(
            user = "John Doe",
            onNewPrintClick = {},
            onMultiColorDashboardClick = {},
            onConnectClick = {},
            onWifiConnectClick = {},
            onUsbConnectClick = {},
            onLogoutClick = {},
            connectedDeviceName = "None",
            isConnected = false,
            bluetoothViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
            wifiViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        )
    }
}
