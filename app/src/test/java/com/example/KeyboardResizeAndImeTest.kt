package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethod
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.KeyboardPreferencesRepository
import com.example.ui.theme.KeyboardKeyShape
import com.example.ui.theme.KeyboardThemePreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KeyboardResizeAndImeTest {

    @Test
    fun imeService_isRegisteredForSystemWideInputMethod() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(InputMethod.SERVICE_INTERFACE)
        val services = context.packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
        val prismIme = services.firstOrNull { it.serviceInfo.packageName == context.packageName }

        assertTrue("Expected PrismKeysImeService to be discoverable by Android system IME", prismIme != null)
        assertEquals("android.permission.BIND_INPUT_METHOD", prismIme!!.serviceInfo.permission)
        assertTrue("Expected IME service to be exported", prismIme.serviceInfo.exported)
    }

    @Test
    fun preferencesRepository_persistsResizeBoundsAndReset() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = KeyboardPreferencesRepository(context)

        repo.setThemePreset(KeyboardThemePreset.MATERIAL_BLUE)
        repo.setKeyShape(KeyboardKeyShape.ROUNDED)
        repo.setKeyboardHeightScale(1.30f)
        repo.setKeyboardWidthScale(0.88f)
        repo.setKeyboardBottomOffsetDp(24)

        val updated = repo.settingsFlow.first()
        assertEquals(KeyboardThemePreset.MATERIAL_BLUE, updated.themePreset)
        assertEquals(1.30f, updated.keyboardHeightScale, 0.001f)
        assertEquals(0.88f, updated.keyboardWidthScale, 0.001f)
        assertEquals(24, updated.keyboardBottomOffsetDp)

        // Verify clamping bounds
        repo.setKeyboardHeightScale(2.5f)
        repo.setKeyboardWidthScale(0.2f)
        repo.setKeyboardBottomOffsetDp(99)
        val clamped = repo.settingsFlow.first()
        assertEquals(1.35f, clamped.keyboardHeightScale, 0.001f)
        assertEquals(0.82f, clamped.keyboardWidthScale, 0.001f)
        assertEquals(32, clamped.keyboardBottomOffsetDp)

        // Verify reset restores defaults
        repo.resetKeyboardSize()
        val reset = repo.settingsFlow.first()
        assertEquals(1.0f, reset.keyboardHeightScale, 0.001f)
        assertEquals(1.0f, reset.keyboardWidthScale, 0.001f)
        assertEquals(0, reset.keyboardBottomOffsetDp)
    }
}
