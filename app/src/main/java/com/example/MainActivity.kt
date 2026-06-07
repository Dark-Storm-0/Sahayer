package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TobaccoAuthEngine
import com.example.data.TobaccoPackInfo
import com.example.data.VerificationLog
import com.example.data.VerificationOutcome
import com.example.ui.VerificationViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CigaretteAuthenticatorApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Main Composable Layout
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CigaretteAuthenticatorApp(
    modifier: Modifier = Modifier,
    viewModel: VerificationViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: QR Scan, 1: Checklist, 2: AI Advisor, 3: History Logs
    
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val scannedCode by viewModel.scannedCode.collectAsStateWithLifecycle()
    val isCameraOpen by viewModel.isCameraOpen.collectAsStateWithLifecycle()
    val verificationOutcome by viewModel.verificationOutcome.collectAsStateWithLifecycle()
    val isVerifying by viewModel.isVerifying.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SecurityDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Navigation-Safe Top Header
            AppHeaderSection()

            // Main Interactive Section Swapper
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> QrScannerTab(
                        viewModel = viewModel,
                        scannedCode = scannedCode,
                        isCameraOpen = isCameraOpen,
                        isVerifying = isVerifying
                    )
                    1 -> ManualChecklistTab(
                        viewModel = viewModel
                    )
                    2 -> AiForensicsTab(
                        viewModel = viewModel
                    )
                    3 -> LogHistoryTab(
                        logs = logs,
                        onDeleteLog = { viewModel.deleteLog(it) },
                        onClearAll = { viewModel.clearAllLogs() }
                    )
                }
            }

            // Bottom Navigation Panel (Centralized in Single View)
            Surface(
                color = GeometricOverlayBg,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, GeometricDivider)
            ) {
                TabSelectorBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    logsCount = logs.size
                )
            }
        }

        // Animated Dialog Overlay showing Authenticity Status Verification Outcome Reports
        verificationOutcome?.let { outcome ->
            VerificationResultDialog(
                outcome = outcome,
                onDismiss = { viewModel.clearActiveOutcome() }
            )
        }
    }
}

