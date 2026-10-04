package com.lectoria.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * These helpers run on the JVM, so they can be checked without a device:
 *   ./gradlew :app:testDebugUnitTest
 */
class FileUtilsTest {

    @Test
    fun `formats bytes below one kilobyte`() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("512 B", FileUtils.formatFileSize(512))
        assertEquals("1023 B", FileUtils.formatFileSize(1023))
    }

    @Test
    fun `formats kilobytes`() {
        assertEquals("1.0 KB", FileUtils.formatFileSize(1024))
        assertEquals("1.5 KB", FileUtils.formatFileSize(1536))
    }

    @Test
    fun `formats megabytes and gigabytes`() {
        assertEquals("1.0 MB", FileUtils.formatFileSize(1_048_576))
        assertEquals("2.5 MB", FileUtils.formatFileSize(2_621_440))
        assertEquals("1.0 GB", FileUtils.formatFileSize(1_073_741_824))
    }

    @Test
    fun `shows the extension as the type label`() {
        assertEquals("PDF", FileUtils.typeLabel("Lecture 01.pdf"))
        assertEquals("PPTX", FileUtils.typeLabel("Lecture 02.pptx"))
        assertEquals("DOCX", FileUtils.typeLabel("Activity 01.docx"))
    }

    @Test
    fun `falls back to FILE when there is no extension`() {
        assertEquals("FILE", FileUtils.typeLabel("untitled"))
    }

    @Test
    fun `uses only the last extension when there are several`() {
        assertEquals("GZ", FileUtils.typeLabel("handout.tar.gz"))
    }

    @Test
    fun `sanitizeName keeps ordinary academic names intact`() {
        assertEquals("BSIT 3-6", FileUtils.sanitizeName("BSIT 3-6"))
        assertEquals("Web Systems", FileUtils.sanitizeName("  Web Systems  "))
        assertEquals("Database Management", FileUtils.sanitizeName("Database Management"))
    }

    @Test
    fun `sanitizeName removes characters that would break folders or routes`() {
        assertEquals("BSIT-3-6", FileUtils.sanitizeName("BSIT/3-6"))
        assertEquals("50- off", FileUtils.sanitizeName("50% off"))
        assertEquals("a-b", FileUtils.sanitizeName("a\\b"))
    }

    @Test
    fun `sanitizeName returns empty for blank input so creation is refused`() {
        assertEquals("", FileUtils.sanitizeName("   "))
    }

    @Test
    fun `sanitizeName replaces each unsafe character rather than deleting it`() {
        assertEquals("---", FileUtils.sanitizeName("///"))
    }
}