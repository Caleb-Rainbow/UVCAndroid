package com.herohan.uvcdemo.utils

import android.net.Uri
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SaveHelper {
    private val baseStoragePath: String
        get() = Environment.getExternalStorageDirectory().absolutePath + File.separator + "UVCDemo"

    fun getSavePhotoPath(): String {
        val dateFolder = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(Date()) + ".jpg"
        val dir = File(baseStoragePath, "$dateFolder/photo")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName).absolutePath
    }

    fun getSaveVideoPath(): String {
        val dateFolder = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(Date()) + ".mp4"
        val dir = File(baseStoragePath, "$dateFolder/video")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName).absolutePath
    }
}
