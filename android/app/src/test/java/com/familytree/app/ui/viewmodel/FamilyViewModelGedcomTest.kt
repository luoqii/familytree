package com.familytree.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w400dp-h800dp-xxhdpi")
class FamilyViewModelGedcomTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var app: Application
    private lateinit var viewModel: FamilyViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        viewModel = FamilyViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun awaitGedcomResult(): GedcomOperationState {
        return withTimeout(5000) {
            while (true) {
                val state = viewModel.gedcomState.value
                if (!state.isLoading && (state.errorMessage != null || state.successMessage != null)) {
                    return@withTimeout state
                }
                delay(50)
            }
            @Suppress("UNREACHABLE_CODE")
            viewModel.gedcomState.value
        }
    }

    @Test
    fun importGedcom_validContent_showsSuccess() = runBlocking {
        val gedcomContent = """
            0 HEAD
            1 SOUR Test
            1 GEDC
            2 VERS 5.5
            2 FORM LINEAGE-LINKED
            1 CHAR UTF-8
            0 @I1@ INDI
            1 NAME 测试 /人/
            1 SEX M
            0 TRLR
        """.trimIndent()

        val uri = Uri.parse("content://test.import.authority/import.ged")
        val shadow = Shadows.shadowOf(app.contentResolver)
        shadow.registerInputStream(uri, gedcomContent.byteInputStream())

        viewModel.importGedcom(uri)
        val state = awaitGedcomResult()

        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("1"))
        assertFalse(state.isLoading)
    }

    @Test
    fun importGedcom_securityException_showsPermissionError() = runBlocking {
        val uri = Uri.parse("content://test.security.authority/file.ged")
        val shadow = Shadows.shadowOf(app.contentResolver)
        shadow.registerInputStreamSupplier(uri) {
            throw SecurityException("Permission denied")
        }

        viewModel.importGedcom(uri)
        val state = awaitGedcomResult()

        assertEquals("没有文件访问权限，请重新选择文件", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun importGedcom_unreachableUri_showsErrorWithoutCrash() = runBlocking {
        val uri = Uri.parse("content://nonexistent.authority/file.ged")

        viewModel.importGedcom(uri)
        val state = awaitGedcomResult()

        assertNotNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun clearGedcomState_resetsState() = runBlocking {
        val uri = Uri.parse("content://test.clear.authority/file.ged")
        val shadow = Shadows.shadowOf(app.contentResolver)
        shadow.registerInputStream(
            uri,
            "0 HEAD\n0 @I1@ INDI\n1 NAME A /B/\n1 SEX M\n0 TRLR".byteInputStream()
        )

        viewModel.importGedcom(uri)
        awaitGedcomResult()

        viewModel.clearGedcomState()
        val clearedState = viewModel.gedcomState.value

        assertFalse(clearedState.isLoading)
        assertEquals(null, clearedState.successMessage)
        assertEquals(null, clearedState.errorMessage)
    }

    @Test
    fun importGedcom_multipleMembers_showsCorrectCount() = runBlocking {
        val gedcomContent = """
            0 HEAD
            1 SOUR Test
            0 @I1@ INDI
            1 NAME 大明 /张/
            1 SEX M
            0 @I2@ INDI
            1 NAME 秀英 /李/
            1 SEX F
            0 TRLR
        """.trimIndent()

        val uri = Uri.parse("content://test.multi.authority/import.ged")
        val shadow = Shadows.shadowOf(app.contentResolver)
        shadow.registerInputStream(uri, gedcomContent.byteInputStream())

        viewModel.importGedcom(uri)
        val state = awaitGedcomResult()

        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("2"))
    }

    @Test
    fun exportGedcom_completesWithoutCrash() = runBlocking {
        val uri = Uri.parse("content://test.export.authority/file.ged")
        val shadow = Shadows.shadowOf(app.contentResolver)
        shadow.registerOutputStream(uri, java.io.ByteArrayOutputStream())

        viewModel.exportGedcom(uri)
        val state = awaitGedcomResult()

        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("GEDCOM"))
        assertFalse(state.isLoading)
    }
}
