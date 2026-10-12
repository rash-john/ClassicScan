package com.classicscan.ui.camera

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.classicscan.codes.Barcodes
import com.classicscan.data.DocKind
import com.classicscan.data.DocumentRepository
import com.classicscan.data.PageEntity
import com.classicscan.image.Filter
import com.classicscan.image.Quad
import com.classicscan.ui.CameraRoute
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class CameraError { CAPTURE, IMPORT, NO_CODE }

enum class ScanMode { DOCUMENT, BOOK, ID_CARD, WHITEBOARD, QR }

data class ScannedCode(val format: String, val raw: String)

fun idCardGuide(frameAspect: Float): Quad {
    val gh = (0.84f * frameAspect / 1.586f).coerceAtMost(0.95f)
    val top = 0.5f - gh / 2; val bottom = 0.5f + gh / 2
    return floatArrayOf(0.08f, top, 0.92f, top, 0.92f, bottom, 0.08f, bottom)
}

class CameraViewModel(private val repo: DocumentRepository, private val context: Context, handle: SavedStateHandle) : ViewModel() {
    private val route = handle.toRoute<CameraRoute>()
    private val docId = route.docId

    val mode = MutableStateFlow(ScanMode.entries.firstOrNull { it.name == route.mode } ?: ScanMode.DOCUMENT)
    val pages: StateFlow<List<PageEntity>> = repo.observePages(docId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saving = MutableStateFlow(0)
    val error = MutableStateFlow<CameraError?>(null)

    val idSides = MutableStateFlow(0)

    val code = MutableStateFlow<ScannedCode?>(null)
    private var lastCode: String? = null
    private var lastCodeAt = 0L

    fun newFile(): File = repo.newOriginalFile()

    fun onCaptured(file: File, frameAspect: Float, liveQuad: Quad?, onIdDone: () -> Unit) {
        val m = mode.value
        if (m == ScanMode.ID_CARD) idSides.update { it + 1 }
        track(CameraError.CAPTURE) {
            when (m) {
                ScanMode.BOOK -> repo.addPages(docId, file, Filter.AUTO, splitBook = true, fallback = liveQuad)
                ScanMode.WHITEBOARD -> repo.addPages(docId, file, Filter.WHITEBOARD, splitBook = false, fallback = liveQuad)
                ScanMode.ID_CARD -> {
                    repo.setKind(docId, DocKind.ID_CARD)
                    repo.addPages(docId, file, Filter.COLOR, splitBook = false, guide = idCardGuide(frameAspect))
                    if (idSides.value >= 2) onIdDone()
                }
                else -> repo.addPages(docId, file, Filter.AUTO, splitBook = false, fallback = liveQuad)
            }
        }
    }

    fun import(uris: List<Uri>) = track(CameraError.IMPORT) {
        val failed = uris.count { runCatching { repo.importUri(docId, it) }.isFailure }
        if (failed > 0) error.value = CameraError.IMPORT
    }

    fun onCode(format: String, raw: String) {
        if (code.value != null) return
        val now = System.currentTimeMillis()
        if (raw == lastCode && now - lastCodeAt < 3000) return
        lastCode = raw
        code.value = ScannedCode(format, raw)
        viewModelScope.launch { repo.saveCode(format, raw) }
    }

    fun dismissCode() {
        lastCodeAt = System.currentTimeMillis()
        code.value = null
    }

    fun scanImage(uri: Uri?) {
        uri ?: return
        viewModelScope.launch {
            val hit = runCatching { Barcodes.scan(InputImage.fromFilePath(context, uri)) }.getOrNull()?.firstOrNull { it.rawValue != null }
            if (hit == null) error.value = CameraError.NO_CODE
            else {
                lastCode = null
                onCode(Barcodes.formatName(hit.format), hit.rawValue!!)
            }
        }
    }

    private fun track(kind: CameraError, block: suspend () -> Unit) {
        saving.update { it + 1 }
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                error.value = kind
            } finally {
                saving.update { it - 1 }
            }
        }
    }
}
