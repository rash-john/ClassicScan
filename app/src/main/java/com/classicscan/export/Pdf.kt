package com.classicscan.export

import java.io.Closeable
import java.io.OutputStream
import java.util.Locale
import kotlin.math.min

enum class PageSize { AUTO, A4, LETTER, ORIGINAL }

enum class Quality(val maxDim: Int, val jpeg: Int, val dpi: Int) {
    SMALL(1400, 60, 120),
    BALANCED(2000, 75, 150),
    HIGH(3000, 90, 200),
}

data class Placement(val pageW: Float, val pageH: Float, val x: Float, val y: Float, val w: Float, val h: Float)

private const val A4_W = 595.28f
private const val A4_H = 841.89f
private const val LETTER_W = 612f
private const val LETTER_H = 792f

fun place(imgW: Int, imgH: Int, size: PageSize, dpi: Int): Placement {
    val iw = imgW * 72f / dpi
    val ih = imgH * 72f / dpi
    val (pw, ph) = when (size) {
        PageSize.ORIGINAL -> return Placement(iw, ih, 0f, 0f, iw, ih)
        PageSize.A4 -> A4_W to A4_H
        PageSize.LETTER -> LETTER_W to LETTER_H
        PageSize.AUTO -> if (imgW > imgH) A4_H to A4_W else A4_W to A4_H
    }
    val s = min(pw / iw, ph / ih)
    val w = iw * s
    val h = ih * s
    return Placement(pw, ph, (pw - w) / 2, (ph - h) / 2, w, h)
}

data class TextBox(val text: String, val left: Float, val top: Float, val right: Float, val bottom: Float)

fun sanitizeFileName(title: String): String =
    title.replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F]"), "_").trim().trim('.').take(80).ifBlank { "Scan" }

class PdfWriter(stream: OutputStream, private val title: String? = null) : Closeable {
    private val out = object : OutputStream() {
        var count = 0L
        override fun write(b: Int) { stream.write(b); count++ }
        override fun write(b: ByteArray, off: Int, len: Int) { stream.write(b, off, len); count += len }
        override fun flush() = stream.flush()
        override fun close() = stream.close()
    }
    private val offsets = mutableMapOf<Int, Long>()
    private val pageIds = mutableListOf<Int>()
    private var nextId = 4
    private var fontId = 0

    init {
        out.write("%PDF-1.4\n%âãÏÓ\n".toByteArray(Charsets.ISO_8859_1))
    }

    private fun f(v: Float) = String.format(Locale.US, "%.2f", v)
    private fun ascii(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))

    private fun obj(id: Int, body: String) {
        offsets[id] = out.count
        ascii("$id 0 obj\n$body\nendobj\n")
    }

    fun addPage(jpeg: ByteArray, imgW: Int, imgH: Int, p: Placement, text: List<TextBox> = emptyList()) {
        val img = nextId++
        offsets[img] = out.count
        ascii("$img 0 obj\n<< /Type /XObject /Subtype /Image /Width $imgW /Height $imgH /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n")
        out.write(jpeg)
        ascii("\nendstream\nendobj\n")

        val content = buildString {
            append("q ${f(p.w)} 0 0 ${f(p.h)} ${f(p.x)} ${f(p.y)} cm /Im0 Do Q")
            if (text.isNotEmpty()) append(textLayer(text, imgW, imgH, p))
        }
        val bytes = content.toByteArray(Charsets.ISO_8859_1)
        val contentId = nextId++
        offsets[contentId] = out.count
        ascii("$contentId 0 obj\n<< /Length ${bytes.size} >>\nstream\n")
        out.write(bytes)
        ascii("\nendstream\nendobj\n")

        if (text.isNotEmpty() && fontId == 0) {
            fontId = nextId++
            obj(fontId, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        }
        val font = if (text.isNotEmpty()) " /Font << /F1 $fontId 0 R >>" else ""
        val pageId = nextId++
        obj(pageId, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${f(p.pageW)} ${f(p.pageH)}] /Resources << /XObject << /Im0 $img 0 R >>$font >> /Contents $contentId 0 R >>")
        pageIds += pageId
    }

    val pageCount get() = pageIds.size

    private fun textLayer(lines: List<TextBox>, imgW: Int, imgH: Int, p: Placement): String = buildString {
        val sx = p.w / imgW
        val sy = p.h / imgH
        append("\nBT 3 Tr")
        for (l in lines) {
            val s = l.text.trim()
            if (s.isEmpty()) continue
            val size = ((l.bottom - l.top) * sy).coerceAtLeast(1f)
            val width = (l.right - l.left) * sx
            val scale = (100f * width / (s.length * size * 0.5f)).coerceIn(10f, 400f)
            val x = p.x + l.left * sx
            val y = p.y + p.h - l.bottom * sy + size * 0.2f
            append("\n/F1 ${f(size)} Tf ${f(scale)} Tz 1 0 0 1 ${f(x)} ${f(y)} Tm (${pdfString(s)}) Tj")
        }
        append("\nET")
    }

    private fun pdfString(s: String) = buildString {
        for (ch in s) {
            when {
                ch == '\\' || ch == '(' || ch == ')' -> append('\\').append(ch)
                ch.code in 32..255 -> append(ch)
                else -> append('?')
            }
        }
    }

    override fun close() {
        obj(2, "<< /Type /Pages /Kids [${pageIds.joinToString(" ") { "$it 0 R" }}] /Count ${pageIds.size} >>")
        obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        obj(3, "<< /Producer (ClassicScan)${title?.let { " /Title <${utf16Hex(it)}>" } ?: ""} >>")
        val xref = out.count
        val size = nextId
        ascii("xref\n0 $size\n0000000000 65535 f \n")
        for (id in 1 until size) ascii(String.format(Locale.US, "%010d 00000 n \n", offsets[id] ?: 0L))
        ascii("trailer\n<< /Size $size /Root 1 0 R /Info 3 0 R >>\nstartxref\n$xref\n%%EOF\n")
        out.flush()
        out.close()
    }

    private fun utf16Hex(s: String) =
        "FEFF" + s.toByteArray(Charsets.UTF_16BE).joinToString("") { String.format(Locale.US, "%02X", it) }
}
