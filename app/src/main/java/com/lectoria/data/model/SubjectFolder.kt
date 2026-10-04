package com.lectoria.data.model

/**
 * A subject folder inside a class.
 * Example: "Web Systems" inside "BSIT 3-6"
 */
data class SubjectFolder(
    val name: String,
    val className: String
)