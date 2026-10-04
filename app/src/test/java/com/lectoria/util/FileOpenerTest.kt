package com.lectoria.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.lectoria.R
import com.lectoria.data.model.LectureFile
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * Checks that tapping a file builds a safe, shareable intent, and that a
 * device with no suitable app is handled without crashing.
 *
 * The intent logic is tested with a stand-in uri because Robolectric cannot load
 * the FileProvider's path configuration from the manifest; the real provider is
 * exercised when the app runs on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileOpenerTest {

    private lateinit var context: Context

    /** Stands in for the FileProvider uri so intent logic can be tested. */
    private val fakeShareUri: (Context, LectureFile) -> Uri = { _, file ->
        Uri.parse("content://${context.packageName}.fileprovider/classes/${file.name}")
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    /** Creates files/classes/<Class>/<Subject>/<name> and describes it. */
    private fun storedFile(
        fileName: String = "Lecture 01.pdf",
        content: String = "content"
    ): LectureFile {
        val dir = File(context.filesDir, "classes/BSIT 3-6/Web Systems")
        dir.mkdirs()
        val file = File(dir, fileName)
        file.writeText(content)
        return LectureFile(
            name = fileName,
            path = file.absolutePath,
            size = file.length(),
            mimeType = FileUtils.mimeTypeOf(fileName)
        )
    }

    @Test
    fun `view intent asks to view the file`() {
        val intent = FileOpener.buildViewIntent(context, storedFile(), fakeShareUri)

        assertEquals(Intent.ACTION_VIEW, intent.action)
    }

    @Test
    fun `view intent uses a content uri and never a file uri`() {
        val intent = FileOpener.buildViewIntent(context, storedFile(), fakeShareUri)

        assertEquals("content", intent.data?.scheme)
        assertTrue(intent.data?.toString()?.startsWith("file:") != true)
    }

    @Test
    fun `view uri points at the Lectoria file provider`() {
        val intent = FileOpener.buildViewIntent(context, storedFile(), fakeShareUri)

        assertEquals(context.packageName + ".fileprovider", intent.data?.authority)
    }

    @Test
    fun `view intent carries the mime type of the tapped file`() {
        listOf("Lecture 01.pdf", "Lecture 02.pptx", "Activity 01.docx").forEach { name ->
            val file = storedFile(fileName = name)
            val intent = FileOpener.buildViewIntent(context, file, fakeShareUri)

            assertEquals(
                "Wrong mime type passed for $name",
                file.mimeType,
                intent.type?.substringBefore(";")
            )
        }
    }

    @Test
    fun `view intent grants read access to the receiving app`() {
        val intent = FileOpener.buildViewIntent(context, storedFile(), fakeShareUri)

        assertTrue(
            "Intent must grant read permission",
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        )
    }

    @Test
    fun `view uri identifies only the tapped file`() {
        val intent = FileOpener.buildViewIntent(context, storedFile(), fakeShareUri)

        assertTrue(
            "Uri should name the selected file, was ${intent.data}",
            intent.data.toString().endsWith("Lecture 01.pdf")
        )
    }

    @Test
    fun `opening a file with no compatible app reports it instead of crashing`() {
        // Robolectric registers no app that can view a PDF.
        val result = FileOpener.open(context, storedFile(), fakeShareUri)

        assertEquals(FileOpenResult.NoCompatibleApp, result)
        assertEquals(
            "No compatible app found to open this file.",
            result.errorMessage
        )
    }

    @Test
    fun `opening a deleted file reports it instead of crashing`() {
        val missing = LectureFile(
            name = "Gone.pdf",
            path = File(context.filesDir, "classes/BSIT 3-6/Web Systems/Gone.pdf").absolutePath,
            size = 0L,
            mimeType = "application/pdf"
        )

        val result = FileOpener.open(context, missing, fakeShareUri)

        assertEquals(FileOpenResult.FileMissing, result)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `a uri failure is reported as a missing file instead of crashing`() {
        val boom: (Context, LectureFile) -> Uri = { _, _ ->
            throw IllegalArgumentException("outside shared folder")
        }

        val result = FileOpener.open(context, storedFile(), boom)

        assertEquals(FileOpenResult.FileMissing, result)
    }

    @Test
    fun `an opened file needs no message`() {
        assertNull(FileOpenResult.Opened.errorMessage)
    }

    @Test
    fun `shared folder covers only the classes tree`() {
        val parser = context.resources.getXml(R.xml.file_paths)
        var tagName = ""
        var pathAttribute = ""
        var found = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT && !found) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "files-path") {
                tagName = parser.name
                pathAttribute = parser.getAttributeValue(null, "path") ?: ""
                found = true
            }
            parser.next()
        }

        assertEquals("files-path", tagName)
        assertEquals("classes/", pathAttribute)
    }
}