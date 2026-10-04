package com.lectoria.data.storage

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.lectoria.data.model.LectureFile
import com.lectoria.util.FileUtils
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Exercises the real filesystem behaviour the whole app depends on.
 * Run with: ./gradlew :app:testDebugUnitTest
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileStorageManagerTest {

    private lateinit var context: Context
    private lateinit var storage: FileStorageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = FileStorageManager(context)
    }

    /** Builds files/classes/<parts...> so tests can assert on real folders. */
    private fun onDisk(vararg parts: String): File {
        var dir = File(context.filesDir, "classes")
        parts.forEach { dir = File(dir, it) }
        return dir
    }

    // ---------- Classes ----------

    @Test
    fun `createClass makes a folder and shows up in the list`() {
        assertTrue(storage.createClass("BSIT 3-6"))
        assertTrue(onDisk("BSIT 3-6").isDirectory)
        assertEquals(listOf("BSIT 3-6"), storage.listClasses().map { it.name })
    }

    @Test
    fun `creating the same class twice is refused`() {
        assertTrue(storage.createClass("BSIT 3-6"))
        assertFalse(storage.createClass("BSIT 3-6"))
        assertEquals(1, storage.listClasses().size)
    }

    @Test
    fun `renameClass keeps the subjects inside it`() {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")

        assertTrue(storage.renameClass("BSIT 3-6", "BSIT 3-5"))

        assertFalse(onDisk("BSIT 3-6").exists())
        assertEquals(listOf("BSIT 3-5"), storage.listClasses().map { it.name })
        assertEquals(listOf("Web Systems"), storage.listSubjects("BSIT 3-5").map { it.name })
    }

    @Test
    fun `renameClass refuses to overwrite an existing class`() {
        storage.createClass("BSIT 3-6")
        storage.createClass("BSIT 3-5")

        assertFalse(storage.renameClass("BSIT 3-6", "BSIT 3-5"))

        assertEquals(listOf("BSIT 3-5", "BSIT 3-6"), storage.listClasses().map { it.name })
    }

    @Test
    fun `deleteClass removes the whole tree`() = runBlocking {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")
        importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems")

        assertTrue(storage.deleteClass("BSIT 3-6"))

        assertFalse(onDisk("BSIT 3-6").exists())
        assertTrue(storage.listClasses().isEmpty())
    }

    // ---------- Subjects ----------

    @Test
    fun `createSubject nests under the class and is listed`() {
        storage.createClass("BSIT 3-6")

        assertTrue(storage.createSubject("BSIT 3-6", "Web Systems"))

        assertTrue(onDisk("BSIT 3-6", "Web Systems").isDirectory)
        assertEquals(listOf("Web Systems"), storage.listSubjects("BSIT 3-6").map { it.name })
        assertEquals("BSIT 3-6", storage.listSubjects("BSIT 3-6").first().className)
    }

    @Test
    fun `subjects of different classes stay separate`() {
        storage.createClass("BSIT 3-6")
        storage.createClass("BSIT 3-5")
        storage.createSubject("BSIT 3-6", "Web Systems")
        storage.createSubject("BSIT 3-5", "Database Management")

        assertEquals(listOf("Web Systems"), storage.listSubjects("BSIT 3-6").map { it.name })
        assertEquals(
            listOf("Database Management"),
            storage.listSubjects("BSIT 3-5").map { it.name }
        )
    }

    @Test
    fun `deleteSubject only removes that subject`() {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")
        storage.createSubject("BSIT 3-6", "Network Fundamentals")

        assertTrue(storage.deleteSubject("BSIT 3-6", "Web Systems"))

        assertEquals(
            listOf("Network Fundamentals"),
            storage.listSubjects("BSIT 3-6").map { it.name }
        )
    }
