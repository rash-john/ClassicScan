package com.classicscan.ui.viewer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.classicscan.data.DocKind
import com.classicscan.data.DocumentEntity
import com.classicscan.data.DocumentRepository
import com.classicscan.data.PageEntity
import com.classicscan.export.ExportFormat
import com.classicscan.export.Exporter
import com.classicscan.export.PageSize
import com.classicscan.export.Quality
import com.classicscan.ui.ViewerRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface ExportState {
    data object Idle : ExportState
    data class Running(val done: Int, val total: Int) : ExportState
    data class Done(val files: List<File>, val format: ExportFormat, val shareNow: Boolean) : ExportState
    data object Saved : ExportState
    data object Failed : ExportState
}

class ViewerViewModel(
    private val repo: DocumentRepository,
    val exporter: Exporter,
    handle: SavedStateHandle,
) : ViewModel() {
    val docId = handle.toRoute<ViewerRoute>().docId
    val document: StateFlow<DocumentEntity?> = repo.observeDocument(docId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gone: StateFlow<Boolean> = repo.observeDocument(docId).map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val pages: StateFlow<List<PageEntity>?> = repo.observePages(docId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val export = MutableStateFlow<ExportState>(ExportState.Idle)
    private var job: Job? = null

    fun startExport(format: ExportFormat, size: PageSize, quality: Quality, searchable: Boolean, onlyPage: Int?, shareNow: Boolean = false) {
        if (export.value is ExportState.Running) return
        val all = pages.value.orEmpty()
        val selected = if (onlyPage != null) listOfNotNull(all.getOrNull(onlyPage)) else all
        if (selected.isEmpty()) return
        val title = document.value?.title.orEmpty()
        export.value = ExportState.Running(0, selected.size)
        job = viewModelScope.launch {
            export.value = try {
                val files = exporter.export(selected, title, format, size, quality, searchable, idCard = document.value?.kind == DocKind.ID_CARD && format == ExportFormat.PDF) { d, t -> export.value = ExportState.Running(d, t) }
                ExportState.Done(files, format, shareNow)
            } catch (e: CancellationException) {
                ExportState.Idle
            } catch (e: Exception) {
                ExportState.Failed
            }
        }
    }

    fun cancelExport() { job?.cancel(); export.value = ExportState.Idle }
    fun dismissExport() { if (export.value !is ExportState.Running) export.value = ExportState.Idle }

    fun saveTo(uri: Uri?, folder: Boolean) {
        val done = export.value as? ExportState.Done ?: return
        if (uri == null) return
        viewModelScope.launch {
            export.value = try {
                if (folder) exporter.saveToFolder(done.files, uri, done.format) else exporter.saveTo(done.files.first(), uri)
                ExportState.Saved
            } catch (e: Exception) {
                ExportState.Failed
            }
        }
    }

    fun toggleFavorite() = viewModelScope.launch { document.value?.let { repo.setFavorite(it.id, !it.isFavorite) } }

    fun rename(title: String) = viewModelScope.launch { repo.rename(docId, title) }
    fun duplicate(title: String, onDone: (String) -> Unit) = viewModelScope.launch { runCatching { repo.duplicate(docId, title) }.onSuccess(onDone) }
    fun delete(onDone: () -> Unit) = viewModelScope.launch { repo.delete(docId); onDone() }
}
