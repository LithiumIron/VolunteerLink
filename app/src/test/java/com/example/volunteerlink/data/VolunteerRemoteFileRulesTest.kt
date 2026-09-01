package com.example.volunteerlink.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolunteerRemoteFileRulesTest {
    @Test fun acceptsExactLimit() {
        VolunteerRemoteFileRules.checkSize(20_000_000)
        VolunteerRemoteFileRules.checkSize(1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAboveLimit() { VolunteerRemoteFileRules.checkSize(20_000_001) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyFile() { VolunteerRemoteFileRules.checkSize(0) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsExecutable() { VolunteerRemoteFileRules.mimeType("slides.pdf.exe") }

    @Test fun acceptsUppercaseExtension() {
        assertEquals("application/pdf", VolunteerRemoteFileRules.mimeType("Project.PDF"))
    }

    @Test fun sanitizesNamesWithoutChangingType() {
        val name = VolunteerRemoteFileRules.safeName("../活动成果 (final).PPTX")
        assertTrue(name.matches(Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,119}$")))
        assertTrue(name.endsWith(".pptx"))
        assertEquals("application/vnd.openxmlformats-officedocument.presentationml.presentation", VolunteerRemoteFileRules.mimeType(name))
    }

    @Test fun supportsEveryPromisedExtension() {
        listOf("pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx", "ppt", "pptx").forEach {
            assertTrue(VolunteerRemoteFileRules.mimeType("work.$it").isNotBlank())
        }
    }
}
