package com.classicscan.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import com.classicscan.data.PageEntity
import com.classicscan.image.ImageProcessor
import com.classicscan.image.TextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

enum class ExportFormat(val mime: String, val ext: String) {
    PDF("application/pdf", "pdf"),
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
}

class Exporter(
    private val context: Context,
    private val processor: ImageProcessor,
    private val recognizer: TextRecognizer,
) {
    private val dir get() = File(context.cacheDir, "exports").apply { mkdirs() }

    suspend fun export(
        pages: List<PageEntity>,
        title: String,
        format: ExportFormat,
        size: PageSize,
        quality: Quality,
        searchable: Boolean,
        idCard: Boolean = false,
        onProgress: (done: Int, total: Int) -> Unit,
    ): List<File> = withContext(Dispatchers.IO) {
        val name = sanitizeFileName(title)
        dir.listFiles()?.forEach { it.deleteRecursively() }
        val dir = File(dir, System.currentTimeMillis().toString()).apply { mkdirs() }
        val temps = mutableListOf<File>()
        try {
            val finals = if (format == ExportFormat.PDF) {
                val tmp = File(dir, "$name.pdf.tmp").also { temps += it }
                PdfWriter(tmp.outputStream().buffered(), title).use { pdf ->

                    val sheets: List<List<PageEntity>> = if (idCard) pages.chunked(2) else pages.map { listOf(it) }
                    sheets.forEachIndexed { i, sheet ->
                        ensureActive()
                        val bmp = if (idCard) idSheet(sheet.map { render(it, quality) }, quality.dpi) else render(sheet[0], quality)
                        val jpeg = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, quality.jpeg, it) }.toByteArray()

                        val text = if (searchable) runCatching { recognizer.lines(bmp) }.getOrDefault(emptyList()) else emptyList()
                        pdf.addPage(jpeg, bmp.width, bmp.height, place(bmp.width, bmp.height, if (idCard) PageSize.A4 else size, quality.dpi), text)
                        bmp.recycle()
                        onProgress(i + 1, sheets.size)
                    }
                }
                listOf(tmp to File(dir, "$name.pdf"))
            } else {
                pages.mapIndexed { i, page ->
                    ensureActive()
                    val suffix = if (pages.size > 1) "-${i + 1}" else ""
                    val tmp = File(dir, "$name$suffix.${format.ext}.tmp").also { temps += it }
                    val bmp = render(page, quality)
                    tmp.outputStream().buffered().use {
                        val cf = if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                        if (!bmp.compress(cf, quality.jpeg, it)) throw IOException("encode")
                    }
                    bmp.recycle()
                    onProgress(i + 1, pages.size)
                    tmp to File(dir, "$name$suffix.${format.ext}")
                }
            }
            finals.map { (tmp, final) ->
                final.delete()
                if (!tmp.renameTo(final)) throw IOException("finalize")
                final
            }
        } finally {
            temps.forEach { it.delete() }
        }
    }

    private fun idSheet(cards: List<Bitmap>, dpi: Int): Bitmap {
        val pageW = (8.27f * dpi).toInt(); val pageH = (11.69f * dpi).toInt()
        val cardW = 3.370f * dpi; val cardH = 2.125f * dpi
        val out = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
        cards.forEachIndexed { i, src ->

            val card = if (src.height > src.width) Bitmap.createBitmap(src, 0, 0, src.width, src.height, android.graphics.Matrix().apply { postRotate(90f) }, true) else src
            val s = minOf(cardW / card.width, cardH / card.height)
            val w = card.width * s; val h = card.height * s
            val top = 1.2f * dpi + i * (cardH + 0.6f * dpi) + (cardH - h) / 2
            val left = (pageW - w) / 2
            canvas.drawBitmap(card, null, android.graphics.RectF(left, top, left + w, top + h), paint)
            if (card !== src) card.recycle()
            src.recycle()
        }
        return out
    }

    private suspend fun render(page: PageEntity, quality: Quality): Bitmap =
        processor.render(page, quality.maxDim, useCache = false) ?: throw IOException("decode")

    fun uri(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun shareIntent(files: List<File>, format: ExportFormat): Intent {
        val uris = files.map(::uri)
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        send.type = format.mime
        send.clipData = ClipData.newRawUri(null, uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, null)
    }

    suspend fun saveTo(file: File, dest: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(dest, "wt")?.use { out -> file.inputStream().use { it.copyTo(out) } }
            ?: throw IOException("destination")
    }

    suspend fun saveToFolder(files: List<File>, tree: Uri, format: ExportFormat) = withContext(Dispatchers.IO) {
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        files.forEach { f ->
            val doc = DocumentsContract.createDocument(context.contentResolver, parent, format.mime, f.name)
                ?: throw IOException("destination")
            saveTo(f, doc)
        }
    }

    fun cacheBytes(): Long = context.cacheDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    fun clearCache() {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
