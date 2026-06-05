package com.herohan.uvcapp.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object SaveHelper {

    private const val PHOTO_DIR = "photo"
    private const val VIDEO_DIR = "video"

    /**
     * Returns a save path under the app-private external storage.
     * No MANAGE_EXTERNAL_STORAGE permission is needed.
     */
    fun getSavePhotoPath(context: Context): String {
        val dir = getSaveDir(context, PHOTO_DIR)
        val fileName = generateFileName("jpg")
        return File(dir, fileName).absolutePath
    }

    /**
     * Returns a save path under the app-private external storage.
     * No MANAGE_EXTERNAL_STORAGE permission is needed.
     */
    fun getSaveVideoPath(context: Context): String {
        val dir = getSaveDir(context, VIDEO_DIR)
        val fileName = generateFileName("mp4")
        return File(dir, fileName).absolutePath
    }

    private fun getSaveDir(context: Context, subDir: String): File {
        val dateFolder = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        // Use app-private external storage — no special permission required
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM)
            ?: File(context.filesDir, "USBCamera")
        val dir = File(File(baseDir, "USBCamera"), "$dateFolder/$subDir")
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w("SaveHelper", "Failed to create directory: ${dir.absolutePath}")
            // Fallback to internal storage if external storage is unavailable
            val fallback = File(context.filesDir, "USBCamera/$dateFolder/$subDir")
            fallback.mkdirs()
            return fallback
        }
        return dir
    }

    private fun generateFileName(extension: String): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(Date())
        val unique = UUID.randomUUID().toString().take(8)
        return "${timestamp}_$unique.$extension"
    }
}
