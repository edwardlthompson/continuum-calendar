package org.fossify.calendar.continuum

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FaqLocaleCopyTest {

    @Test
    fun localeFaq2OmitsDavx5() {
        val res = File("src/main/res")
        assertTrue("res dir missing at ${res.absolutePath}", res.isDirectory)
        var scanned = 0
        res.listFiles()?.filter { it.isDirectory && it.name.startsWith("values-") }?.forEach { dir ->
            val strings = File(dir, "strings.xml")
            if (!strings.isFile) return@forEach
            val text = strings.readText()
            val start = text.indexOf("<string name=\"faq_2_text\">")
            if (start < 0) return@forEach
            val end = text.indexOf("</string>", start)
            assertTrue(end > start)
            val body = text.substring(start, end)
            scanned += 1
            assertFalse("${dir.name} faq_2_text still mentions DAVx5", body.contains("DAVx5", ignoreCase = true))
        }
        assertTrue("no locale faq_2_text scanned", scanned > 0)
    }
}