// Header Section
@Composable
fun AppHeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = GeometricDivider,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(SecurityAccentBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TOBACCO SECURE-NET",
                    color = SecurityAccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Cigarette Authenticator",
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = GeometricOverlayBg,
            border = BorderStroke(1.dp, SecurityAccentBlue),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Active Guard Status",
                    tint = AuthenticGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Guard Node Active",
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Bottom Tab Selector Component
@Composable
fun TabSelectorBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    logsCount: Int
) {
    val items = listOf(
        TabItem("Scanner", Icons.Default.QrCodeScanner, 0),
        TabItem("Visual Check", Icons.Default.Rule, 1),
        TabItem("AI Recon", Icons.Default.AutoAwesome, 2),
        TabItem("Scan Logs", Icons.Default.History, 3)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = selectedTab == item.index
            val color = if (isSelected) SecurityAccentBlue else TextSecondaryDark

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(item.index) }
                    .padding(vertical = 6.dp)
                    .testTag("tab_item_${item.index}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    // Badge Count on Scan Logs index
                    if (item.index == 3 && logsCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Red,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-4).dp)
                        ) {
                            Text(
                                text = "$logsCount",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.label,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class TabItem(val label: String, val icon: ImageVector, val index: Int)

// ------------------- QR Code Tab View -------------------
@Composable
fun QrScannerTab(
    viewModel: VerificationViewModel,
    scannedCode: String,
    isCameraOpen: Boolean,
    isVerifying: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var manualInputText by remember { mutableStateOf("") }
    
    // Permission launcher for opening the physical scan camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setCameraOpen(true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Core Scanner Frame Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecurityCardBg),
                border = BorderStroke(1.dp, GeometricBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pack Verification Hub",
                        color = TextPrimaryDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Point camera at the serial QR print or select a test scenario to evaluate local security checks.",
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated Camera View Finder or Real Camera Preview Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GeometricOverlayBg)
                            .border(BorderStroke(2.dp, GeometricBorder), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCameraOpen) {
                            CameraPreviewContainer(
                                onQrScanned = { code ->
                                    viewModel.setCameraOpen(false)
                                    viewModel.verifyScannedCode(code)
                                }
                            )
                        } else {
                            // ViewFinder Standby Overlay with scan visual lines
                            ViewFinderStandbyOverlay(
                                onStartScan = {
                                    val cameraPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.CAMERA
                                    )
                                    if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                                        viewModel.setCameraOpen(true)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            )
                        }
                    }

                    if (isVerifying) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(
                            color = SecurityAccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Section: Instant Test Case Simulator (Super helpful for Emulators/Screencasts!)
        item {
            Column {
                Text(
                    text = "🔐 Direct Code Simulation Panel",
                    color = TextPrimaryDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Select an automated preset scenario to demonstrate high-accuracy double-scan cloned stamps, valid region stamps, or invalid mathematical CRC checks.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SimulatorItem(
                        title = "Authentic Marlboro Gold Packet",
                        code = "EU-LEPL-M72B-94F32948",
                        verdict = "Genuine EU Track System Stamp",
                        badgeColor = AuthenticGreen,
                        onClick = { viewModel.verifyScannedCode("EU-LEPL-M72B-94F32948") }
                    )
                    SimulatorItem(
                        title = "Authentic Camel Blue Selection",
                        code = "EU-EUTAC-C12A-18F42173",
                        verdict = "Genuine Customs Tax Paid",
                        badgeColor = AuthenticGreen,
                        onClick = { viewModel.verifyScannedCode("EU-EUTAC-C12A-18F42173") }
                    )
                    SimulatorItem(
                        title = "Double-Scan Cloned Serial Alert",
                        code = "EU-LEPL-M72R-50F12450",
                        verdict = "Triggers immediate Duplicate Warning",
                        badgeColor = CounterfeitRed,
                        onClick = {
                            // First scan works fine
                            viewModel.verifyScannedCode("EU-LEPL-M72R-50F12450")
                        },
                        isCloneDemonstration = true,
                        onScanDouble = {
                            // Triggers warning since it is in database logs now!
                            viewModel.verifyScannedCode("EU-LEPL-M72R-50F12450")
                        }
                    )
                    SimulatorItem(
                        title = "Smuggled Old Stock Stamp Case",
                        code = "EU-LEPL-M72B-94F32100", // Will resolve as Marlboro Gold but expired date 2021
                        verdict = "Triggers stale aging caution warnings",
                        badgeColor = SuspiciousAmber,
                        onClick = { viewModel.verifyScannedCode("EU-LEPL-M72B-94F32100") }
                    )
                    SimulatorItem(
                        title = "Counterfeit Code (Muffled Checksum)",
                        code = "EU-EUTAC-C12A-18F42199",
                        verdict = "Parity check fails (Mathematical check failed)",
                        badgeColor = CounterfeitRed,
                        onClick = { viewModel.verifyScannedCode("EU-EUTAC-C12A-18F42199") }
                    )
                    SimulatorItem(
                        title = "Unrecognized Regional Stamp Signature",
                        code = "RU-BAD-SIGN-FORM-AB8D",
                        verdict = "Invalid prefix format alert",
                        badgeColor = CounterfeitRed,
                        onClick = { viewModel.verifyScannedCode("RU-BAD-SIGN-FORM-AB8D") }
                    )
                }
            }
        }

        // Section: Manual Code Entry Keyboard Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecurityCardBg),
                border = BorderStroke(1.dp, GeometricBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⌨️ Manual Serial Security Entry",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { manualInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_serial_input"),
                        placeholder = { Text("E.g., EU-LEPL-M72B-94F32948") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecurityAccentBlue,
                            unfocusedBorderColor = GeometricBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedPlaceholderColor = TextSecondaryDark,
                            unfocusedPlaceholderColor = TextSecondaryDark
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (manualInputText.isNotBlank()) {
                                viewModel.verifyScannedCode(manualInputText, "KEYBOARD INPUT")
                                focusManager.clearFocus()
                            }
                        })
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (manualInputText.isNotBlank()) {
                                viewModel.verifyScannedCode(manualInputText, "KEYBOARD INPUT")
                                focusManager.clearFocus()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecurityAccentBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_manual_code_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manually Authenticate Pack Serial", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ViewFinderStandbyOverlay(
    onStartScan: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val lineY = size.height * scanProgress
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 3f
                )
            }
            .clickable { onStartScan() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Trigger Camera Focus",
                tint = SecurityAccentBlue,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to Launch Scanner Camera",
                color = TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Use device camera to physically scan QR Codes",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// CameraX Preview Container safely embedded (Simulated fallback if permission error)
@Composable
fun CameraPreviewContainer(
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // Simply bind life-cycle
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Camera failed to initialize"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (errorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Camera Feed Restricted: Emulating scan", color = TextSecondaryDark, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onQrScanned("EU-LEPL-M72B-94F32948") },
                    colors = ButtonDefaults.buttonColors(containerColor = SecurityAccentBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Auto Capture Simulated QR Code", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            // Scanner guide overlay lines
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .border(BorderStroke(2.dp, SecurityAccentBlue), RoundedCornerShape(12.dp))
                    .align(Alignment.Center)
            )
            // Float click scan capture button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SecurityDarkBg.copy(alpha = 0.8f))
                    .clickable { onQrScanned("EU-LEPL-M72B-94F32948") }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Press to Capture Focus Code", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SimulatorItem(
    title: String,
    code: String,
    verdict: String,
    badgeColor: Color,
    onClick: () -> Unit,
    isCloneDemonstration: Boolean = false,
    onScanDouble: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .border(BorderStroke(1.dp, badgeColor), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Preset",
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Code: $code",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Expected Verdict: $verdict",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            if (isCloneDemonstration && onScanDouble != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onScanDouble() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("Trigger Cloning Double Scan (2nd Scanned Block Check)", color = Color.White, fontSize = 9.sp)
                }
            }
        }
    }
}

