package com.classicscan.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.classicscan.image.Filter
import com.classicscan.image.ImageProcessor
import com.classicscan.image.FULL_QUAD
import com.classicscan.image.Quad
import com.classicscan.image.polygonArea
import com.classicscan.image.encode
import com.classicscan.image.splitQuad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

class ImportException(message: String) : IOException(message)

class DocumentRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val processor: ImageProcessor,
    val originalsDir: File,
) {
    private val dao = db.dao()

    fun observeDocuments(): Flow<List<DocumentEntity>> = dao.observeReady()
    fun observeDraft(): Flow<DocumentEntity?> = dao.observeDraft()
    fun observeDocument(id: String): Flow<DocumentEntity?> = dao.observeDocument(id)
    fun observePages(docId: String): Flow<List<PageEntity>> = dao.observePages(docId)
    suspend fun pages(docId: String) = dao.pages(docId)
    suspend fun page(id: String) = dao.page(id)
    suspend fun document(id: String) = dao.document(id)

    suspend fun draftId(): String = db.withTransaction {
        dao.draft()?.id ?: UUID.randomUUID().toString().also { id ->
            val now = System.currentTimeMillis()
            dao.insert(DocumentEntity(id, "", now, now, 0, null, DocStatus.DRAFT, 0))
        }
    }

    fun newOriginalFile(): File = File(originalsDir.apply { mkdirs() }, "${UUID.randomUUID()}.jpg")

    suspend fun addPage(docId: String, file: File, defaultFilter: Filter = Filter.AUTO): PageEntity =
        addPages(docId, file, defaultFilter, splitBook = false).first()

    suspend fun addPages(docId: String, file: File, defaultFilter: Filter, splitBook: Boolean, guide: Quad? = null, fallback: Quad? = null): List<PageEntity> = withContext(Dispatchers.IO) {
        try {
            val (w, h) = processor.orientedSize(file) ?: throw ImportException("undecodable")
            val detected = processor.detect(file) ?: fallback

            val crop = if (guide == null) detected else detected?.takeIf { polygonArea(it) / polygonArea(guide) in 0.5f..1.6f } ?: guide
            val crops = if (splitBook) splitQuad(crop ?: FULL_QUAD).toList() else listOf(crop)
            val now = System.currentTimeMillis()
            db.withTransaction {
                val start = dao.pages(docId).size
                crops.mapIndexed { i, q ->
                    PageEntity(
                        UUID.randomUUID().toString(), docId, start + i, file.name, w, h, 0,
                        q?.encode(), defaultFilter.name, 0, now, now,
                    ).also { dao.insert(it) }
                }.also { touch(docId) }
            }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    suspend fun setKind(docId: String, kind: String) = dao.setKind(docId, kind)

    fun observeCodes() = dao.observeCodes()
    suspend fun saveCode(format: String, raw: String) = dao.insert(CodeEntity(format = format, raw = raw, createdAt = System.currentTimeMillis()))
    suspend fun deleteCode(id: Long) = dao.deleteCode(id)
    suspend fun clearCodes() = dao.clearCodes()

    suspend fun importUri(docId: String, uri: Uri): PageEntity = withContext(Dispatchers.IO) {
        val file = newOriginalFile()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            } ?: throw ImportException("unreadable")
        } catch (e: Exception) {
            file.delete()
            throw if (e is ImportException) e else ImportException("unreadable")
        }
        addPage(docId, file)
    }

    suspend fun updatePage(page: PageEntity) {
        processor.evict(page.id)
        db.withTransaction {
            dao.update(page.copy(editVersion = page.editVersion + 1, updatedAt = System.currentTimeMillis(), ocrText = null))
            touch(page.documentId)
        }
    }

    suspend fun applyFilterToAll(docId: String, filter: Filter) = db.withTransaction {
        dao.pages(docId).filter { it.filter != filter.name }.forEach {
            processor.evict(it.id)
            dao.update(it.copy(filter = filter.name, editVersion = it.editVersion + 1, updatedAt = System.currentTimeMillis()))
        }
        touch(docId)
    }

    suspend fun setFavorite(docId: String, favorite: Boolean) = dao.setFavorite(docId, favorite)

    fun observeTextMatches(q: String) = dao.observeTextMatches(q)
    fun observeReadyPageCount() = dao.observeReadyPageCount()
    fun observeUnindexedCount() = dao.observeUnindexedCount()
    suspend fun unindexedPages() = dao.unindexedPages()
    suspend fun setOcrText(page: PageEntity, text: String) = dao.setOcrText(page.id, page.editVersion, text)

    suspend fun deletePage(page: PageEntity) {
        db.withTransaction {
            dao.deletePage(page.id)
            dao.updatePages(dao.pages(page.documentId).mapIndexed { i, p -> p.copy(position = i) })
            touch(page.documentId)
        }
        processor.evict(page.id)

        if (dao.fileRefs(page.originalFile) == 0) File(originalsDir, page.originalFile).delete()
    }

    suspend fun movePage(docId: String, from: Int, to: Int) = db.withTransaction {
        val list = dao.pages(docId).toMutableList()
        if (from !in list.indices || to !in list.indices) return@withTransaction
        list.add(to, list.removeAt(from))
        dao.updatePages(list.mapIndexed { i, p -> p.copy(position = i) })
        touch(docId)
    }

    suspend fun duplicatePage(page: PageEntity) = withContext(Dispatchers.IO) {
        val copy = newOriginalFile()
        File(originalsDir, page.originalFile).copyTo(copy)
        db.withTransaction {
            val list = dao.pages(page.documentId)
            dao.updatePages(list.filter { it.position > page.position }.map { it.copy(position = it.position + 1) })
            dao.insert(page.copy(id = UUID.randomUUID().toString(), position = page.position + 1, originalFile = copy.name))
            touch(page.documentId)
        }
    }

    suspend fun finalizeDraft(docId: String, title: String) = db.withTransaction {
        val doc = dao.document(docId) ?: return@withTransaction
        dao.update(doc.copy(title = title.trim(), status = DocStatus.READY, updatedAt = System.currentTimeMillis()))
        touch(docId)
    }

    suspend fun rename(docId: String, title: String) = db.withTransaction {
        val doc = dao.document(docId) ?: return@withTransaction
        dao.update(doc.copy(title = title.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun duplicate(docId: String, title: String): String = withContext(Dispatchers.IO) {
        val doc = dao.document(docId) ?: throw IOException("missing")
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val copied = mutableMapOf<String, File>()
        val copies = dao.pages(docId).map { p ->
            val f = copied.getOrPut(p.originalFile) { newOriginalFile().also { File(originalsDir, p.originalFile).copyTo(it) } }
            p.copy(id = UUID.randomUUID().toString(), documentId = newId, originalFile = f.name, createdAt = now, updatedAt = now)
        }
        try {
            db.withTransaction {
                dao.insert(doc.copy(id = newId, title = title, createdAt = now, updatedAt = now, coverPageId = null, isFavorite = false))
                dao.insertPages(copies)
                touch(newId)
            }
        } catch (e: Exception) {
            copied.values.forEach { it.delete() }
            throw e
        }
        newId
    }

    suspend fun delete(docId: String) = withContext(Dispatchers.IO) {
        val pages = dao.pages(docId)
        dao.deleteDocument(docId)
        pages.forEach { processor.evict(it.id); File(originalsDir, it.originalFile).delete() }
    }

    private suspend fun touch(docId: String) {
        val doc = dao.document(docId) ?: return
        val pages = dao.pages(docId)
        dao.update(
            doc.copy(
                pageCount = pages.size,
                coverPageId = pages.firstOrNull()?.id,
                sizeBytes = pages.sumOf { File(originalsDir, it.originalFile).length() },
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun cleanOrphans() = withContext(Dispatchers.IO) {
        val referenced = dao.allOriginalFiles().toSet()
        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000
        originalsDir.listFiles()?.forEach {
            if (it.name !in referenced && it.lastModified() < cutoff) it.delete()
        }
    }

    fun storageBytes(): Long = originalsDir.listFiles()?.sumOf { it.length() } ?: 0
}
