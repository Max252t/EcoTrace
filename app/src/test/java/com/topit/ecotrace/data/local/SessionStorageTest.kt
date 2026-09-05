package com.topit.ecotrace.data.local

import android.content.Context
import android.content.SharedPreferences
import com.topit.ecotrace.domain.repository.AuthSession
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SessionStorageTest {

    private val context: Context = mockk()
    private val prefs: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk()

    @Before
    fun setUp() {
        every { context.getSharedPreferences("ecotrace_session", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.clear() } returns editor
        every { editor.apply() } just Runs
    }

    @Test
    fun read_returnsNullWhenNothingWasEverStored() {
        every { prefs.getString("token", null) } returns null

        val storage = storageWithEmptyPrefs()

        assertNull(storage.read())
        assertNull(storage.token())
        assertNull(storage.userId())
    }

    @Test
    fun read_restoresPreviouslyStoredSessionOnConstruction() {
        every { prefs.getString("token", null) } returns "token"
        every { prefs.getString("user_id", null) } returns "user-1"
        every { prefs.getString("email", "") } returns "user@example.com"
        every { prefs.getString("display_name", "") } returns "User"
        every { prefs.getString("role", "USER") } returns "USER"

        val storage = SessionStorage(context)

        assertEquals(SESSION, storage.read())
        assertEquals("token", storage.token())
        assertEquals("user-1", storage.userId())
    }

    @Test
    fun save_persistsSessionAndUpdatesReadState() {
        val storage = storageWithEmptyPrefs()

        storage.save(SESSION)

        verify {
            editor.putString("token", "token")
            editor.putString("user_id", "user-1")
            editor.putString("email", "user@example.com")
            editor.putString("display_name", "User")
            editor.putString("role", "USER")
            editor.apply()
        }
        assertEquals(SESSION, storage.read())
        assertEquals("token", storage.token())
    }

    @Test
    fun save_emitsUpdatedSessionOnSessionFlow() = runBlocking {
        val storage = storageWithEmptyPrefs()

        storage.save(SESSION)

        assertEquals(SESSION, storage.session.first())
    }

    @Test
    fun clear_removesPersistedSessionAndUpdatesReadState() {
        val storage = storageWithEmptyPrefs()
        storage.save(SESSION)

        storage.clear()

        verifyOrder {
            editor.putString(any(), any())
            editor.clear()
        }
        assertNull(storage.read())
        assertNull(storage.token())
        assertNull(storage.userId())
    }

    private fun storageWithEmptyPrefs(): SessionStorage {
        every { prefs.getString("token", null) } returns null
        return SessionStorage(context)
    }

    private companion object {
        val SESSION = AuthSession(
            token = "token",
            userId = "user-1",
            email = "user@example.com",
            displayName = "User",
            role = "USER",
        )
    }
}
