package com.aube.minimallog.data.database

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ImageStorage(
    private val appContext: Context,
    private val resolver: ContentResolver = appContext.contentResolver
) {
    suspend fun persist(uri: Uri): String = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "images").apply { if (!exists()) mkdirs() }
        val outFile = File(dir, "${UUID.randomUUID()}.jpg")
        resolver.openInputStream(uri).use { input ->
            outFile.outputStream().use { output ->
                input?.copyTo(output) ?: error("Could not read image stream")
            }
        }
        outFile.name
    }

    fun resolve(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val stored = File(path)
        return if (stored.isAbsolute) stored else File(File(appContext.filesDir, "images"), path)
    }

    suspend fun deleteFile(path: String?): Boolean = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext false
        runCatching { resolve(path)?.takeIf { it.exists() }?.delete() ?: false }.getOrDefault(false)
    }
}
