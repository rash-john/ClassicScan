package com.classicscan.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

object DocStatus {
    const val DRAFT = "DRAFT"
    const val READY = "READY"
}

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pageCount: Int,
    val coverPageId: String?,
    val status: String,
    val sizeBytes: Long,
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,

    @ColumnInfo(defaultValue = "DOCUMENT") val kind: String = DocKind.DOCUMENT,
)

object DocKind {
    const val DOCUMENT = "DOCUMENT"
    const val ID_CARD = "ID_CARD"
}

@Entity(
    tableName = "pages",
    foreignKeys = [ForeignKey(DocumentEntity::class, ["id"], ["documentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("documentId")],
)
data class PageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val position: Int,
    val originalFile: String,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val crop: String?,
    val filter: String,
    val editVersion: Int,
    val createdAt: Long,
    val updatedAt: Long,

    val ocrText: String? = null,

    @ColumnInfo(defaultValue = "0") val brightness: Float = 0f,
    @ColumnInfo(defaultValue = "0") val contrast: Float = 0f,

    val ink: String? = null,
)

@Entity(tableName = "codes")
data class CodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val format: String,
    val raw: String,
    val createdAt: Long,
)

@Dao
interface ScanDao {
    @Query("SELECT * FROM documents WHERE status = 'READY'")
    fun observeReady(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE status = 'DRAFT' ORDER BY updatedAt DESC LIMIT 1")
    fun observeDraft(): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE status = 'DRAFT' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun draft(): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeDocument(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun document(id: String): DocumentEntity?

    @Query("SELECT * FROM pages WHERE documentId = :docId ORDER BY position")
    fun observePages(docId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE documentId = :docId ORDER BY position")
    suspend fun pages(docId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun page(id: String): PageEntity?

    @Query("SELECT DISTINCT p.documentId FROM pages p JOIN documents d ON d.id = p.documentId WHERE d.status = 'READY' AND p.ocrText LIKE '%' || :q || '%'")
    fun observeTextMatches(q: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM pages WHERE ocrText IS NULL")
    fun observeUnindexedCount(): Flow<Int>

    @Query("SELECT * FROM pages WHERE ocrText IS NULL ORDER BY createdAt")
    suspend fun unindexedPages(): List<PageEntity>

    @Query("UPDATE pages SET ocrText = :text WHERE id = :id AND editVersion = :editVersion")
    suspend fun setOcrText(id: String, editVersion: Int, text: String)

    @Query("UPDATE documents SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("SELECT COALESCE(SUM(pageCount), 0) FROM documents WHERE status = 'READY'")
    fun observeReadyPageCount(): Flow<Int>

    @Query("SELECT originalFile FROM pages")
    suspend fun allOriginalFiles(): List<String>

    @Query("SELECT COUNT(*) FROM pages WHERE originalFile = :file")
    suspend fun fileRefs(file: String): Int

    @Query("UPDATE documents SET kind = :kind WHERE id = :id")
    suspend fun setKind(id: String, kind: String)

    @Query("SELECT * FROM codes ORDER BY createdAt DESC")
    fun observeCodes(): Flow<List<CodeEntity>>

    @Insert suspend fun insert(code: CodeEntity): Long

    @Query("DELETE FROM codes WHERE id = :id")
    suspend fun deleteCode(id: Long)

    @Query("DELETE FROM codes")
    suspend fun clearCodes()

    @Insert suspend fun insert(doc: DocumentEntity)
    @Insert suspend fun insert(page: PageEntity)
    @Insert suspend fun insertPages(pages: List<PageEntity>)
    @Update suspend fun update(doc: DocumentEntity)
    @Update suspend fun update(page: PageEntity)
    @Update suspend fun updatePages(pages: List<PageEntity>)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deletePage(id: String)
}

@Database(entities = [DocumentEntity::class, PageEntity::class, CodeEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): ScanDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE documents ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE pages ADD COLUMN ocrText TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pages ADD COLUMN brightness REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE pages ADD COLUMN contrast REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE pages ADD COLUMN ink TEXT")
        db.execSQL("ALTER TABLE documents ADD COLUMN kind TEXT NOT NULL DEFAULT 'DOCUMENT'")
        db.execSQL("CREATE TABLE IF NOT EXISTS codes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, format TEXT NOT NULL, raw TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}
