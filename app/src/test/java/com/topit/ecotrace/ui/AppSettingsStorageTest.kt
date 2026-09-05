package com.topit.ecotrace.ui

import android.content.Context
import android.content.SharedPreferences
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppSettingsStorageTest {

    private val context: Context = mockk()
    private val prefs: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk()

    @Before
    fun setUp() {
        every { context.getSharedPreferences("ecotrace_app_settings", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs
    }

    @Test
    fun getThemeMode_returnsSystemByDefault() {
        every { prefs.getString("theme_mode", ThemeMode.SYSTEM.name) } returns ThemeMode.SYSTEM.name

        assertEquals(ThemeMode.SYSTEM, storage().getThemeMode())
    }

    @Test
    fun getThemeMode_returnsStoredValue() {
        every { prefs.getString("theme_mode", ThemeMode.SYSTEM.name) } returns ThemeMode.DARK.name

        assertEquals(ThemeMode.DARK, storage().getThemeMode())
    }

    @Test
    fun getThemeMode_fallsBackToSystemForCorruptedValue() {
        every { prefs.getString("theme_mode", ThemeMode.SYSTEM.name) } returns "NOT_A_MODE"

        assertEquals(ThemeMode.SYSTEM, storage().getThemeMode())
    }

    @Test
    fun setThemeMode_persistsChosenValue() {
        storage().setThemeMode(ThemeMode.LIGHT)

        verify { editor.putString("theme_mode", ThemeMode.LIGHT.name) }
        verify { editor.apply() }
    }

    @Test
    fun getLanguage_returnsRussianByDefault() {
        every { prefs.getString("language", AppLanguage.RU.name) } returns AppLanguage.RU.name

        assertEquals(AppLanguage.RU, storage().getLanguage())
    }

    @Test
    fun getLanguage_returnsStoredValue() {
        every { prefs.getString("language", AppLanguage.RU.name) } returns AppLanguage.EN.name

        assertEquals(AppLanguage.EN, storage().getLanguage())
    }

    @Test
    fun getLanguage_fallsBackToRussianForCorruptedValue() {
        every { prefs.getString("language", AppLanguage.RU.name) } returns "NOT_A_LANGUAGE"

        assertEquals(AppLanguage.RU, storage().getLanguage())
    }

    @Test
    fun setLanguage_persistsChosenValue() {
        storage().setLanguage(AppLanguage.EN)

        verify { editor.putString("language", AppLanguage.EN.name) }
        verify { editor.apply() }
    }

    private fun storage() = AppSettingsStorage(context)
}