// ---------- Files ----------

    @Test
    fun `importFile copies the file into the subject folder`() = runBlocking {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")
        val content = "fake pdf bytes"

        val stored = importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems", content)

        assertNotNull(stored)
        assertEquals("Lecture 01.pdf", stored!!.name)
        assertEquals(content.length.toLong(), stored.size)
        assertTrue(stored.mimeType.isNotEmpty())
        assertEquals(
            content,
            onDisk("BSIT 3-6", "Web Systems", "Lecture 01.pdf").readText()
        )
        assertEquals(
            listOf("Lecture 01.pdf"),
            storage.listFiles("BSIT 3-6", "Web Systems").map { it.name }
        )
    }

    @Test
    fun `importFile stores the file inside app private storage`() = runBlocking {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")

        val stored = importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems")

        assertTrue(stored!!.path.startsWith(context.filesDir.absolutePath))
    }

    @Test
    fun `importing the same name twice does not overwrite`() = runBlocking {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")

        val first = importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems", "first")
        val second = importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems", "second")

        assertEquals("Lecture 01.pdf", first!!.name)
        assertEquals("Lecture 01 (2).pdf", second!!.name)
        assertEquals(
            "first",
            onDisk("BSIT 3-6", "Web Systems", "Lecture 01.pdf").readText()
        )
        assertEquals(
            "second",
            onDisk("BSIT 3-6", "Web Systems", "Lecture 01 (2).pdf").readText()
        )
    }

    @Test
    fun `fileCount reflects the files in a subject`() = runBlocking {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")
        assertEquals(0, storage.countFiles("BSIT 3-6", "Web Systems"))

        importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems")
        importFile("Lecture 02.pptx", "BSIT 3-6", "Web Systems")

        assertEquals(2, storage.countFiles("BSIT 3-6", "Web Systems"))
    }

    @Test
    fun `deleteFile removes only that file`() = runBlocking {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")
        importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems")
        importFile("Lecture 02.pptx", "BSIT 3-6", "Web Systems")

        assertTrue(storage.deleteFile("BSIT 3-6", "Web Systems", "Lecture 01.pdf"))

        assertEquals(
            listOf("Lecture 02.pptx"),
            storage.listFiles("BSIT 3-6", "Web Systems").map { it.name }
        )
    }

    @Test
    fun `listing files of a subject that does not exist returns nothing`() {
        assertTrue(storage.listFiles("BSIT 3-6", "Web Systems").isEmpty())
        assertEquals(0, storage.countFiles("BSIT 3-6", "Web Systems"))
    }

    // ---------- Batch import (multiple files selected at once) ----------

    @Test
    fun `importing several files keeps all of them in the subject`() = runBlocking {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")

        importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems", "one")
        importFile("Lecture 02.pdf", "BSIT 3-6", "Web Systems", "two")
        importFile("Lecture 03.pptx", "BSIT 3-6", "Web Systems", "three")
        importFile("Activity 01.docx", "BSIT 3-6", "Web Systems", "four")

        assertEquals(
            listOf(
                "Activity 01.docx",
                "Lecture 01.pdf",
                "Lecture 02.pdf",
                "Lecture 03.pptx"
            ),
            storage.listFiles("BSIT 3-6", "Web Systems").map { it.name }
        )
        assertEquals(4, storage.countFiles("BSIT 3-6", "Web Systems"))
    }

    @Test
    fun `a duplicate name inside one batch does not overwrite`() = runBlocking {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")

        importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems", "first")
        importFile("Lecture 02.pdf", "BSIT 3-6", "Web Systems", "second")
        importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems", "third")

        assertEquals(
            listOf("Lecture 01 (2).pdf", "Lecture 01.pdf", "Lecture 02.pdf"),
            storage.listFiles("BSIT 3-6", "Web Systems").map { it.name }
        )
        // The original must still hold its own content.
        assertEquals(
            "first",
            onDisk("BSIT 3-6", "Web Systems", "Lecture 01.pdf").readText()
        )
        assertEquals(
            "third",
            onDisk("BSIT 3-6", "Web Systems", "Lecture 01 (2).pdf").readText()
        )
    }

    @Test
    fun `repeated duplicates keep counting up`() = runBlocking {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")

        repeat(3) { index ->
            importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems", "copy $index")
        }

        assertEquals(
            listOf("Lecture 01 (2).pdf", "Lecture 01 (3).pdf", "Lecture 01.pdf"),
            storage.listFiles("BSIT 3-6", "Web Systems").map { it.name }
        )
    }

    @Test
    fun `files of different types keep their type and size`() = runBlocking {
        storage.createClass("BSIT 3-6")
        storage.createSubject("BSIT 3-6", "Web Systems")

        val pdf = importFile("Lecture 01.pdf", "BSIT 3-6", "Web Systems", "12345")
        val pptx = importFile("Lecture 02.pptx", "BSIT 3-6", "Web Systems", "123")
        val docx = importFile("Activity 01.docx", "BSIT 3-6", "Web Systems", "12")

        assertEquals(5L, pdf!!.size)
        assertEquals(3L, pptx!!.size)
        assertEquals(2L, docx!!.size)
        assertEquals("PPTX", FileUtils.typeLabel(pptx.name))
        assertEquals("DOCX", FileUtils.typeLabel(docx.name))
        assertTrue(pptx.mimeType.isNotEmpty())
    }

    // ---------- Helpers ----------

    /** Stands in for the file the user picks in the system dialog. */
    private suspend fun importFile(
        fileName: String,
        className: String,
        subjectName: String,
        content: String = "content of $fileName"
    ): LectureFile? {
        val uri = Uri.parse("content://lectoria.test/$fileName")
        Shadows.shadowOf(context.contentResolver)
            .registerInputStream(uri, content.byteInputStream())
        return storage.importFile(uri, className, subjectName)
    }
}