package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRecord
import com.example.data.Student

enum class AnalyticsTab {
    PEAK_HOURS,
    DAILY_TREND,
    SINGLE_STUDENT
}

@Composable
fun AttendanceAnalyticsView(
    attendanceRecords: List<AttendanceRecord>,
    students: List<Student>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AnalyticsTab.PEAK_HOURS) }
    var selectedStudentLid by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tab Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == AnalyticsTab.PEAK_HOURS,
                onClick = { selectedTab = AnalyticsTab.PEAK_HOURS },
                label = { Text("পিক আওয়ার (চাপ)", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFD32F2F),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )

            FilterChip(
                selected = selectedTab == AnalyticsTab.DAILY_TREND,
                onClick = { selectedTab = AnalyticsTab.DAILY_TREND },
                label = { Text("দৈনিক উপস্থিতি ট্রেন্ড", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFD32F2F),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )

            FilterChip(
                selected = selectedTab == AnalyticsTab.SINGLE_STUDENT,
                onClick = { selectedTab = AnalyticsTab.SINGLE_STUDENT },
                label = { Text("একক শিক্ষার্থী", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFD32F2F),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            AnalyticsTab.PEAK_HOURS -> {
                PeakHoursChartCard(attendanceRecords)
            }
            AnalyticsTab.DAILY_TREND -> {
                DailyTrendChartCard(attendanceRecords, students.size.coerceAtLeast(1))
            }
            AnalyticsTab.SINGLE_STUDENT -> {
                SingleStudentAnalyticsView(
                    students = students,
                    attendanceRecords = attendanceRecords,
                    selectedLid = selectedStudentLid,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSelectLid = { selectedStudentLid = it }
                )
            }
        }
    }
}

@Composable
fun PeakHoursChartCard(records: List<AttendanceRecord>) {
    // Group records by 2-hour interval
    val timeSlots = listOf("08-10 AM", "10-12 PM", "12-02 PM", "02-04 PM", "04-06 PM", "06-08 PM", "08-10 PM")
    val counts = remember(records) {
        val result = IntArray(timeSlots.size)
        for (rec in records) {
            val t = rec.startTime.uppercase()
            when {
                t.contains("08:") || t.contains("09:") -> if (t.contains("AM")) result[0]++
                t.contains("10:") || t.contains("11:") -> if (t.contains("AM")) result[1]++
                t.contains("12:") || t.contains("01:") -> if (t.contains("PM")) result[2]++
                t.contains("02:") || t.contains("03:") -> if (t.contains("PM")) result[3]++
                t.contains("04:") || t.contains("05:") -> if (t.contains("PM")) result[4]++
                t.contains("06:") || t.contains("07:") -> if (t.contains("PM")) result[5]++
                t.contains("08:") || t.contains("09:") -> if (t.contains("PM")) result[6]++
                else -> result[1]++ // default bucket
            }
        }
        result
    }

    val maxVal = (counts.maxOrNull() ?: 1).coerceAtLeast(5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "লাইব্রেরিতে উপস্থিতির চাপ (পিক আওয়ার)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "কোন সময়ে শিক্ষার্থীদের ভিড় বা চাপ সবচেয়ে বেশি",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val chartWidth = size.width
                val chartHeight = size.height - 30.dp.toPx()
                val barCount = timeSlots.size
                val barSpacing = chartWidth / (barCount * 1.5f)
                val barWidth = barSpacing * 0.7f

                // Draw baseline
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(0f, chartHeight),
                    end = Offset(chartWidth, chartHeight),
                    strokeWidth = 2f
                )

                for (i in 0 until barCount) {
                    val count = counts[i]
                    val barHeight = (count.toFloat() / maxVal) * chartHeight
                    val x = (i * (chartWidth / barCount)) + (chartWidth / barCount - barWidth) / 2f
                    val y = chartHeight - barHeight

                    // Color based on load
                    val barColor = when {
                        count >= maxVal * 0.75f -> Color(0xFFD32F2F) // high load
                        count >= maxVal * 0.4f -> Color(0xFFFF9800) // medium load
                        else -> Color(0xFF4CAF50) // low load
                    }

                    // Draw bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight.coerceAtLeast(8f)),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Draw count on top
                    drawContext.canvas.nativeCanvas.drawText(
                        count.toString(),
                        x + (barWidth / 2f),
                        y - 6f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )

                    // Draw time label
                    val label = timeSlots[i].replace(" ", "\n")
                    drawContext.canvas.nativeCanvas.drawText(
                        timeSlots[i].take(5),
                        x + (barWidth / 2f),
                        chartHeight + 18.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendChip(color = Color(0xFF4CAF50), label = "স্বাভাবিক চাপ")
                LegendChip(color = Color(0xFFFF9800), label = "মাঝারি চাপ")
                LegendChip(color = Color(0xFFD32F2F), label = "সর্বোচ্চ চাপ (Peak)")
            }
        }
    }
}

