package com.classicscan

import com.classicscan.export.PageSize
import com.classicscan.export.PdfWriter
import com.classicscan.export.Placement
import com.classicscan.export.TextBox
import com.classicscan.export.place
import com.classicscan.export.sanitizeFileName
import com.classicscan.image.FULL_QUAD
import com.classicscan.image.decodeQuad
import com.classicscan.image.detectQuad
import com.classicscan.image.encode
import com.classicscan.image.maxCornerDelta
import com.classicscan.image.suggestTitle
import com.classicscan.image.rotateNorm
import com.classicscan.image.rotated
import com.classicscan.image.warpedSize
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class CoreLogicTest {
    @Test fun rotationRoundTrips() {
        val q = floatArrayOf(0.1f, 0.2f, 0.9f, 0.1f, 0.8f, 0.9f, 0.2f, 0.8f)
        for (deg in listOf(0, 90, 180, 270)) {
            assertArrayEquals(q, q.rotated(deg).rotated(360 - deg), 1e-6f)
        }
        assertEquals(1f to 0f, rotateNorm(0f, 0f, 90))
    }

    @Test fun quadEncoding() {
        assertArrayEquals(FULL_QUAD, decodeQuad(FULL_QUAD.encode()), 0f)
        assertNull(decodeQuad("1,2,3"))
        assertNull(decodeQuad(null))
    }

    @Test fun warpedSizeOfFullFrameIsImageSize() {
        assertEquals(400 to 300, warpedSize(FULL_QUAD, 400, 300))
    }

    @Test fun detectsBrightPageOnDarkBackground() {
        val w = 160; val h = 120
        val lum = IntArray(w * h) { p -> val x = p % w; val y = p / w; if (x in 40..119 && y in 20..99) 230 else 40 }
        val q = detectQuad(lum, w, h)
        assertNotNull(q)
        assertEquals(40.5f / w, q!![0], 0.01f)
        assertEquals(99.5f / h, q[5], 0.01f)
    }

    @Test fun detectsTiltedTextPageAndLowContrastPage() {
        val w = 160; val h = 120
        val c = kotlin.math.cos(0.5); val sn = kotlin.math.sin(0.5)
        fun inPage(x: Int, y: Int): Boolean {
            val dx = x - 80.0; val dy = y - 60.0
            return kotlin.math.abs(dx * c + dy * sn) < 38 && kotlin.math.abs(-dx * sn + dy * c) < 30
        }
        val tilted = IntArray(w * h) { p -> val x = p % w; val y = p / w; if (inPage(x, y)) (if (y % 6 < 2 && x % 5 < 3) 30 else 235) else 50 }
        val q = detectQuad(tilted, w, h)
        assertNotNull(q)
        val expected = listOf(-38.0 to -30.0, 38.0 to -30.0, 38.0 to 30.0, -38.0 to 30.0).map { (u, v) ->
            ((80 + u * c - v * sn) / w).toFloat() to ((60 + u * sn + v * c) / h).toFloat()
        }
        for ((ex, ey) in expected) assertTrue((0 until 4).any { kotlin.math.hypot(q!![it * 2] - ex, q[it * 2 + 1] - ey) < 0.03f })

        val soft = IntArray(w * h) { p -> val x = p % w; val y = p / w; if (x in 30..129 && y in 15..104) 215 else 190 + (x * 7 + y * 13) % 9 }
        val s = detectQuad(soft, w, h)
        assertNotNull(s)
        assertEquals(30.5f / w, s!![0], 0.02f)
    }

    @Test fun alignedQuadKeepsCornerIdentity() {
        val q = floatArrayOf(0.1f, 0.2f, 0.6f, 0.1f, 0.7f, 0.9f, 0.2f, 0.8f)
        val rolled = FloatArray(8) { q[(it + 4) % 8] }
        assertArrayEquals(q, com.classicscan.image.alignQuad(rolled, q), 0f)
    }

    @Test fun noDetectionOnBlankOrNoise() {
        assertNull(detectQuad(IntArray(160 * 120) { 128 }, 160, 120))
        val rnd = java.util.Random(1)
        assertNull(detectQuad(IntArray(160 * 120) { rnd.nextInt(256) }, 160, 120))
    }

    @Test fun placementKeepsAspectAndFitsPage() {
        val p = place(1000, 2000, PageSize.A4, 150)
        assertEquals(0.5f, p.w / p.h, 1e-3f)
        assertTrue(p.w <= p.pageW + 0.01f && p.h <= p.pageH + 0.01f)
        assertEquals(p.pageW, p.x * 2 + p.w, 0.01f)
        val land = place(2000, 1000, PageSize.AUTO, 150)
        assertTrue(land.pageW > land.pageH)
        assertEquals(Placement(480f, 240f, 0f, 0f, 480f, 240f), place(2000, 1000, PageSize.ORIGINAL, 300))
    }

    @Test fun fileNamesAreSanitized() {
        assertEquals("a_b_c", sanitizeFileName("a/b:c"))
        assertEquals("Scan", sanitizeFileName("  ..  "))
        assertEquals("রসিদ অক্টোবর", sanitizeFileName("রসিদ অক্টোবর"))
    }

    @Test fun pdfHasValidXref() {
        val out = ByteArrayOutputStream()
        PdfWriter(out, "টেস্ট").use { pdf ->
            repeat(2) { pdf.addPage(byteArrayOf(1, 2, 3), 10, 20, place(10, 20, PageSize.A4, 150)) }
        }
        val bytes = out.toByteArray()
        val text = String(bytes, Charsets.ISO_8859_1)
        assertTrue(text.startsWith("%PDF-1.4"))
        assertTrue(text.contains("/Count 2"))
        val startxref = text.substringAfter("startxref\n").substringBefore("\n").toInt()
        assertTrue(text.substring(startxref).startsWith("xref"))

        val entries = text.substring(startxref).lines().drop(3).takeWhile { it.endsWith(" n ") || it.matches(Regex("\\d{10} 00000 n")) }
        entries.forEachIndexed { i, e ->
            val off = e.take(10).toInt()
            assertTrue("obj ${i + 1}", text.substring(off).startsWith("${i + 1} 0 obj"))
        }
        assertEquals(9, entries.size)
    }

    @Test fun searchablePdfEmbedsInvisibleText() {
        val out = ByteArrayOutputStream()
        PdfWriter(out).use { pdf ->
            pdf.addPage(byteArrayOf(1), 1000, 1400, place(1000, 1400, PageSize.A4, 150), listOf(TextBox("Invoice (No. 42)", 100f, 100f, 600f, 140f)))
        }
        val text = String(out.toByteArray(), Charsets.ISO_8859_1)
        assertTrue(text.contains("3 Tr"))
        assertTrue(text.contains("(Invoice \\(No. 42\\)) Tj"))
        assertTrue(text.contains("/BaseFont /Helvetica"))

        val m = Regex("<< /Length (\\d+) >>\nstream\n").findAll(text).last()
        val len = m.groupValues[1].toInt()
        val body = text.substring(m.range.last + 1)
        assertTrue(body.substring(len).startsWith("\nendstream"))
    }

    @Test fun titleSuggestionSkipsNoise() {
        assertEquals("ACME Corp Invoice", suggestTitle("--\n12\n  ACME Corp Invoice  \nTotal 30"))
        assertNull(suggestTitle("1234\n--"))
        assertNull(suggestTitle(null))
    }

    @Test fun stabilityDeltaIsLargestCornerMove() {
        val b = FULL_QUAD.copyOf().also { it[4] = 0.9f }
        assertEquals(0.1f, maxCornerDelta(FULL_QUAD, b), 1e-6f)
        assertEquals(0f, maxCornerDelta(FULL_QUAD, FULL_QUAD), 0f)
    }
}
