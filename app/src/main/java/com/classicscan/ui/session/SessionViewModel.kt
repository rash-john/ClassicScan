package com.classicscan.ui.session

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.classicscan.data.DocStatus
import com.classicscan.data.DocumentEntity
import com.classicscan.data.DocumentRepository
import com.classicscan.data.PageEntity
import com.classicscan.image.suggestTitle
import com.classicscan.ui.SessionRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(private val repo: DocumentRepository, handle: SavedStateHandle) : ViewModel() {
    val docId = handle.toRoute<SessionRoute>().docId

    val document: StateFlow<DocumentEntity?> = repo.observeDocument(docId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gone: StateFlow<Boolean> = repo.observeDocument(docId).map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val pages: StateFlow<List<PageEntity>?> = repo.observePages(docId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val suggestion: StateFlow<String?> = repo.observePages(docId).map { suggestTitle(it.firstOrNull()?.ocrText) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val busy = MutableStateFlow(false)
    val failed = MutableStateFlow(false)

    val isDraft get() = document.value?.status == DocStatus.DRAFT

    fun rotate(page: PageEntity, delta: Int) = run { repo.updatePage(page.copy(rotation = ((page.rotation + delta) % 360 + 360) % 360)) }
    fun delete(page: PageEntity) = run { repo.deletePage(page) }
    fun duplicate(page: PageEntity) = run { repo.duplicatePage(page) }
    fun move(from: Int, to: Int) = run { repo.movePage(docId, from, to) }
    fun import(uris: List<Uri>) = run {
        if (uris.count { runCatching { repo.importUri(docId, it) }.isFailure } > 0) failed.value = true
    }
    fun save(title: String, onSaved: () -> Unit) = run { repo.finalizeDraft(docId, title); onSaved() }
    fun discard(onDone: () -> Unit) = run { repo.delete(docId); onDone() }

    private fun run(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                failed.value = true
            } finally {
                busy.value = false
            }
        }
    }
}
