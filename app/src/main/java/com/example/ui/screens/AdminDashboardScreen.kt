package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRecord
import com.example.data.QrConfig
import com.example.data.Student
import com.example.data.local.PreferencesManager
import com.example.data.remote.GoogleSheetApiService
import com.example.qr.PdfExportHelper
import com.example.qr.QrCodeGenerator
import com.example.ui.components.AddStudentDialog
import com.example.ui.components.AttendanceAnalyticsView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    preferencesManager: PreferencesManager,
    apiService: GoogleSheetApiService,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0: Students, 1: Attendance, 2: Analytics, 3: QR Generator

    var qrConfig by remember { mutableStateOf(preferencesManager.getQrConfig()) }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var showAppsScriptDialog by remember { mutableStateOf(false) }

    fun refreshData() {
        if (qrConfig.scriptUrl.isBlank()) return
        isLoading = true
        errorMessage = null
        scope.launch {
            val stRes = apiService.getStudents(qrConfig.scriptUrl)
            stRes.onSuccess { students = it }
                .onFailure { errorMessage = it.localizedMessage }

            val atRes = apiService.getAttendanceLogs(qrConfig.scriptUrl)
            atRes.onSuccess { attendanceRecords = it }

            isLoading = false
        }
    }

    LaunchedEffect(qrConfig.scriptUrl) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "অ্যাডমিন প্যানেল",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Job Express Library System",
                            fontSize = 11.sp,
                            color = Color(0xFFFFCDD2)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }, modifier = Modifier.testTag("admin_refresh_button")) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "রিফ্রেশ",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFD32F2F))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    label = { Text("শিক্ষার্থী", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFD32F2F),
                        indicatorColor = Color(0xFFFFEBEE)
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 1,
                    onClick = { selectedBottomTab = 1 },
                    icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                    label = { Text("হাজিরা", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFD32F2F),
                        indicatorColor = Color(0xFFFFEBEE)
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 2,
                    onClick = { selectedBottomTab = 2 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("অ্যানালিটিক্স", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFD32F2F),
                        indicatorColor = Color(0xFFFFEBEE)
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 3,
                    onClick = { selectedBottomTab = 3 },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    label = { Text("QR কোড", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFD32F2F),
                        indicatorColor = Color(0xFFFFEBEE)
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedBottomTab == 0) {
                FloatingActionButton(
                    onClick = { showAddStudentDialog = true },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_student_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "শিক্ষার্থী যুক্ত করুন")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            when (selectedBottomTab) {
                0 -> {
                    // TAB 0: STUDENTS MANAGEMENT
                    StudentsTab(
                        students = students,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onDeleteClick = { studentToDelete = it }
                    )
                }
                1 -> {
                    // TAB 1: ATTENDANCE LOGS
                    AttendanceLogsTab(
                        records = attendanceRecords,
                        isLoading = isLoading,
                        errorMessage = errorMessage
                    )
                }
                2 -> {
                    // TAB 2: ANALYTICS & CHARTS
                    AttendanceAnalyticsView(
                        attendanceRecords = attendanceRecords,
                        students = students,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                3 -> {
                    // TAB 3: QR GENERATOR & A4 EXPORT
                    QrGeneratorTab(
                        config = qrConfig,
                        onSaveConfig = { newConfig ->
                            qrConfig = newConfig
                            preferencesManager.saveQrConfig(newConfig)
                            Toast.makeText(context, "QR সেটিংস সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                            refreshData()
                        },
                        onOpenAppsScriptCode = { showAppsScriptDialog = true }
                    )
                }
            }
        }
    }

    if (showAddStudentDialog) {
        AddStudentDialog(
            scriptUrl = qrConfig.scriptUrl,
            apiService = apiService,
            onDismiss = { showAddStudentDialog = false },
            onStudentAdded = {
                Toast.makeText(context, "শিক্ষার্থী সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                refreshData()
            }
        )
    }

    studentToDelete?.let { st ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("শিক্ষার্থী মুছে ফেলবেন?", fontWeight = FontWeight.Bold) },
            text = { Text("${st.name} (LID: ${st.lid}) কে মুছে ফেলতে চান? গুগল শীট থেকে এই রেকর্ড ডিলিট হয়ে যাবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        val lid = st.lid
                        studentToDelete = null
                        scope.launch {
                            val res = apiService.deleteStudent(qrConfig.scriptUrl, lid)
                            res.onSuccess {
                                Toast.makeText(context, "শিক্ষার্থী মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                refreshData()
                            }.onFailure {
                                Toast.makeText(context, "ডিলিট করতে সমস্যা হয়েছে: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.testTag("confirm_delete_student_button")
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showAppsScriptDialog) {
        AppsScriptCodeDialog(onDismiss = { showAppsScriptDialog = false })
    }
}

@Composable
private fun StudentsTab(
    students: List<Student>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onDeleteClick: (Student) -> Unit
) {
    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.lid.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true) ||
            it.shelfNo.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("নাম, LID, ফোন বা শেল্ফ দিয়ে খুঁজুন...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth().testTag("search_students_field"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "মোট শিক্ষার্থী: ${filteredStudents.size} জন",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF334155)
            )
            val activeCount = filteredStudents.count { it.isActive }
            Text(
                text = "সক্রিয়: $activeCount | নিষ্ক্রিয়: ${filteredStudents.size - activeCount}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD32F2F))
            }
        } else if (filteredStudents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMessage ?: "কোনো শিক্ষার্থী পাওয়া যায়নি। নতুন যুক্ত করতে নিচের + বাটনে চাপ দিন।",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredStudents, key = { it.lid }) { student ->
                    StudentCard(student = student, onDeleteClick = { onDeleteClick(student) })
                }
            }
        }
    }
}

@Composable
private fun StudentCard(student: Student, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                    text = student.name.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = student.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (student.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (student.isActive) "Active" else "Inactive",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (student.isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "LID: ${student.lid} • Shelf: ${student.shelfNo.ifEmpty { "N/A" }} • Blood: ${student.bloodGroup}",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                if (student.phone.isNotBlank()) {
                    Text(
                        text = "Phone: ${student.phone}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "মুছে ফেলুন",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AttendanceLogsTab(
    records: List<AttendanceRecord>,
    isLoading: Boolean,
    errorMessage: String?
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(records, searchQuery) {
        if (searchQuery.isBlank()) records
        else records.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.lid.contains(searchQuery, ignoreCase = true) ||
            it.date.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("তারিখ (YYYY-MM-DD), নাম বা LID দিয়ে ফিল্টার...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "মোট রেকর্ড: ${filtered.size} টি",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF334155)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD32F2F))
            }
        } else if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMessage ?: "কোনো হাজিরা রেকর্ড পাওয়া যায়নি।",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${item.lid})",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "তারিখ: ${item.date}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "প্রবেশ: ${item.startTime} | বাহির: ${item.endTime.ifEmpty { "এখনো ভিতরে" }}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0288D1),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (item.isInside) Color(0xFFE8F5E9) else Color(0xFFF1F5F9))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (item.isInside) "ভিতরে আছেন" else "বের হয়েছেন",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isInside) Color(0xFF2E7D32) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrGeneratorTab(
    config: QrConfig,
    onSaveConfig: (QrConfig) -> Unit,
    onOpenAppsScriptCode: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var scriptUrl by remember { mutableStateOf(config.scriptUrl) }
    var latText by remember { mutableStateOf(config.latitude.toString()) }
    var lngText by remember { mutableStateOf(config.longitude.toString()) }

    val qrContent = remember(scriptUrl, latText, lngText) {
        val lat = latText.toDoubleOrNull() ?: 23.5462131
        val lng = lngText.toDoubleOrNull() ?: 89.1720178
        QrCodeGenerator.createQrDataJson(
            QrConfig(scriptUrl = scriptUrl.trim(), latitude = lat, longitude = lng, radiusMeters = 300.0)
        )
    }

    val qrBitmap = remember(qrContent) {
        if (scriptUrl.isNotBlank()) {
            QrCodeGenerator.generateQrBitmap(qrContent, width = 500, height = 500)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "QR কোড ও গুগল শীট কনফিগারেশন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "এই সেটিংসের ওপর ভিত্তি করে দেয়ালের QR কোড ও A4 PDF জেনারেট হবে",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = scriptUrl,
                    onValueChange = { scriptUrl = it },
                    label = { Text("Google Sheet Apps Script Web App URL") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    modifier = Modifier.fillMaxWidth().testTag("config_script_url_field"),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f).testTag("config_lat_field"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f).testTag("config_lng_field"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull() ?: 23.5462131
                        val lng = lngText.toDoubleOrNull() ?: 89.1720178
                        onSaveConfig(
                            QrConfig(
                                scriptUrl = scriptUrl.trim(),
                                latitude = lat,
                                longitude = lng,
                                radiusMeters = 300.0
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth().testTag("save_qr_config_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সেটিংস সংরক্ষণ করুন")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QR Code Preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "দেয়ালের জন্য কিউআর কোড প্রিভিউ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .border(2.dp, Color(0xFFD32F2F), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("উপরে স্ক্রিপ্ট লিংক দিলে কিউআর কোড দেখা যাবে", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action: A4 Size PDF Download / Print
                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull() ?: 23.5462131
                        val lng = lngText.toDoubleOrNull() ?: 89.1720178
                        val curConfig = QrConfig(scriptUrl = scriptUrl.trim(), latitude = lat, longitude = lng)
                        PdfExportHelper.generateAndShareA4Pdf(context, curConfig, qrBitmap)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth().testTag("download_a4_pdf_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "A4 সাইজে PDF ডাউনলোড / প্রিন্ট করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenAppsScriptCode,
                    modifier = Modifier.fillMaxWidth().testTag("view_apps_script_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Apps Script কোড দেখুন ও কপি করুন", color = Color(0xFFD32F2F))
                }
            }
        }
    }
}

@Composable
private fun AppsScriptCodeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scriptCode = """
/**
 * Job Express Attendance - Google Apps Script Backend
 * Paste this in Extensions -> Apps Script and Deploy as Web App (Anyone access)
 */
function doGet(e) { return handleRequest(e); }
function doPost(e) { return handleRequest(e); }

function handleRequest(e) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var studentsSheet = ss.getSheetByName("Students") || ss.insertSheet("Students");
  var attendanceSheet = ss.getSheetByName("Attendance") || ss.insertSheet("Attendance");
  var p = e && e.parameter ? e.parameter : {};
  var action = p.action || "get_stats";
  
  if (action === "get_students") {
    var data = studentsSheet.getDataRange().getValues();
    var list = [];
    for (var i = 1; i < data.length; i++) {
      if (data[i][0]) {
        list.push({
          lid: String(data[i][0]).trim(),
          name: String(data[i][1]||""),
          shelfNo: String(data[i][2]||""),
          phone: String(data[i][3]||""),
          bloodGroup: String(data[i][4]||"A+"),
          email: String(data[i][5]||""),
          address: String(data[i][6]||""),
          photoUrl: String(data[i][7]||""),
          active: String(data[i][8]||"active"),
          createdAt: String(data[i][9]||"")
        });
      }
    }
    return ContentService.createTextOutput(JSON.stringify({success:true, students:list})).setMimeType(ContentService.MimeType.JSON);
  }
  
  if (action === "record_attendance") {
    var lid = String(p.lid||"").trim().toLowerCase();
    var now = new Date();
    var tz = Session.getScriptTimeZone() || "Asia/Dhaka";
    var today = Utilities.formatDate(now, tz, "yyyy-MM-dd");
    var time = Utilities.formatDate(now, tz, "hh:mm a");
    
    // Check Students
    var sData = studentsSheet.getDataRange().getValues();
    var st = null;
    for (var i = 1; i < sData.length; i++) {
      if (String(sData[i][0]).trim().toLowerCase() === lid) {
        st = { lid: sData[i][0], name: sData[i][1], active: String(sData[i][8]||"active").toLowerCase() };
        break;
      }
    }
    if (!st || st.active !== "active") {
      return ContentService.createTextOutput(JSON.stringify({success:false, message:"স্টুডেন্ট আইডি সঠিক নয় বা নিষ্ক্রিয়।"})).setMimeType(ContentService.MimeType.JSON);
    }
    
    // Check Attendance
    var aData = attendanceSheet.getDataRange().getValues();
    var rowIndex = -1;
    for (var i = 1; i < aData.length; i++) {
      var d = Utilities.formatDate(new Date(aData[i][2]), tz, "yyyy-MM-dd");
      if (String(aData[i][0]).trim().toLowerCase() === lid && d === today) {
        rowIndex = i + 1;
        break;
      }
    }
    if (rowIndex === -1) {
      attendanceSheet.appendRow([st.lid, st.name, today, time, "", "", "Inside"]);
      return ContentService.createTextOutput(JSON.stringify({success:true, type:"ENTRY", time:time, date:today, student:st, message:"প্রবেশ সময় সফলভাবে রেকর্ড হয়েছে"})).setMimeType(ContentService.MimeType.JSON);
    } else {
      attendanceSheet.getRange(rowIndex, 5).setValue(time);
      attendanceSheet.getRange(rowIndex, 7).setValue("Left");
      return ContentService.createTextOutput(JSON.stringify({success:true, type:"EXIT", time:time, date:today, student:st, message:"বের হওয়ার সময় সফলভাবে রেকর্ড হয়েছে"})).setMimeType(ContentService.MimeType.JSON);
    }
  }
  
  // Default Stats
  var sCount = Math.max(0, studentsSheet.getLastRow() - 1);
  return ContentService.createTextOutput(JSON.stringify({success:true, stats:{totalStudents:sCount, totalSeats:sCount, availableSeats:sCount}})).setMimeType(ContentService.MimeType.JSON);
}
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Google Apps Script কোড", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "১. আপনার Google Sheet ওপেন করে Extensions -> Apps Script এ যান।\n" +
                            "২. নিচের কোডটি পেস্ট করে Deploy -> New Deployment -> Web App (Who has access: Anyone) করুন।\n" +
                            "৩. প্রাপ্ত Web App URL টি উপরের ইনপুট বক্সে পেস্ট করুন।",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scriptCode,
                        color = Color(0xFF81C784),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Apps Script Code", scriptCode))
                    Toast.makeText(context, "কোড ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("কোড কপি করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}
