package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.QrConfig
import com.example.data.Student
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("job_express_prefs", Context.MODE_PRIVATE)
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val studentAdapter = moshi.adapter(Student::class.java)
    private val qrConfigAdapter = moshi.adapter(QrConfig::class.java)

    companion object {
        private const val KEY_STUDENT_LID = "student_lid"
        private const val KEY_STUDENT_JSON = "student_json"
        private const val KEY_QR_CONFIG_JSON = "qr_config_json"
        private const val KEY_SCRIPT_URL = "script_url"
        private const val KEY_LATITUDE = "library_lat"
        private const val KEY_LONGITUDE = "library_lng"
        private const val KEY_RADIUS = "library_radius"
        private const val KEY_LAST_STATUS = "last_attendance_status"
        private const val KEY_LAST_DATE = "last_attendance_date"
        private const val KEY_LAST_START_TIME = "last_start_time"
        private const val KEY_LAST_END_TIME = "last_end_time"
    }

    fun getStudentLid(): String? {
        return prefs.getString(KEY_STUDENT_LID, null)?.takeIf { it.isNotBlank() }
    }

    fun saveStudent(student: Student) {
        prefs.edit()
            .putString(KEY_STUDENT_LID, student.lid)
            .putString(KEY_STUDENT_JSON, studentAdapter.toJson(student))
            .apply()
    }

    fun getSavedStudent(): Student? {
        val json = prefs.getString(KEY_STUDENT_JSON, null) ?: return null
        return try {
            studentAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun clearStudent() {
        prefs.edit()
            .remove(KEY_STUDENT_LID)
            .remove(KEY_STUDENT_JSON)
            .apply()
    }

    fun getQrConfig(): QrConfig {
        val json = prefs.getString(KEY_QR_CONFIG_JSON, null)
        if (json != null) {
            try {
                val cfg = qrConfigAdapter.fromJson(json)
                if (cfg != null && cfg.scriptUrl.isNotBlank()) return cfg
            } catch (e: Exception) {
                // Ignore
            }
        }
        val url = prefs.getString(KEY_SCRIPT_URL, "") ?: ""
        val lat = prefs.getFloat(KEY_LATITUDE, 23.5462131f).toDouble()
        val lng = prefs.getFloat(KEY_LONGITUDE, 89.1720178f).toDouble()
        val radius = prefs.getFloat(KEY_RADIUS, 300f).toDouble()
        return QrConfig(scriptUrl = url, latitude = lat, longitude = lng, radiusMeters = radius)
    }

    fun saveQrConfig(config: QrConfig) {
        prefs.edit()
            .putString(KEY_QR_CONFIG_JSON, qrConfigAdapter.toJson(config))
            .putString(KEY_SCRIPT_URL, config.scriptUrl)
            .putFloat(KEY_LATITUDE, config.latitude.toFloat())
            .putFloat(KEY_LONGITUDE, config.longitude.toFloat())
            .putFloat(KEY_RADIUS, config.radiusMeters.toFloat())
            .apply()
    }

    fun updateTodayAttendance(date: String, type: String, time: String) {
        val editor = prefs.edit()
        editor.putString(KEY_LAST_DATE, date)
        if (type.equals("ENTRY", ignoreCase = true)) {
            editor.putString(KEY_LAST_STATUS, "Inside")
            editor.putString(KEY_LAST_START_TIME, time)
        } else if (type.equals("EXIT", ignoreCase = true)) {
            editor.putString(KEY_LAST_STATUS, "Left")
            editor.putString(KEY_LAST_END_TIME, time)
        }
        editor.apply()
    }

    fun getTodayStatus(todayDate: String): String {
        val savedDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        if (savedDate != todayDate) return "None"
        return prefs.getString(KEY_LAST_STATUS, "None") ?: "None"
    }

    fun getTodayEntryTime(todayDate: String): String? {
        val savedDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        if (savedDate != todayDate) return null
        return prefs.getString(KEY_LAST_START_TIME, null)
    }

    fun getTodayExitTime(todayDate: String): String? {
        val savedDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        if (savedDate != todayDate) return null
        return prefs.getString(KEY_LAST_END_TIME, null)
    }
}
