package com.lectoria.data.repository

import android.net.Uri
import com.lectoria.data.model.ClassFolder
import com.lectoria.data.model.LectureFile
import com.lectoria.data.model.SubjectFolder
import com.lectoria.data.storage.FileStorageManager

/**
 * Single entry point the ViewModels use to talk to storage.
 * Keeps the screens free of any file-system knowledge.
 */
class LectoriaRepository(private val storage: FileStorageManager) {

    fun classes(): List<ClassFolder> = storage.listClasses()

    fun subjects(className: String): List<SubjectFolder> = storage.listSubjects(className)

    fun files(className: String, subjectName: String): List<LectureFile> =
        storage.listFiles(className, subjectName)

    fun createClass(className: String): Boolean = storage.createClass(className)

    fun renameClass(oldName: String, newName: String): Boolean =
        storage.renameClass(oldName, newName)

    fun deleteClass(className: String): Boolean = storage.deleteClass(className)

    fun createSubject(className: String, subjectName: String): Boolean =
        storage.createSubject(className, subjectName)

    fun renameSubject(className: String, oldName: String, newName: String): Boolean =
        storage.renameSubject(className, oldName, newName)

    fun deleteSubject(className: String, subjectName: String): Boolean =
        storage.deleteSubject(className, subjectName)

    suspend fun importFile(
        uri: Uri,
        className: String,
        subjectName: String
    ): LectureFile? = storage.importFile(uri, className, subjectName)

    fun deleteFile(className: String, subjectName: String, fileName: String): Boolean =
        storage.deleteFile(className, subjectName, fileName)

    fun subjectCount(className: String): Int = storage.listSubjects(className).size

    fun fileCount(className: String, subjectName: String): Int =
        storage.countFiles(className, subjectName)
}