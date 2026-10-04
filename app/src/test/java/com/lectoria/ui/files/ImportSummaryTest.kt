package com.lectoria.ui.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The message shown after importing one or many files at once.
 */
class ImportSummaryTest {

    @Test
    fun `one saved file reads naturally`() {
        assertEquals("1 file saved", importSummaryMessage(saved = 1, failed = 0))
    }

    @Test
    fun `several saved files are counted`() {
        assertEquals("4 files saved", importSummaryMessage(saved = 4, failed = 0))
    }

    @Test
    fun `partial success reports both numbers`() {
        assertEquals(
            "2 of 3 files saved",
            importSummaryMessage(saved = 2, failed = 1)
        )
    }

    @Test
    fun `a complete failure says so`() {
        assertEquals("No files were saved", importSummaryMessage(saved = 0, failed = 2))
    }

    @Test
    fun `nothing at all reports nothing`() {
        assertNull(importSummaryMessage(saved = 0, failed = 0))
    }
}