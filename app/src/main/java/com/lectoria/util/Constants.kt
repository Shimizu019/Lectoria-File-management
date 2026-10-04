package com.lectoria.util

/**
 * App-wide constants.
 */
object Constants {

    /**
     * File types offered by the system picker when importing a lecture file.
     * PDFs, Word, PowerPoint, Excel and plain text cover what teachers send.
     */
    val IMPORTABLE_MIME_TYPES = arrayOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain"
    )
}