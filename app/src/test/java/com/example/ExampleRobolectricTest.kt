package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.QrConfig
import com.example.data.Student
import com.example.data.local.PreferencesManager
import com.example.qr.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Job Express Attendance", appName)
    }

    @Test
    fun testQrEncodingAndDecoding() {
        val config = QrConfig(
            scriptUrl = "https://script.google.com/macros/s/AKfycbxTest/exec",
            latitude = 23.5462131,
            longitude = 89.1720178,
            radiusMeters = 300.0
        )
        val json = QrCodeGenerator.createQrDataJson(config)
        val parsed = QrCodeGenerator.parseQrData(json)

        assertNotNull(parsed)
        assertEquals("https://script.google.com/macros/s/AKfycbxTest/exec", parsed?.scriptUrl)
        assertEquals(23.5462131, parsed?.latitude ?: 0.0, 0.0001)
        assertEquals(89.1720178, parsed?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun testPreferencesManagerPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        val student = Student(
            lid = "LID-501",
            name = "Test Student",
            shelfNo = "B-03",
            phone = "01700000000",
            bloodGroup = "B+",
            active = "active"
        )
        prefs.saveStudent(student)

        val retrieved = prefs.getSavedStudent()
        assertNotNull(retrieved)
        assertEquals("LID-501", retrieved?.lid)
        assertEquals("Test Student", retrieved?.name)
    }
}
