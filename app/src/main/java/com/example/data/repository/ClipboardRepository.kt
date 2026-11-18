package com.example.data.repository

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.data.local.ClipboardDao
import com.example.data.local.ClipboardItemEntity
import kotlinx.coroutines.flow.Flow

class ClipboardRepository(
    private val context: Context,
    private val clipboardDao: ClipboardDao,
) {
    val items: Flow<List<ClipboardItemEntity>> = clipboardDao.observeAll()

    suspend fun ensureSeeded() {
        if (clipboardDao.count() == 0) {
            val now = System.currentTimeMillis()
            clipboardDao.insert(
                ClipboardItemEntity(
                    content = "On my way! Be there in 10 minutes.",
                    category = "Quick Reply",
                    isPinned = true,
                    timestamp = now - 60_000L,
                ),
            )
            clipboardDao.insert(
                ClipboardItemEntity(
                    content = "Let's sync tomorrow morning at 9:30 AM.",
                    category = "Quick Reply",
                    isPinned = true,
                    timestamp = now - 120_000L,
                ),
            )
            clipboardDao.insert(
                ClipboardItemEntity(
                    content = "Thanks for sending that over — reviewing now!",
                    category = "Text",
                    isPinned = false,
                    timestamp = now - 300_000L,
                ),
            )
        }
    }

    suspend fun saveClip(text: String, pinned: Boolean = false) {
        val cleaned = text.trim()
        if (cleaned.isBlank()) return
        val existing = clipboardDao.findByContent(cleaned)
        if (existing != null) {
            clipboardDao.markUsed(existing.id)
            if (pinned && !existing.isPinned) {
                clipboardDao.setPinned(existing.id, true)
            }
            return
        }
        clipboardDao.insert(
            ClipboardItemEntity(
                content = cleaned,
                category = detectCategory(cleaned),
                isPinned = pinned,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun togglePin(item: ClipboardItemEntity) {
        clipboardDao.setPinned(item.id, !item.isPinned)
    }

    suspend fun markUsedAndCopyToSystem(item: ClipboardItemEntity) {
        clipboardDao.markUsed(item.id)
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("PrismKeys Clip", item.content))
    }

    suspend fun captureSystemClipboardIfPresent() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = cm.primaryClip ?: return
        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                saveClip(text)
            }
        }
    }

    suspend fun deleteClip(id: Long) {
        clipboardDao.deleteById(id)
    }

    suspend fun clearUnpinned() {
        clipboardDao.clearUnpinned()
    }

    private fun detectCategory(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") || lower.contains("www.") -> "Link"
            text.contains("@") && text.contains(".") && !text.contains(" ") -> "Email"
            text.count { it.isDigit() } >= 7 && text.length <= 20 -> "Number"
            text.contains("{") || text.contains("fun ") || text.contains("val ") || text.contains("->") -> "Code"
            else -> "Text"
        }
    }
}