@Composable
fun DailyTrendChartCard(records: List<AttendanceRecord>, totalStudents: Int) {
    // Group records by Date
    val dateMap = remember(records) {
        records.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "দৈনিক উপস্থিতি হার (Daily Attendance Rate)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "প্রতিদিনের মোট শিক্ষার্থী উপস্থিতির শতকরা হিসাব",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (dateMap.isEmpty()) {
                Text(
                    text = "কোনো উপস্থিতির রেকর্ড পাওয়া যায়নি",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                dateMap.entries.take(7).forEach { entry ->
                    val presentCount = entry.value.size
                    val percentage = ((presentCount.toFloat() / totalStudents) * 100).toInt().coerceIn(0, 100)

                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = entry.key,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "$presentCount জন ($percentage%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(percentage / 100f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFD32F2F))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SingleStudentAnalyticsView(
    students: List<Student>,
    attendanceRecords: List<AttendanceRecord>,
    selectedLid: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectLid: (String) -> Unit
) {
    val selectedStudent = students.find { it.lid.equals(selectedLid, ignoreCase = true) }
    val studentRecords = remember(selectedLid, attendanceRecords) {
        attendanceRecords.filter { it.lid.equals(selectedLid, ignoreCase = true) }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("শিক্ষার্থীর নাম বা LID দিয়ে খুঁজুন...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Student Selector list if not selected
        if (selectedStudent == null) {
            val filtered = students.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.lid.contains(searchQuery, ignoreCase = true)
            }

            Text("যে কোনো শিক্ষার্থীর ওপর চাপ দিয়ে তার গ্রাফ ও তথ্য দেখুন:", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))

            filtered.take(6).forEach { st ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectLid(st.lid) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(st.name.take(1), fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(st.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("LID: ${st.lid} • Shelf: ${st.shelfNo}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        } else {
            // Selected Student Report Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(selectedStudent.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("LID: ${selectedStudent.lid} • Shelf: ${selectedStudent.shelfNo}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(
                            text = "পরিবর্তন",
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onSelectLid("") }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${studentRecords.size}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFD32F2F))
                            Text("মোট উপস্থিতি", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(selectedStudent.bloodGroup, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1E293B))
                            Text("রক্তের গ্রুপ", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (selectedStudent.isActive) "সক্রিয়" else "নিষ্ক্রিয়", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF2E7D32))
                            Text("সদস্যপদ", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("হাজিরা লগ হিস্ট্রি:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (studentRecords.isEmpty()) {
                        Text("এই শিক্ষার্থীর এখনো কোনো হাজিরা রেকর্ড পাওয়া যায়নি।", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        studentRecords.take(5).forEach { rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rec.date, fontSize = 12.sp, color = Color(0xFF1E293B))
                                Text("প্রবেশ: ${rec.startTime} | বের: ${rec.endTime.ifEmpty { "..." }}", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
    }
}
