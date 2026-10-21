package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.AttendanceResult
import com.example.data.LibraryStats
import com.example.data.Student
import com.example.data.local.PreferencesManager
import com.example.data.remote.GoogleSheetApiService
import com.example.location.LocationHelper
import com.example.qr.QrCodeGenerator
import com.example.ui.components.AdminLoginDialog
import com.example.ui.components.CameraQrScanner
import com.example.ui.components.DeveloperInfoDialog
import com.example.ui.components.LidRegistrationDialog
import com.example.ui.components.SeatStatusCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    preferencesManager: PreferencesManager,
    apiService: GoogleSheetApiService,
    onNavigateToAdmin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

    var student by remember { mutableStateOf(preferencesManager.getSavedStudent()) }
    var stats by remember { mutableStateOf(LibraryStats()) }
    var isStatsLoading by remember { mutableStateOf(false) }

    var showScanner by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showDeveloperInfoDialog by remember { mutableStateOf(false) }
    var showLidRegistrationDialog by remember { mutableStateOf(false) }

    var isVerifyingAttendance by remember { mutableStateOf(false) }
    var verificationStatusMessage by remember { mutableStateOf<String?>(null) }
    var attendanceResult by remember { mutableStateOf<AttendanceResult?>(null) }
    var locationErrorAlert by remember { mutableStateOf<String?>(null) }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var todayStatus by remember {
        mutableStateOf(preferencesManager.getTodayStatus(todayDateStr))
    }
    var todayEntryTime by remember {
        mutableStateOf(preferencesManager.getTodayEntryTime(todayDateStr))
    }
    var todayExitTime by remember {
        mutableStateOf(preferencesManager.getTodayExitTime(todayDateStr))
    }

    // Refresh Stats from Google Sheet
    fun refreshStats() {
        val config = preferencesManager.getQrConfig()
        if (config.scriptUrl.isBlank()) return
        isStatsLoading = true
        scope.launch {
            val res = apiService.getStats(config.scriptUrl)
            isStatsLoading = false
            res.onSuccess { stats = it }
        }
    }

    LaunchedEffect(Unit) {
        refreshStats()
        // If student is not registered yet, prompt LID dialog
        if (student == null) {
            showLidRegistrationDialog = true
        }
    }

    // Permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val cameraGranted = perms[Manifest.permission.CAMERA] ?: false
        val fineLocationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (cameraGranted && (fineLocationGranted || coarseLocationGranted)) {
            showScanner = true
        } else {
            Toast.makeText(
                context,
                "ক্যামেরা ও লোকেশন পারমিশন হাজিরা দেওয়ার জন্য প্রয়োজন।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun startScanningFlow() {
        val hasCamera = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCamera && hasLocation) {
            showScanner = true
        } else {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun handleScannedQrData(scannedData: String) {
        showScanner = false
        val parsedConfig = QrCodeGenerator.parseQrData(scannedData)
        if (parsedConfig == null) {
            Toast.makeText(context, "অবৈধ কিউআর কোড! Job Express দেয়ালের কোড স্ক্যান করুন।", Toast.LENGTH_LONG).show()
            return
        }

        // Save QR config locally if updated
        preferencesManager.saveQrConfig(parsedConfig)

        // Check if student is logged in
        val currentStudent = student
        if (currentStudent == null) {
            Toast.makeText(context, "অনুগ্রহ করে প্রথমে আপনার LID ভেরিফাই করুন।", Toast.LENGTH_LONG).show()
            showLidRegistrationDialog = true
            return
        }

        // Start Verification: GPS Location + Attendance Record
        isVerifyingAttendance = true
        verificationStatusMessage = "জিপিএস লোকেশন পরীক্ষা করা হচ্ছে..."

        scope.launch {
            // Step 1: Verify Location
            val locationCheck = locationHelper.verifyLocation(
                targetLat = parsedConfig.latitude,
                targetLng = parsedConfig.longitude,
                allowedRadiusMeters = parsedConfig.radiusMeters
            )

            if (!locationCheck.isWithinRange) {
                isVerifyingAttendance = false
                verificationStatusMessage = null
                locationErrorAlert = locationCheck.message
                return@launch
            }

            // Step 2: Location is valid, send Attendance to Google Sheet
            verificationStatusMessage = "গুগল শীটে হাজিরা জমা দেওয়া হচ্ছে..."
            val result = apiService.recordAttendance(parsedConfig.scriptUrl, currentStudent.lid)
            isVerifyingAttendance = false
            verificationStatusMessage = null

            result.onSuccess { res ->
                attendanceResult = res
                // Update local today state
                preferencesManager.updateTodayAttendance(todayDateStr, res.type, res.time)
                todayStatus = preferencesManager.getTodayStatus(todayDateStr)
                todayEntryTime = preferencesManager.getTodayEntryTime(todayDateStr)
                todayExitTime = preferencesManager.getTodayExitTime(todayDateStr)
                refreshStats()
            }.onFailure { ex ->
                locationErrorAlert = ex.localizedMessage ?: "হাজিরা রেকর্ড করতে সমস্যা হয়েছে"
            }
        }
    }

    // Pulsating animation for scanner button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_button")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    if (showScanner) {
        CameraQrScanner(
            onQrScanned = { handleScannedQrData(it) },
            onClose = { showScanner = false }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // -------------------------------------------------------------
            // TOP HEADER: Brand, Admin Button, Developer Info Button
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFD32F2F), Color(0xFFC62828))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Job Express",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Smart Library Attendance",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFCDD2)
                        )
                    }

                    // Developer Information Button (small, top corner)
                    IconButton(
                        onClick = { showDeveloperInfoDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .testTag("developer_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "ডেভেলপার পরিচিতি",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Admin Button (small, top corner)
                    IconButton(
                        onClick = { showAdminLoginDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .testTag("admin_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "অ্যাডমিন প্যানেল",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // -------------------------------------------------------------
                // STUDENT PROFILE BANNER
                // -------------------------------------------------------------
                if (student != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = student?.name?.take(1)?.uppercase() ?: "S",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color(0xFFD32F2F)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student?.name ?: "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "LID: ${student?.lid} • Shelf: ${student?.shelfNo} • Blood: ${student?.bloodGroup}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            // Today attendance status pill
                            val isCurrentlyInside = todayStatus == "Inside"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrentlyInside) Color(0xFFE8F5E9) else Color(0xFFF1F5F9))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isCurrentlyInside) "ভিতরে আছেন" else if (todayStatus == "Left") "বাহিরে আছেন" else "হাজিরা বাকি",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentlyInside) Color(0xFF2E7D32) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                } else {
                    // Not registered yet
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLidRegistrationDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "আপনার স্টুডেন্ট আইডি (LID) ভেরিফাই করুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = "হাজিরা দিতে একবার আপনার LID ভেরিফাই করা প্রয়োজন",
                                    fontSize = 11.sp,
                                    color = Color(0xFFBF360C)
                                )
                            }
                            Text(
                                text = "ভেরিফাই",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // -------------------------------------------------------------
                // GIANT QR CODE SCANNER BUTTON
                // -------------------------------------------------------------
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = { startScanningFlow() },
                        shape = RoundedCornerShape(26.dp),
                        color = Color.Transparent,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .scale(pulseScale)
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("scan_qr_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFE53935), Color(0xFFB71C1C))
                                    )
                                )
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "স্ক্যানার",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(18.dp))

                                Column {
                                    Text(
                                        text = "QR কোড স্ক্যান করুন",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "দেয়ালের কিউআর কোড স্ক্যান করে হাজিরা দিন",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFFCDD2)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "অটো জিপিএস ভেরিফিকেশন সক্রিয়",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFE082)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // -------------------------------------------------------------
                // SEAT STATUS & ATTENDANCE STATS CARD
                // -------------------------------------------------------------
                SeatStatusCard(stats = stats)

                Spacer(modifier = Modifier.height(16.dp))

                // Today Attendance Summary Tile
                if (todayEntryTime != null || todayExitTime != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "আজকের আপনার হাজিরা হিস্ট্রি",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("প্রবেশ সময়", fontSize = 11.sp, color = Color.Gray)
                                        Text(todayEntryTime ?: "--:--", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("বের হওয়ার সময়", fontSize = 11.sp, color = Color.Gray)
                                        Text(todayExitTime ?: "এখনো ভিতরে", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verification in-progress overlay
        if (isVerifyingAttendance) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = verificationStatusMessage ?: "হাজিরা যাচাই হচ্ছে...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }
    }

    // Attendance Result Dialog (Entry vs Exit)
    attendanceResult?.let { res ->
        val isEntry = res.type.equals("ENTRY", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { attendanceResult = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isEntry) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEntry) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = if (isEntry) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isEntry) "প্রবেশ সময় রেকর্ড হয়েছে!" else "বের হওয়ার সময় রেকর্ড হয়েছে!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (isEntry) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                        Text(
                            text = if (isEntry) "প্রথম স্ক্যান (স্বাগতম)" else "দ্বিতীয় স্ক্যান (বিদায়)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = res.message,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "শিক্ষার্থী: ${res.student?.name ?: student?.name ?: ""}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "রেকর্ড সময়: ${res.time} (${res.date})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFD32F2F)
                            )
                            Text(
                                text = "লোকেশন স্ট্যাটাস: লাইব্রেরি প্রাঙ্গণ নিশ্চিত",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { attendanceResult = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    // Location Error Alert Dialog with helpline
    locationErrorAlert?.let { err ->
        AlertDialog(
            onDismissRequest = { locationErrorAlert = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("হাজিরা ব্যর্থ হয়েছে", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column {
                    Text(text = err, fontSize = 13.sp, color = Color(0xFF334155), lineHeight = 19.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "সমস্যা হলে লাইব্রেরি হেল্পলাইনে যোগাযোগ করতে পারেন:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:01351835060")
                            }
                            try {
                                context.startActivity(dialIntent)
                            } catch (e: Exception) {
                                // ignore
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("কল করুন (01351835060)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { locationErrorAlert = null }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    if (showAdminLoginDialog) {
        AdminLoginDialog(
            onDismiss = { showAdminLoginDialog = false },
            onLoginSuccess = {
                showAdminLoginDialog = false
                onNavigateToAdmin()
            }
        )
    }

    if (showDeveloperInfoDialog) {
        DeveloperInfoDialog(onDismiss = { showDeveloperInfoDialog = false })
    }

    if (showLidRegistrationDialog) {
        LidRegistrationDialog(
            preferencesManager = preferencesManager,
            apiService = apiService,
            onRegistered = { regStudent ->
                student = regStudent
                showLidRegistrationDialog = false
                Toast.makeText(context, "স্বাগতম, ${regStudent.name}! আপনার LID সংরক্ষিত হয়েছে।", Toast.LENGTH_LONG).show()
            },
            onDismiss = { showLidRegistrationDialog = false }
        )
    }
}
