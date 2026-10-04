package com.lectoria.data.model

/**
 * A lecture file stored inside a subject folder.
 * Example: "Lecture 01.pdf"
 *
 * The file is a copy that lives in app-private storage, so [path] is enough to
 * reopen it later without needing the original SAF uri.
 */
data class LectureFile(
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String
)