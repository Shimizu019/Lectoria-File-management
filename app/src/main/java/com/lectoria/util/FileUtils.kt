package com.lectoria.util

import android.webkit.MimeTypeMap

/**
 * Small formatting helpers shared by the screens.
 */
object FileUtils {

    /** 512 -> "512 B", 2048 -> "2 KB", 1048576 -> "1 MB" */
    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes < BYTES_IN_KB) return "$sizeInBytes B"
        if (sizeInBytes < BYTES_IN_MB) return "${sizeInBytes.toDouble() / BYTES_IN_KB} KB"
        if (sizeInBytes < BYTES_IN_GB) return "${sizeInBytes.toDouble() / BYTES_IN_MB} MB"
        return "${sizeInBytes.toDouble() / BYTES_IN_GB} GB"
    }

    /** Short type shown next to the file name, e.g. "PDF" or "FILE". */
    fun typeLabel(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return if (extension.isEmpty()) "FILE" else extension.uppercase()
    }

    fun mimeTypeOf(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) return FALLBACK_MIME_TYPE
        return MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension)
            ?: FALLBACK_MIME_TYPE
    }

    private const val BYTES_IN_KB = 1024L
    private const val BYTES_IN_MB = 1024L * 1024L
    private const val BYTES_IN_GB = 1024L * 1024L * 1024L
    private const val FALLBACK_MIME_TYPE = "application/octet-stream"
}