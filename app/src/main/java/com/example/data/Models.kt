package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Student(
    @Json(name = "lid") val lid: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "shelfNo") val shelfNo: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "bloodGroup") val bloodGroup: String = "A+",
    @Json(name = "email") val email: String = "",
    @Json(name = "address") val address: String = "",
    @Json(name = "photoUrl") val photoUrl: String = "",
    @Json(name = "active") val active: String = "active",
    @Json(name = "createdAt") val createdAt: String = ""
) {
    val isActive: Boolean
        get() = active.equals("active", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class AttendanceRecord(
    @Json(name = "lid") val lid: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "date") val date: String = "",
    @Json(name = "startTime") val startTime: String = "",
    @Json(name = "endTime") val endTime: String = "",
    @Json(name = "photoUrl") val photoUrl: String = "",
    @Json(name = "status") val status: String = "Inside"
) {
    val isInside: Boolean
        get() = status.equals("Inside", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class LibraryStats(
    @Json(name = "totalStudents") val totalStudents: Int = 0,
    @Json(name = "totalSeats") val totalSeats: Int = 150,
    @Json(name = "activeSeats") val activeSeats: Int = 150,
    @Json(name = "todayTotalPresent") val todayTotalPresent: Int = 0,
    @Json(name = "todayCurrentlyInside") val todayCurrentlyInside: Int = 0,
    @Json(name = "availableSeats") val availableSeats: Int = 150,
    @Json(name = "todayAbsent") val todayAbsent: Int = 0,
    @Json(name = "date") val date: String = ""
)

@JsonClass(generateAdapter = true)
data class QrConfig(
    @Json(name = "scriptUrl") val scriptUrl: String = "",
    @Json(name = "latitude") val latitude: Double = 23.5462131,
    @Json(name = "longitude") val longitude: Double = 89.1720178,
    @Json(name = "radiusMeters") val radiusMeters: Double = 300.0
)

data class AttendanceResult(
    val success: Boolean,
    val type: String = "", // "ENTRY" or "EXIT"
    val time: String = "",
    val date: String = "",
    val student: Student? = null,
    val message: String = ""
)
