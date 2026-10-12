package com.classicscan.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.classicscan.data.DocumentRepository
import com.classicscan.data.PageEntity
import com.classicscan.image.Filter
import com.classicscan.image.ImageProcessor
import com.classicscan.image.Quad
import com.classicscan.image.Stroke
import com.classicscan.image.decodeInk
import com.classicscan.image.decodeQuad
import com.classicscan.image.encode
import com.classicscan.image.encodeInk
import com.classicscan.image.filterOf
import com.classicscan.ui.EditorRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Tool { NONE, CROP, FILTER, ADJUST, DRAW }

data class EditState(
    val page: PageEntity? = null,
    val crop: Quad? = null,
    val rotation: Int = 0,
    val filter: Filter = Filter.AUTO,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val strokes: List<Stroke> = emptyList(),
    val penColor: Int = PEN_COLORS[0],
    val penWidth: Float = PEN_WIDTHS[1],
    val tool: Tool = Tool.NONE,
    val missing: Boolean = false,
    val noEdgesFound: Boolean = false,
    val saving: Boolean = false,
    val savedStrokes: List<Stroke> = emptyList(),
) {
    val dirty: Boolean
        get() = page != null && (crop?.encode() != page.crop || rotation != page.rotation || filter.name != page.filter ||
            brightness != page.brightness || contrast != page.contrast || strokes != savedStrokes)

    fun applyTo(p: PageEntity) = p.copy(
        crop = crop?.encode(), rotation = rotation, filter = filter.name,
        brightness = brightness, contrast = contrast, ink = encodeInk(strokes),
    )
}

val PEN_COLORS = listOf(0xFF1C1B22.toInt(), 0xFF1E4FD8.toInt(), 0xFFD62D3A.toInt(), 0xFF138A4A.toInt())
val PEN_WIDTHS = listOf(0.003f, 0.006f, 0.012f)

class EditorViewModel(
    private val repo: DocumentRepository,
    private val processor: ImageProcessor,
    handle: SavedStateHandle,
) : ViewModel() {
    private val pageId = handle.toRoute<EditorRoute>().pageId
    val state = MutableStateFlow(EditState())

    init {
        viewModelScope.launch {
            val page = repo.page(pageId)
            state.value = if (page == null) EditState(missing = true)
            else decodeInk(page.ink).let { ink ->
                EditState(page, decodeQuad(page.crop), page.rotation, filterOf(page.filter), page.brightness, page.contrast, ink, savedStrokes = ink)
            }
        }
    }

    fun tool(t: Tool) = state.update { it.copy(tool = if (it.tool == t) Tool.NONE else t, noEdgesFound = false) }
    fun setCrop(q: Quad) = state.update { it.copy(crop = q) }
    fun fullPage() = state.update { it.copy(crop = null, noEdgesFound = false) }

    fun rotate(delta: Int) = state.update { it.copy(rotation = ((it.rotation + delta) % 360 + 360) % 360, strokes = emptyList()) }
    fun setFilter(f: Filter) = state.update { it.copy(filter = f) }
    fun setBrightness(v: Float) = state.update { it.copy(brightness = v) }
    fun setContrast(v: Float) = state.update { it.copy(contrast = v) }
    fun resetAdjust() = state.update { it.copy(brightness = 0f, contrast = 0f) }
    fun setPen(color: Int? = null, width: Float? = null) = state.update { it.copy(penColor = color ?: it.penColor, penWidth = width ?: it.penWidth) }
    fun addStroke(points: List<Float>) = state.update { s ->
        if (points.size < 4) s else s.copy(strokes = s.strokes + Stroke(s.penColor, s.penWidth, points))
    }
    fun undoStroke() = state.update { it.copy(strokes = it.strokes.dropLast(1)) }
    fun clearInk() = state.update { it.copy(strokes = emptyList()) }
    fun reset() = state.update {
        it.copy(crop = null, rotation = 0, filter = Filter.AUTO, brightness = 0f, contrast = 0f, strokes = emptyList(), noEdgesFound = false)
    }

    fun autoDetect() {
        val page = state.value.page ?: return
        viewModelScope.launch {
            val q = runCatching { processor.detect(processor.fileOf(page)) }.getOrNull()
            state.update { if (q != null) it.copy(crop = q, noEdgesFound = false) else it.copy(noEdgesFound = true) }
        }
    }

    fun applyFilterToAll(onDone: () -> Unit) = saveThen(onDone) { page, s ->
        repo.applyFilterToAll(page.documentId, s.filter)
    }

    fun save(onDone: () -> Unit) = saveThen(onDone) { _, _ -> }

    private fun saveThen(onDone: () -> Unit, after: suspend (PageEntity, EditState) -> Unit) {
        val s = state.value
        val page = s.page ?: return
        if (s.saving) return
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                if (s.dirty) repo.updatePage(s.applyTo(page))
                after(page, s)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                state.update { it.copy(saving = false) }
            }
        }
    }
}
