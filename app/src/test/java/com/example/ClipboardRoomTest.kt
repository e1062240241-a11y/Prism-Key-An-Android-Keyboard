package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClipboardRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: ClipboardRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ClipboardRepository(context, db.clipboardDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun clipboardHistory_storesPinsCategorizesAndClearsUnpinned() = runBlocking {
        repo.saveClip("https://developer.android.com", pinned = false)
        repo.saveClip("user@example.com", pinned = true)
        repo.saveClip("Meeting at 4 PM", pinned = false)

        val initial = repo.items.first()
        assertEquals(3, initial.size)
        // Pinned item should always be ordered first
        assertTrue(initial.first().isPinned)
        assertEquals("Email", initial.first().category)
        assertTrue(initial.any { it.category == "Link" && it.content == "https://developer.android.com" })

        // Mark used increments usage count
        val linkItem = initial.first { it.category == "Link" }
        repo.markUsedAndCopyToSystem(linkItem)
        val afterUse = repo.items.first().first { it.id == linkItem.id }
        assertEquals(1, afterUse.usageCount)

        // Clear unpinned retains only pinned entries
        repo.clearUnpinned()
        val remaining = repo.items.first()
        assertEquals(1, remaining.size)
        assertEquals("user@example.com", remaining.first().content)
        assertFalse(remaining.any { !it.isPinned })
    }
}