// ------------------- Visual Inspection Checklist Tab -------------------
@Composable
fun ManualChecklistTab(
    viewModel: VerificationViewModel
) {
    val brands = listOf("Marlboro Gold", "Marlboro Red", "Camel Blue", "Winston Blue", "Lucky Strike Red", "Dunhill Red", "Other Brand")
    val selectedBrand by viewModel.visualChecklistBrand.collectAsStateWithLifecycle()
    var expandedBrandMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecurityCardBg),
                border = BorderStroke(1.dp, GeometricBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cigarette Forensics Checklist",
                        color = TextPrimaryDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "When barcodes are scuffed or QR signatures lack active telemetry, execute this manual physical verification based on tax stamps and wrapping indicators.",
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Brand Dropdown Selection
                    Text(
                        text = "Select Brand Inspected",
                        color = SecurityAccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedBrandMenu = true }
                                .border(BorderStroke(1.dp, GeometricBorder), RoundedCornerShape(8.dp)),
                            color = GeometricOverlayBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = selectedBrand, color = TextPrimaryDark, fontSize = 14.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryDark)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedBrandMenu,
                            onDismissRequest = { expandedBrandMenu = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(SecurityCardBg)
                        ) {
                            brands.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand, color = TextPrimaryDark) },
                                    onClick = {
                                        viewModel.setVisualBrand(brand)
                                        expandedBrandMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Checklist Question Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChecklistQuestionItem(
                    title = "🪙 Customs Excise Tax Stamp",
                    question = "Does the paper stamp feature razor-sharp regulatory text, tight alignment with the cardboard package lid, and clean metallic color-shifting ink when tilted under overhead shop lights?",
                    criterion = "stamp",
                    currentValue = viewModel.visualAnswers["stamp"],
                    onValueChange = { viewModel.setVisualAnswer("stamp", it) }
                )
                ChecklistQuestionItem(
                    title = "🔬 warning microtext typography",
                    question = "Under direct light, inspect the safety typography. Are the fonts crisp and clean? (Counterfeited packs typically feature blurry edges, color bleeding, or minor spelling errors/spacing issues).",
                    criterion = "print",
                    currentValue = viewModel.visualAnswers["print"],
                    onValueChange = { viewModel.setVisualAnswer("print", it) }
                )
                ChecklistQuestionItem(
                    title = "📦 Machine-Sealed Wrapper Folds",
                    question = "Is the clear outer cellophane wrapper tight, with perfectly crisp parallel machine seals and tiny tidy dots of adhesive on the bottom? (Hand-glued fakes look loose/scruffy).",
                    criterion = "wrapper",
                    currentValue = viewModel.visualAnswers["wrapper"],
                    onValueChange = { viewModel.setVisualAnswer("wrapper", it) }
                )
                ChecklistQuestionItem(
                    title = "🏷️ Metallic Tear-Tape Film strip",
                    question = "Inspect the gold or branded plastic tear strip wrapping around the pack. Does it open smoothly along a line containing uniform microprinted official factory crests? (Fakes use blank cheap foil).",
                    criterion = "tape",
                    currentValue = viewModel.visualAnswers["tape"],
                    onValueChange = { viewModel.setVisualAnswer("tape", it) }
                )
            }
        }

        // Process Action button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetVisualChecklist() },
                    modifier = Modifier.weight(0.35f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                    border = BorderStroke(1.dp, GeometricBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reset")
                }

                Button(
                    onClick = { viewModel.submitVisualChecklist() },
                    modifier = Modifier
                        .weight(0.65f)
                        .testTag("submit_checklist_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SecurityAccentBlue),
                    shape = RoundedCornerShape(8.dp),
                    enabled = viewModel.visualAnswers.values.any { it != null }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculate Verdict", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistQuestionItem(
    title: String,
    question: String,
    criterion: String,
    currentValue: Boolean?,
    onValueChange: (Boolean?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SecurityCardBg),
        border = BorderStroke(1.dp, if (currentValue != null) SecurityAccentBlue.copy(alpha = 0.5f) else GeometricBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title.uppercase(),
                color = SecurityAccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = question,
                color = TextPrimaryDark,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Yes Option
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (currentValue == true) AuthenticGreen.copy(alpha = 0.15f) else GeometricOverlayBg)
                        .border(
                            BorderStroke(1.dp, if (currentValue == true) AuthenticGreen else GeometricBorder),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onValueChange(true) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (currentValue == true) AuthenticGreen else TextSecondaryDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Authentic",
                        color = if (currentValue == true) TextPrimaryDark else TextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // No Option
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (currentValue == false) CounterfeitRed.copy(alpha = 0.15f) else GeometricOverlayBg)
                        .border(
                            BorderStroke(1.dp, if (currentValue == false) CounterfeitRed else GeometricBorder),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onValueChange(false) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (currentValue == false) CounterfeitRed else TextSecondaryDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Suspect",
                        color = if (currentValue == false) TextPrimaryDark else TextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ------------------- AI Assistant Forensics Tab view -------------------
@Composable
fun AiForensicsTab(
    viewModel: VerificationViewModel
) {
    val aiPrompt by viewModel.aiPrompt.collectAsStateWithLifecycle()
    val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val selectedBrandForAi by viewModel.inspectedBrandForAi.collectAsStateWithLifecycle()

    val aiBrands = listOf("Marlboro Gold", "Marlboro Red", "Camel Blue", "Winston Blue", "Lucky Strike Red", "Dunhill Red", "Other")
    var expandedAiMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val promptPresets = listOf(
        "Tax stamp paper label feel photocopied, coloring stays flat yellow when shifting under light.",
        "Warning text has spelling label typo: printed 'Healt Danger' instead of 'Health'.",
        "Cellophane wrapping around the outer box has loose folds and messy thick hot-glue residue on bottom.",
        "Tear string breaks mid-peel, lacks red verification marker seal entirely."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecurityCardBg),
                border = BorderStroke(1.dp, GeometricBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🤖 AI Forensics Inspection",
                        color = TextPrimaryDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Consult our server-side secure Gemini AI Model to perform deep package diagnostics based on custom anomaly descriptions.",
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Brand selection
                    Text(
                        text = "Pack Brand to Inquire",
                        color = SecurityAccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedAiMenu = true }
                                .border(BorderStroke(1.dp, GeometricBorder), RoundedCornerShape(8.dp)),
                            color = GeometricOverlayBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = selectedBrandForAi, color = TextPrimaryDark, fontSize = 14.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryDark)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedAiMenu,
                            onDismissRequest = { expandedAiMenu = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(SecurityCardBg)
                        ) {
                            aiBrands.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b, color = TextPrimaryDark) },
                                    onClick = {
                                        viewModel.setInspectedBrandForAi(b)
                                        expandedAiMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Input
                    Text(
                        text = "Describe Observed Physical Defects",
                        color = SecurityAccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { viewModel.setAiPrompt(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("ai_prompt_text_field"),
                        placeholder = { Text("E.g., The paper tax stamp text is somewhat blurry and the hologram sticker tilts with a simple flat color.") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecurityAccentBlue,
                            unfocusedBorderColor = GeometricBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedPlaceholderColor = TextSecondaryDark,
                            unfocusedPlaceholderColor = TextSecondaryDark
                        ),
                        maxLines = 4
                    )
                }
            }
        }

        // Quick Preset Tags
        item {
            Column {
                Text(
                    text = "💡 Quick Report Presets (Tap to Fill)",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRowWithSpacing(spacing = 8.dp) {
                    promptPresets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = GeometricOverlayBg,
                            border = BorderStroke(1.dp, GeometricBorder),
                            modifier = Modifier
                                .clickable { viewModel.setAiPrompt(preset) }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = if (preset.length > 50) preset.take(47) + "..." else preset,
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Execute Consultation Trigger
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetAiConsole() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                    border = BorderStroke(1.dp, GeometricBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.35f)
                ) {
                    Text("Clear")
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.analyzePackWithAi()
                    },
                    modifier = Modifier
                        .weight(0.65f)
                        .testTag("submit_ai_diagnose_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SecurityAccentBlue),
                    shape = RoundedCornerShape(8.dp),
                    enabled = aiPrompt.isNotBlank() && !isAiLoading
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAiLoading) {
                             CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAiLoading) "Analyzing..." else "Verify via Gemini", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Response Render
        item {
            AnimatedVisibility(
                visible = isAiLoading || aiResult != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SecurityAccentBlue.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = GeometricOverlayBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecurityAccentBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini Forensic Assessment", color = TextPrimaryDark, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isAiLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    color = SecurityAccentBlue,
                                    trackColor = SecurityCardBg,
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Connecting Tobacco Guard AI Model...", color = TextSecondaryDark, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            aiResult?.let { res ->
                                Text(
                                    text = res,
                                    color = TextPrimaryDark,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple Layout Helper to place preset chips dynamically
@Composable
fun FlowRowWithSpacing(
    spacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Simple sequential rendering
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // To keep layout fully compatible and avoid compile experimental errors we simply layout items
                content()
            }
        }
    }
}

// ------------------- Log History Tab -------------------
@Composable
fun LogHistoryTab(
    logs: List<VerificationLog>,
    onDeleteLog: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Verification Audit Logs",
                    color = TextPrimaryDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${logs.size} items recorded locally",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }

            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = { onClearAll() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                    modifier = Modifier.testTag("clear_all_logs_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Wipe database", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Empty History logs",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Scans Logged Yet",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Successfully verified items will register a tamper-proof audit trace here.",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogCardItem(
                        log = log,
                        onDelete = { onDeleteLog(log.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LogCardItem(
    log: VerificationLog,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss - MMM dd, yyyy", Locale.getDefault()) }
    val timeLabel = formatter.format(Date(log.timestamp))

    val statusColor = if (!log.isAuthentic) {
        CounterfeitRed
    } else if (log.confidence < 85) {
        SuspiciousAmber
    } else {
        AuthenticGreen
    }

    val icon = if (!log.isAuthentic) {
        Icons.Default.GppBad
    } else if (log.confidence < 85) {
        Icons.Default.ReportProblem
    } else {
        Icons.Default.GppGood
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SecurityCardBg),
        border = BorderStroke(1.dp, GeometricBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Circle Status indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.brandName,
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Delete item log", tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(GeometricOverlayBg)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = log.verificationMethod,
                                color = SecurityAccentBlue,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeLabel,
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Code and diagnosis reports
            Text(
                text = "Track Code: ${log.serialNumber}",
                color = TextPrimaryDark,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.statusReason,
                color = TextSecondaryDark,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

// ------------------- OVERLAY DIALOGS: AUTH WORK DONE PREVIEW REPORT -------------------
@Composable
fun VerificationResultDialog(
    outcome: VerificationOutcome,
    onDismiss: () -> Unit
) {
    val title: String
    val verdictText: String
    val statusColor: Color
    val cardIcon: ImageVector

    when (outcome) {
        is VerificationOutcome.Success -> {
            title = "VERIFIED GENUINE"
            verdictText = "SECURITY VERIFICATION CLEAR"
            statusColor = AuthenticGreen
            cardIcon = Icons.Default.Verified
        }
        is VerificationOutcome.Warning -> {
            title = "STAMP SUSPECT WARNING"
            verdictText = "EXPIRED OR REPURPOSED EMBOSS"
            statusColor = SuspiciousAmber
            cardIcon = Icons.Default.Warning
        }
        is VerificationOutcome.Failed -> {
            title = "COUNTERFEIT WARNING"
            verdictText = "CRITICAL FAIL MARKS DETECTED"
            statusColor = CounterfeitRed
            cardIcon = Icons.Default.Dangerous
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SecurityCardBg),
            border = BorderStroke(2.dp, statusColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .testTag("result_dialog_panel")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Badge Indicator
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = cardIcon,
                        contentDescription = "Status symbol",
                        tint = statusColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    color = statusColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = verdictText,
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Brand details block
                Surface(
                    color = GeometricOverlayBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GeometricBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Brand:",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                modifier = Modifier.width(60.dp)
                            )
                            Text(
                                text = when (outcome) {
                                    is VerificationOutcome.Success -> outcome.brand
                                    is VerificationOutcome.Warning -> outcome.brand
                                    is VerificationOutcome.Failed -> outcome.brand
                                },
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Rent specific properties if Success / Warning
                        val info = when (outcome) {
                            is VerificationOutcome.Success -> outcome.packInfo
                            is VerificationOutcome.Warning -> outcome.packInfo
                            else -> null
                        }

                        if (info != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Batch Date:",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(60.dp)
                                )
                                Text(
                                    text = info.manufactureDate,
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Line:",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(60.dp)
                                )
                                Text(
                                    text = info.productionLine,
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Factory:",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(60.dp)
                                )
                                Text(
                                    text = info.factoryLocation,
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Excise/Tax:",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(60.dp)
                                )
                                Text(
                                    text = info.taxPaidStatus,
                                    color = SecurityAccentBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Diagnostic rationale
                Text(
                    text = "DIAGNOSTIC REPORT SUMMARY",
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (outcome) {
                        is VerificationOutcome.Success -> outcome.details
                        is VerificationOutcome.Warning -> outcome.reason
                        is VerificationOutcome.Failed -> outcome.reason
                    },
                    color = TextPrimaryDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_result_button")
                ) {
                    Text(
                        text = "Acknowledge & Continue",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
