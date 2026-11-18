package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.R
import com.example.data.local.StickerDao
import com.example.data.local.StickerEntity
import com.example.data.local.StickerPackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class StickerRepository(
    private val context: Context,
    private val stickerDao: StickerDao,
) {
    val packs: Flow<List<StickerPackEntity>> = stickerDao.observePacks()
    val allStickers: Flow<List<StickerEntity>> = stickerDao.observeAllStickers()

    suspend fun ensureSeeded() {
        if (stickerDao.packCount() == 0) {
            stickerDao.createPackWithStickers(
                pack = StickerPackEntity(
                    name = "Friendly Mascots",
                    description = "Fun 3D stickers saved right on your phone",
                    isBuiltIn = true,
                ),
                stickers = listOf(
                    StickerEntity(
                        packId = 0,
                        label = "Hello Wave",
                        drawableResName = "img_sticker_wave",
                        emojiFallback = "👋",
                    ),
                    StickerEntity(
                        packId = 0,
                        label = "Rocket Spark",
                        drawableResName = "img_sticker_spark",
                        emojiFallback = "🚀",
                    ),
                    StickerEntity(
                        packId = 0,
                        label = "Coffee Break",
                        drawableResName = "img_sticker_coffee",
                        emojiFallback = "☕",
                    ),
                ),
            )

            stickerDao.createPackWithStickers(
                pack = StickerPackEntity(
                    name = "Quick Reactions",
                    description = "Expressive badges for any conversation",
                    isBuiltIn = true,
                ),
                stickers = listOf(
                    StickerEntity(packId = 0, label = "100% Yes", emojiFallback = "💯"),
                    StickerEntity(packId = 0, label = "On Fire", emojiFallback = "🔥"),
                    StickerEntity(packId = 0, label = "Big Brain", emojiFallback = "🧠"),
                    StickerEntity(packId = 0, label = "Heart Eyes", emojiFallback = "😍"),
                    StickerEntity(packId = 0, label = "Mind Blown", emojiFallback = "🤯"),
                    StickerEntity(packId = 0, label = "Party Time", emojiFallback = "🥳"),
                ),
            )
        }
    }

    suspend fun createCustomPack(name: String, description: String): Long {
        return stickerDao.insertPack(
            StickerPackEntity(
                name = name.trim().ifEmpty { "My Stickers" },
                description = description.trim().ifEmpty { "Custom local sticker pack" },
                isBuiltIn = false,
            ),
        )
    }

    suspend fun importStickerFromUri(
        packId: Long,
        uri: Uri,
        label: String,
    ): Result<StickerEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val stickerDir = File(context.filesDir, "stickers").apply { mkdirs() }
            val outFile = File(stickerDir, "sticker_${UUID.randomUUID()}.png")

            context.contentResolver.openInputStream(uri)?.use { input ->
                val decoded = BitmapFactory.decodeStream(input)
                    ?: error("Unable to decode selected image")
                val scaled = scaleBitmap(decoded, maxDim = 512)
                FileOutputStream(outFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                if (scaled !== decoded) decoded.recycle()
                scaled.recycle()
            } ?: error("Cannot open selected image")

            val entity = StickerEntity(
                packId = packId,
                label = label.trim().ifEmpty { "Custom Sticker" },
                localFilePath = outFile.absolutePath,
                emojiFallback = "🖼️",
            )
            val id = stickerDao.insertSticker(entity)
            entity.copy(id = id)
        }
    }

    suspend fun addEmojiSticker(packId: Long, emoji: String, label: String) {
        stickerDao.insertSticker(
            StickerEntity(
                packId = packId,
                label = label.trim().ifEmpty { "Sticker" },
                emojiFallback = emoji.ifBlank { "✨" },
            ),
        )
    }

    suspend fun deleteCustomPack(packId: Long) {
        stickerDao.deleteCustomPack(packId)
    }

    suspend fun deleteSticker(sticker: StickerEntity) {
        sticker.localFilePath?.let { path ->
            runCatching { File(path).delete() }
        }
        stickerDao.deleteSticker(sticker.id)
    }

    suspend fun resolveShareableFile(sticker: StickerEntity): File? = withContext(Dispatchers.IO) {
        sticker.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) return@withContext file
        }
        val resId = resolveDrawableId(sticker.drawableResName) ?: return@withContext null
        val cacheDir = File(context.cacheDir, "stickers").apply { mkdirs() }
        val outFile = File(cacheDir, "${sticker.drawableResName}.png")
        if (!outFile.exists()) {
            val bitmap = BitmapFactory.decodeResource(context.resources, resId) ?: return@withContext null
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
            }
            bitmap.recycle()
        }
        outFile
    }

    fun resolveDrawableId(name: String?): Int? {
        return when (name) {
            "img_sticker_wave" -> R.drawable.img_sticker_wave
            "img_sticker_spark" -> R.drawable.img_sticker_spark
            "img_sticker_coffee" -> R.drawable.img_sticker_coffee
            else -> null
        }
    }

    private fun scaleBitmap(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source
        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val newW = (width * ratio).toInt().coerceAtLeast(1)
        val newH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
