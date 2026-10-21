package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Student
import com.example.data.local.PreferencesManager
import com.example.data.remote.GoogleSheetApiService
import kotlinx.coroutines.launch

@Composable
fun LidRegistrationDialog(
    preferencesManager: PreferencesManager,
    apiService: GoogleSheetApiService,
    onRegistered: (Student) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputLid by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHelplineCall by remember { mutableStateOf(false) }

    val helplineNumber = "01351835060"

    AlertDialog(
        onDismissRequest = {
            if (onDismiss != null) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "স্টুডেন্ট আইডি (LID) ভেরিফিকেশন",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Job Express Library Management",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "অ্যাপে হাজিরা দেওয়ার জন্য আপনার লাইব্রেরি আইডি (LID) প্রবেশ করুন। এটি একবার ভেরিফাই করলেই অ্যাপে স্থায়ীভাবে সেভ হয়ে যাবে।",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = inputLid,
                    onValueChange = {
                        inputLid = it
                        errorMessage = null
                        showHelplineCall = false
                    },
                    label = { Text("আপনার LID নাম্বার লিখুন") },
                    placeholder = { Text("যেমন: LID-101 বা 101") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = Color(0xFFD32F2F))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("lid_input_field")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (showHelplineCall) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$helplineNumber")
                            }
                            try {
                                context.startActivity(dialIntent)
                            } catch (e: Exception) {
                                // ignore
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth().testTag("call_helpline_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "লাইব্রেরিতে কল করুন ($helplineNumber)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lidTrimmed = inputLid.trim()
                    if (lidTrimmed.isBlank()) {
                        errorMessage = "অনুগ্রহ করে একটি সঠিক LID প্রবেশ করান"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    showHelplineCall = false

                    scope.launch {
                        val qrConfig = preferencesManager.getQrConfig()
                        if (qrConfig.scriptUrl.isBlank()) {
                            // If script URL is not configured yet, notify user to scan QR first or configure
                            isLoading = false
                            errorMessage = "সিস্টেমের গুগল শীট লিংক পাওয়া যায়নি। দেয়ালের QR কোড স্ক্যান করে পুনরায় চেষ্টা করুন।"
                            showHelplineCall = true
                            return@launch
                        }

                        val result = apiService.getStudentByLid(qrConfig.scriptUrl, lidTrimmed)
                        isLoading = false
                        result.onSuccess { student ->
                            if (student.isActive) {
                                preferencesManager.saveStudent(student)
                                onRegistered(student)
                            } else {
                                errorMessage = "আপনার সদস্যপদ বর্তমানে নিষ্ক্রিয় (Inactive)। লাইব্রেরি হেল্পলাইনে যোগাযোগ করুন।"
                                showHelplineCall = true
                            }
                        }.onFailure { ex ->
                            errorMessage = "স্টুডেন্ট আইডি সঠিক নয়। অনুগ্রহ করে হেল্পলাইনে যোগাযোগ করুন।"
                            showHelplineCall = true
                        }
                    }
                },
                enabled = !isLoading && inputLid.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.testTag("verify_lid_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("যাচাই হচ্ছে...")
                } else {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ভেরিফাই করুন")
                }
            }
        },
        dismissButton = {
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text("পরে করব")
                }
            }
        }
    )
}
