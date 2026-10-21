package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Student
import com.example.data.remote.GoogleSheetApiService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddStudentDialog(
    scriptUrl: String,
    apiService: GoogleSheetApiService,
    onDismiss: () -> Unit,
    onStudentAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var lid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var shelfNo by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    var selectedBloodGroup by remember { mutableStateOf("A+") }
    var bloodGroupExpanded by remember { mutableStateOf(false) }

    val activeOptions = listOf("active", "inactive")
    var selectedActive by remember { mutableStateOf("active") }
    var activeExpanded by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "নতুন শিক্ষার্থী যুক্ত করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                OutlinedTextField(
                    value = lid,
                    onValueChange = { lid = it },
                    label = { Text("LID (Library ID) *") },
                    placeholder = { Text("LID-101") },
                    modifier = Modifier.fillMaxWidth().testTag("add_student_lid_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("শিক্ষার্থীর নাম *") },
                    placeholder = { Text("যেমন: Nahid Hasan") },
                    modifier = Modifier.fillMaxWidth().testTag("add_student_name_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = shelfNo,
                        onValueChange = { shelfNo = it },
                        label = { Text("শেল্ফ নম্বর") },
                        placeholder = { Text("A-12") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Blood Group Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedBloodGroup,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("রক্তের গ্রুপ") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { bloodGroupExpanded = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { bloodGroupExpanded = true }
                                .testTag("blood_group_dropdown")
                        )
                        DropdownMenu(
                            expanded = bloodGroupExpanded,
                            onDismissRequest = { bloodGroupExpanded = false }
                        ) {
                            bloodGroups.forEach { bg ->
                                DropdownMenuItem(
                                    text = { Text(bg) },
                                    onClick = {
                                        selectedBloodGroup = bg
                                        bloodGroupExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("ফোন নম্বর") },
                    placeholder = { Text("017XXXXXXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ইমেইল") },
                    placeholder = { Text("student@gmail.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা") },
                    placeholder = { Text("বরিশাল, বাংলাদেশ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it },
                    label = { Text("Photo URL (ঐচ্ছিক)") },
                    placeholder = { Text("https://example.com/photo.jpg") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Active Status Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedActive == "active") "Active (সক্রিয়)" else "Inactive (নিষ্ক্রিয়)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("সদস্যপদ স্ট্যাটাস (Active)") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { activeExpanded = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeExpanded = true }
                            .testTag("active_status_dropdown")
                    )
                    DropdownMenu(
                        expanded = activeExpanded,
                        onDismissRequest = { activeExpanded = false }
                    ) {
                        activeOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(if (opt == "active") "Active (সক্রিয়)" else "Inactive (নিষ্ক্রিয়)") },
                                onClick = {
                                    selectedActive = opt
                                    activeExpanded = false
                                }
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFC62828),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lidT = lid.trim()
                    val nameT = name.trim()
                    if (lidT.isBlank() || nameT.isBlank()) {
                        errorMessage = "LID এবং শিক্ষার্থীর নাম আবশ্যক"
                        return@Button
                    }
                    isSubmitting = true
                    errorMessage = null

                    val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    val newStudent = Student(
                        lid = lidT,
                        name = nameT,
                        shelfNo = shelfNo.trim(),
                        phone = phone.trim(),
                        bloodGroup = selectedBloodGroup,
                        email = email.trim(),
                        address = address.trim(),
                        photoUrl = photoUrl.trim(),
                        active = selectedActive,
                        createdAt = nowStr
                    )

                    scope.launch {
                        val result = apiService.addStudent(scriptUrl, newStudent)
                        isSubmitting = false
                        result.onSuccess {
                            onStudentAdded()
                            onDismiss()
                        }.onFailure { ex ->
                            errorMessage = ex.localizedMessage ?: "যুক্ত করতে সমস্যা হয়েছে"
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.testTag("submit_add_student_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সংরক্ষণ হচ্ছে...")
                } else {
                    Text("সংরক্ষণ করুন")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
