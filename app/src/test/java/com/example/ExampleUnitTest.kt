package com.example

import com.example.data.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testStudentActiveState() {
        val activeStudent = Student(lid = "LID-001", name = "Nahid Hasan", active = "active")
        val inactiveStudent = Student(lid = "LID-002", name = "Rahim", active = "inactive")

        assertTrue(activeStudent.isActive)
        assertTrue(!inactiveStudent.isActive)
    }
}
