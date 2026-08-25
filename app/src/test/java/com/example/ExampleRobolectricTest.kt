package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.TeacherPlannerRepository
import com.example.util.QrBarcodeUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("هاكر التدريس", appName)
    }

    @Test
    fun `generate QR code bitmap creates valid image`() {
        val bitmap = QrBarcodeUtils.generateQrBitmap("STUDENT_ID:100", 200)
        assertNotNull(bitmap)
        assertEquals(200, bitmap.width)
        assertEquals(200, bitmap.height)
    }

    @Test
    fun `repository sample data and operations work properly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getInstance(context)
        val repository = TeacherPlannerRepository(database)

        var teacher = repository.getTeacherSync()
        if (teacher == null) {
            repository.populateSampleData()
            teacher = repository.getTeacherSync()
        }
        assertNotNull(teacher)
        assertTrue(teacher?.name?.isNotEmpty() == true)
    }
}
