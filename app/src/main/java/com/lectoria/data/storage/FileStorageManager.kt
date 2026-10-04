package com.lectoria.data.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.lectoria.data.model.ClassFolder
import com.lectoria.data.model.LectureFile
import com.lectoria.data.model.SubjectFolder
import com.lectoria.util.FileUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns every read and write against app-private storage.
 *
 * Folder layout on disk (the source of truth for the whole app):
 *   files/classes/BSIT 3-6/Web Systems/Lecture 01.pdf
 *
 * Because files are copied inside the app sandbox, Lectoria never needs broad
 * storage permissions and the Storage Access Framework is only used to import.
 */
class FileStorageManager(context: Context) {

    private val appContext = context.applicationContext

    /** Root that holds one folder per class. */
    private val classesRoot: File
        get() = File(appContext.filesDir, CLASSES_DIR)

    fun classDir(className: String): File = File(classesRoot, className)

    fun subjectDir(className: String, subjectName: String): File =
        File(classDir(className), subjectName)

    // ---------------- Classes ----------------

    fun createClass(className: String): Boolean {
        val dir = classDir(className)
        return !dir.exists() && dir.mkdirs()
    }

    fun renameClass(oldName: String, newName: String): Boolean {
        if (oldName == newName) return false
        val oldDir = classDir(oldName)
        val newDir = classDir(newName)
        return oldDir.isDirectory && !newDir.exists() && oldDir.renameTo(newDir)
    }

    fun deleteClass(className: String): Boolean = classDir(className).deleteRecursively()

    fun listClasses(): List<ClassFolder> =
        classesRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.map { ClassFolder(it.name) }
            ?.sortedBy { it.name.lowercase() }
            .orEmpty()

    // ---------------- Subjects ----------------

    fun createSubject(className: String, subjectName: String): Boolean {
        val dir = subjectDir(className, subjectName)
        return !dir.exists() && dir.mkdirs()
    }

    fun renameSubject(className: String, oldName: String, newName: String): Boolean {
        if (oldName == newName) return false
        val oldDir = subjectDir(className, oldName)
        val newDir = subjectDir(className, newName)
        return oldDir.isDirectory && !newDir.exists() && oldDir.renameTo(newDir)
    }

    fun deleteSubject(className: String, subjectName: String): Boolean =
        subjectDir(className, subjectName).deleteRecursively()

    fun listSubjects(className: String): List<SubjectFolder> =
        classDir(className).listFiles()
            ?.filter { it.isDirectory }
            ?.map { SubjectFolder(it.name, className) }
            ?.sortedBy { it.name.lowercase() }
            .orEmpty()

    // ---------------- Files ----------------

    /**
     * Copies a file chosen through the Storage Access Framework into a subject folder.
     * Returns the stored file, or null when the copy failed.
     */
    suspend fun importFile(
        uri: Uri,
        className: String,
        subjectName: String
    ): LectureFile? = withContext(Dispatchers.IO) {
        val targetDir = subjectDir(className, subjectName)
        if (!targetDir.isDirectory && !targetDir.mkdirs()) return@withContext null

        val requestedName = displayNameOf(uri) ?: DEFAULT_FILE_NAME
        val destination = uniqueFileIn(targetDir, requestedName)

        val copied = runCatching {
            appContext.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Could not open the selected file" }
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        }.isSuccess

        if (!copied) {
            destination.delete()
            null
        } else {
            toLectureFile(destination)
        }
    }

    fun deleteFile(className: String, subjectName: String, fileName: String): Boolean =
        File(subjectDir(className, subjectName), fileName).delete()

    fun listFiles(className: String, subjectName: String): List<LectureFile> =
        subjectDir(className, subjectName).listFiles()
            ?.filter { it.isFile }
            ?.map { toLectureFile(it) }
            ?.sortedBy { it.name.lowercase() }
            .orEmpty()

    fun countFiles(className: String, subjectName: String): Int =
        subjectDir(className, subjectName).listFiles()?.count { it.isFile } ?: 0

    // ---------------- Helpers ----------------

    private fun toLectureFile(file: File) = LectureFile(
        name = file.name,
        path = file.absolutePath,
        size = file.length(),
        mimeType = FileUtils.mimeTypeOf(file.name)
    )

    /** Keeps names unique: "Lecture 01.pdf" -> "Lecture 01 (2).pdf". */
    private fun uniqueFileIn(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate

        val baseName = fileName.substringBeforeLast('.', fileName)
        val extension = fileName.substringAfterLast('.', "")
        var counter = 2
        while (candidate.exists()) {
            val name = if (extension.isEmpty()) {
                "$baseName ($counter)"
            } else {
                "$baseName ($counter).$extension"
            }
            candidate = File(dir, name)
            counter++
        }
        return candidate
    }

    /** Reads the display name the file provider reports, e.g. "Lecture 01.pdf". */
    private fun displayNameOf(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return uri.lastPathSegment
    }

    private companion object {
        const val CLASSES_DIR = "classes"
        const val DEFAULT_FILE_NAME = "untitled"
    }
}