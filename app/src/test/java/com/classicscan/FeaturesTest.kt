package com.classicscan

import com.classicscan.codes.CodeContent
import com.classicscan.codes.parseCode
import com.classicscan.image.FULL_QUAD
import com.classicscan.image.Stroke
import com.classicscan.image.decodeInk
import com.classicscan.image.encodeInk
import com.classicscan.image.quadPoint
import com.classicscan.image.splitQuad
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturesTest {
    @Test fun wifiCodeWithEscapes() {
        val c = parseCode("WIFI:T:WPA;S:Cafe\\;Guest;P:p\\:ss\\\\word;H:false;;", "QR_CODE")
        assertEquals(CodeContent.Wifi("Cafe;Guest", "p:ss\\word", "WPA", false), c)
    }

    @Test fun urlsPhonesAndMail() {
        assertEquals(CodeContent.Url("https://example.com/a?b=1"), parseCode("https://example.com/a?b=1", "QR_CODE"))
        assertEquals(CodeContent.Url("https://www.example.com"), parseCode("www.example.com", "QR_CODE"))
        assertEquals(CodeContent.Phone("+8801700000000"), parseCode("tel:+8801700000000", "QR_CODE"))
        assertEquals(CodeContent.Email("a@b.com", "Hi there", null), parseCode("mailto:a@b.com?subject=Hi%20there", "QR_CODE"))
        assertEquals(CodeContent.Sms("12345", "hello"), parseCode("SMSTO:12345:hello", "QR_CODE"))
        assertEquals(CodeContent.Geo(23.81, 90.41), parseCode("geo:23.81,90.41", "QR_CODE"))
    }

    @Test fun contactsFromVCardAndMeCard() {
        val v = parseCode("BEGIN:VCARD\nVERSION:3.0\nFN:Rahim Uddin\nORG:Acme\nTEL;TYPE=CELL:+880171\nEMAIL:r@acme.com\nEND:VCARD", "QR_CODE") as CodeContent.Contact
        assertEquals("Rahim Uddin", v.name); assertEquals(listOf("+880171"), v.phones); assertEquals("Acme", v.org)
        val m = parseCode("MECARD:N:Uddin,Rahim;TEL:+880171;EMAIL:r@acme.com;;", "QR_CODE") as CodeContent.Contact
        assertEquals("Rahim Uddin", m.name); assertEquals(listOf("r@acme.com"), m.emails)
    }

    @Test fun retailBarcodesAreProducts() {
        assertEquals(CodeContent.Product("4006381333931"), parseCode("4006381333931", "EAN_13"))
        assertEquals(CodeContent.Text("4006381333931"), parseCode("4006381333931", "QR_CODE"))
    }

    @Test fun bookSpreadSplitsDownTheMiddle() {
        val (l, r) = splitQuad(FULL_QUAD)
        assertArrayEquals(floatArrayOf(0f, 0f, 0.5f, 0f, 0.5f, 1f, 0f, 1f), l, 1e-6f)
        assertArrayEquals(floatArrayOf(0.5f, 0f, 1f, 0f, 1f, 1f, 0.5f, 1f), r, 1e-6f)
    }

    @Test fun gridPointsFollowTheQuad() {
        val q = floatArrayOf(0.1f, 0.1f, 0.9f, 0.2f, 0.8f, 0.9f, 0.2f, 0.8f)
        assertEquals(0.1f to 0.1f, quadPoint(q, 0f, 0f))
        assertEquals(0.8f, quadPoint(q, 1f, 1f).first, 1e-6f)
        val mid = quadPoint(q, 0.5f, 0.5f)
        assertEquals(0.5f, mid.first, 1e-6f); assertEquals(0.5f, mid.second, 1e-6f)
    }

    @Test fun inkRoundTrips() {
        val strokes = listOf(Stroke(0xFF1E4FD8.toInt(), 0.006f, listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.45f)))
        val back = decodeInk(encodeInk(strokes))
        assertEquals(1, back.size)
        assertEquals(strokes[0].color, back[0].color)
        assertEquals(0.45f, back[0].points.last(), 1e-4f)
        assertNull(encodeInk(listOf(Stroke(0, 0.01f, listOf(0.5f, 0.5f)))))
        assertTrue(decodeInk("garbage|1;x;0,0").isEmpty())
    }

    @Test fun idGuideHasCardProportions() {
        val aspect = 3f / 4f
        val g = com.classicscan.ui.camera.idCardGuide(aspect)
        val pxW = (g[2] - g[0]) * aspect
        val pxH = g[5] - g[1]
        assertEquals(1.586f, pxW / pxH, 1e-3f)
    }

    @Test fun rotatedQuadIsRelabelledUpright() {
        val q = floatArrayOf(0.1f, 0.2f, 0.6f, 0.1f, 0.7f, 0.9f, 0.2f, 0.8f)
        val turned = com.classicscan.image.orderQuad(com.classicscan.image.FULL_QUAD.let { q }.let { r -> FloatArray(8) { r[(it + 2) % 8] } })
        assertArrayEquals(q, turned, 1e-6f)
    }

    @Test fun layoutBreakpoints() {
        val phone = com.classicscan.ui.WindowLayout(411, 891)
        val phoneLandscape = com.classicscan.ui.WindowLayout(891, 411)
        val tabletPortrait = com.classicscan.ui.WindowLayout(800, 1280)
        val tabletLandscape = com.classicscan.ui.WindowLayout(1280, 800)
        val foldBook = com.classicscan.ui.WindowLayout(673, 841, book = true)
        assertTrue(!phone.sideBySide && !phone.medium)
        assertTrue(phoneLandscape.sideBySide && phoneLandscape.shortHeight)
        assertTrue(tabletPortrait.medium && !tabletPortrait.sideBySide)
        assertTrue(tabletLandscape.expanded && tabletLandscape.sideBySide)
        assertTrue(foldBook.sideBySide)
    }

    @Test fun licenseReflowJoinsLinesKeepsParagraphs() {
        val text = "Copyright 2026 ClassicScan\n\nLicensed under the Apache License;\nyou may not use this file.\n\n   http://www.apache.org/licenses/LICENSE-2.0\n"
        assertEquals(
            "Copyright 2026 ClassicScan\n\nLicensed under the Apache License; you may not use this file.\n\nhttp://www.apache.org/licenses/LICENSE-2.0",
            com.classicscan.ui.licenses.reflow(text),
        )
    }
}
