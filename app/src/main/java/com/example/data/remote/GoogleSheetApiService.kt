package com.example.data.remote

import com.example.data.AttendanceRecord
import com.example.data.AttendanceResult
import com.example.data.LibraryStats
import com.example.data.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleSheetApiService {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getStats(scriptUrl: String): Result<LibraryStats> = withContext(Dispatchers.IO) {
        try {
            if (scriptUrl.isBlank()) {
                return@withContext Result.failure(Exception("গুগল শীট অ্যাপস স্ক্রিপ্ট লিংক কনফিগার করা হয়নি। অ্যাডমিন প্যানেল থেকে লিংক যুক্ত করুন।"))
            }
            val urlBuilder = scriptUrl.toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(Exception("অবৈধ স্ক্রিপ্ট ইউআরএল"))
            urlBuilder.addQueryParameter("action", "get_stats")

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("সার্ভার ত্রুটি: ${response.code}"))
            }

            val json = JSONObject(body)
            if (json.optBoolean("success", false)) {
                val statsJson = json.optJSONObject("stats") ?: JSONObject()
                val stats = LibraryStats(
                    totalStudents = statsJson.optInt("totalStudents", 0),
                    totalSeats = statsJson.optInt("totalSeats", 150),
                    activeSeats = statsJson.optInt("activeSeats", 150),
                    todayTotalPresent = statsJson.optInt("todayTotalPresent", 0),
                    todayCurrentlyInside = statsJson.optInt("todayCurrentlyInside", 0),
                    availableSeats = statsJson.optInt("availableSeats", 150),
                    todayAbsent = statsJson.optInt("todayAbsent", 0),
                    date = statsJson.optString("date", "")
                )
                Result.success(stats)
            } else {
                Result.failure(Exception(json.optString("error", "পরিসংখ্যান লোড করতে ব্যর্থ")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudents(scriptUrl: String): Result<List<Student>> = withContext(Dispatchers.IO) {
        try {
            if (scriptUrl.isBlank()) {
                return@withContext Result.failure(Exception("গুগল শীট অ্যাপস স্ক্রিপ্ট লিংক কনফিগার করা হয়নি"))
            }
            val urlBuilder = scriptUrl.toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(Exception("অবৈধ স্ক্রিপ্ট ইউআরএল"))
            urlBuilder.addQueryParameter("action", "get_students")

            val request = Request.Builder().url(urlBuilder.build()).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            val json = JSONObject(body)
            if (json.optBoolean("success", false)) {
                val array = json.optJSONArray("students") ?: JSONArray()
                val list = mutableListOf<Student>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    list.add(
                        Student(
                            lid = item.optString("lid"),
                            name = item.optString("name"),
                            shelfNo = item.optString("shelfNo"),
                            phone = item.optString("phone"),
                            bloodGroup = item.optString("bloodGroup", "A+"),
                            email = item.optString("email"),
                            address = item.optString("address"),
                            photoUrl = item.optString("photoUrl"),
                            active = item.optString("active", "active"),
                            createdAt = item.optString("createdAt")
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception(json.optString("message", "শিক্ষার্থীদের তালিকা পাওয়া যায়নি")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudentByLid(scriptUrl: String, lid: String): Result<Student> = withContext(Dispatchers.IO) {
        try {
            if (scriptUrl.isBlank()) {
                return@withContext Result.failure(Exception("গুগল শীট অ্যাপস স্ক্রিপ্ট লিংক নেই"))
            }
            val urlBuilder = scriptUrl.toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(Exception("অবৈধ স্ক্রিপ্ট ইউআরএল"))
            urlBuilder.addQueryParameter("action", "get_student")
            urlBuilder.addQueryParameter("lid", lid)

            val request = Request.Builder().url(urlBuilder.build()).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            val json = JSONObject(body)
            if (json.optBoolean("success", false)) {
                val item = json.getJSONObject("student")
                val student = Student(
                    lid = item.optString("lid"),
                    name = item.optString("name"),
                    shelfNo = item.optString("shelfNo"),
                    phone = item.optString("phone"),
                    bloodGroup = item.optString("bloodGroup", "A+"),
                    email = item.optString("email"),
                    address = item.optString("address"),
                    photoUrl = item.optString("photoUrl"),
                    active = item.optString("active", "active"),
                    createdAt = item.optString("createdAt")
                )
                Result.success(student)
            } else {
                Result.failure(Exception(json.optString("message", "স্টুডেন্ট আইডি পাওয়া যায়নি")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordAttendance(scriptUrl: String, lid: String): Result<AttendanceResult> = withContext(Dispatchers.IO) {
        try {
            if (scriptUrl.isBlank()) {
                return@withContext Result.failure(Exception("গুগল শীট অ্যাপস স্ক্রিপ্ট লিংক নেই। কিউআর কোড স্ক্যান করুন।"))
            }
            val urlBuilder = scriptUrl.toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(Exception("অবৈধ স্ক্রিপ্ট ইউআরএল"))
            urlBuilder.addQueryParameter("action", "record_attendance")
            urlBuilder.addQueryParameter("lid", lid)

            val request = Request.Builder().url(urlBuilder.build()).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            val json = JSONObject(body)
            if (json.optBoolean("success", false)) {
                val type = json.optString("type", "ENTRY")
                val time = json.optString("time", "")
                val date = json.optString("date", "")
                val msg = json.optString("message", "হাজিরা সফলভাবে রেকর্ড হয়েছে")

                val studentObj = json.optJSONObject("student")
                val student = if (studentObj != null) {
                    Student(
                        lid = studentObj.optString("lid"),
                        name = studentObj.optString("name"),
                        shelfNo = studentObj.optString("shelfNo"),
                        phone = studentObj.optString("phone"),
                        bloodGroup = studentObj.optString("bloodGroup", "A+"),
                        email = studentObj.optString("email"),
                        address = studentObj.optString("address"),
                        photoUrl = studentObj.optString("photoUrl"),
                        active = studentObj.optString("active", "active")
                    )
                } else null

                Result.success(
                    AttendanceResult(
                        success = true,
                        type = type,
                        time = time,
                        date = date,
                        student = student,
                        message = msg
                    )
                )
            } else {
                val errMsg = json.optString("message", "হাজিরা রেকর্ড করা সম্ভব হয়নি")
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceLogs(scriptUrl: String): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        try {
            if (scriptUrl.isBlank()) {
                return@withContext Result.failure(Exception("গুগল শীট স্ক্রিপ্ট লিংক নেই"))
            }
            val urlBuilder = scriptUrl.toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(Exception("অবৈধ স্ক্রিপ্ট ইউআরএল"))
            urlBuilder.addQueryParameter("action", "get_attendance")

            val request = Request.Builder().url(urlBuilder.build()).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            val json = JSONObject(body)
            if (json.optBoolean("success", false)) {
                val array = json.optJSONArray("records") ?: JSONArray()
                val list = mutableListOf<AttendanceRecord>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    list.add(
                        AttendanceRecord(
                            lid = item.optString("lid"),
                            name = item.optString("name"),
                            date = item.optString("date"),
                            startTime = item.optString("startTime"),
                            endTime = item.optString("endTime"),
                            photoUrl = item.optString("photoUrl"),
                            status = item.optString("status", "Inside")
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception(json.optString("error", "হাজিরা লগ পাওয়া যায়নি")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStudent(scriptUrl: String, student: Student): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (scriptUrl.isBlank()) {
                return@withContext Result.failure(Exception("গুগল শীট স্ক্রিপ্ট লিংক নেই"))
            }
            val jsonPayload = JSONObject().apply {
                put("action", "add_student")
                put("lid", student.lid)
                put("name", student.name)
                put("shelfNo", student.shelfNo)
                put("phone", student.phone)
                put("bloodGroup", student.bloodGroup)
                put("email", student.email)
                put("address", student.address)
                put("photoUrl", student.photoUrl)
                put("active", student.active)
                put("createdAt", student.createdAt)
            }

            // Using GET query or POST with JSON
            val urlBuilder = scriptUrl.toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(Exception("অবৈধ স্ক্রিপ্ট ইউআরএল"))
            urlBuilder.addQueryParameter("action", "add_student")
            urlBuilder.addQueryParameter("lid", student.lid)
            urlBuilder.addQueryParameter("name", student.name)
            urlBuilder.addQueryParameter("shelfNo", student.shelfNo)
            urlBuilder.addQueryParameter("phone", student.phone)
            urlBuilder.addQueryParameter("bloodGroup", student.bloodGroup)
            urlBuilder.addQueryParameter("email", student.email)
            urlBuilder.addQueryParameter("address", student.address)
            urlBuilder.addQueryParameter("photoUrl", student.photoUrl)
            urlBuilder.addQueryParameter("active", student.active)
            urlBuilder.addQueryParameter("createdAt", student.createdAt)

            val request = Request.Builder().url(urlBuilder.build()).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            val json = JSONObject(body)
            if (json.optBoolean("success", false)) {
                Result.success(json.optString("message", "শিক্ষার্থী সফলভাবে যুক্ত হয়েছে"))
            } else {
                Result.failure(Exception(json.optString("message", "যুক্ত করা সম্ভব হয়নি")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(scriptUrl: String, lid: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (scriptUrl.isBlank()) {
                return@withContext Result.failure(Exception("গুগল শীট স্ক্রিপ্ট লিংক নেই"))
            }
            val urlBuilder = scriptUrl.toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(Exception("অবৈধ স্ক্রিপ্ট ইউআরএল"))
            urlBuilder.addQueryParameter("action", "delete_student")
            urlBuilder.addQueryParameter("lid", lid)

            val request = Request.Builder().url(urlBuilder.build()).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            val json = JSONObject(body)
            if (json.optBoolean("success", false)) {
                Result.success(json.optString("message", "শিক্ষার্থী মুছে ফেলা হয়েছে"))
            } else {
                Result.failure(Exception(json.optString("message", "ডিলিট করা সম্ভব হয়নি")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
