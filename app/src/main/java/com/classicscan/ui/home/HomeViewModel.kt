package com.classicscan.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classicscan.data.DocumentEntity
import com.classicscan.data.DocumentRepository
import com.classicscan.data.SettingsRepository
import com.classicscan.data.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeMessage { IMPORT_FAILED, IMPORT_PARTIAL, DELETED, DUPLICATED, ERROR }

data class DocItem(val doc: DocumentEntity, val textMatch: Boolean)

data class LibraryStats(val documents: Int, val pages: Int, val bytes: Long)

class HomeViewModel(
    private val repo: DocumentRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    val favoritesOnly = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<HomeMessage?>(null)

    private val textMatches = query.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptySet()) else repo.observeTextMatches(q.trim()).map { it.toSet() }
    }

    val documents: StateFlow<List<DocItem>?> =
        combine(repo.observeDocuments(), settingsRepo.settings, query, favoritesOnly, textMatches) { docs, s, q, fav, matches ->
            val term = q.trim()
            docs.asSequence()
                .filter { !fav || it.isFavorite }
                .mapNotNull { d ->
                    val inTitle = term.isEmpty() || d.title.contains(term, ignoreCase = true)
                    when {
                        inTitle -> DocItem(d, false)
                        d.id in matches -> DocItem(d, true)
                        else -> null
                    }
                }
                .sortedWith(
                    when (s.sort) {
                        SortOrder.MODIFIED -> compareByDescending { it.doc.updatedAt }
                        SortOrder.CREATED -> compareByDescending { it.doc.createdAt }
                        SortOrder.NAME -> compareBy { it.doc.title.lowercase() }
                        SortOrder.SIZE -> compareByDescending { it.doc.sizeBytes }
                    }
                )
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stats: StateFlow<LibraryStats?> = combine(repo.observeDocuments(), repo.observeReadyPageCount()) { docs, pages ->
        LibraryStats(docs.size, pages, docs.sumOf { it.sizeBytes })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val unindexed: StateFlow<Int> = repo.observeUnindexedCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val draft: StateFlow<DocumentEntity?> = repo.observeDraft()
        .map { it?.takeIf { d -> d.pageCount > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun scan(onReady: (String) -> Unit) = guarded { onReady(repo.draftId()) }

    fun import(uris: List<Uri>, onDone: (String) -> Unit) = guarded {
        if (uris.isEmpty()) return@guarded
        val docId = repo.draftId()
        val failed = uris.count { runCatching { repo.importUri(docId, it) }.isFailure }
        when {
            failed == uris.size -> message.value = HomeMessage.IMPORT_FAILED
            else -> {
                if (failed > 0) message.value = HomeMessage.IMPORT_PARTIAL
                onDone(docId)
            }
        }
    }

    fun discardDraft(id: String) = guarded { repo.delete(id) }
    fun rename(id: String, title: String) = guarded { repo.rename(id, title) }
    fun duplicate(id: String, title: String) = guarded { repo.duplicate(id, title); message.value = HomeMessage.DUPLICATED }
    fun delete(id: String) = guarded { repo.delete(id); message.value = HomeMessage.DELETED }
    fun toggleFavorite(doc: DocumentEntity) = viewModelScope.launch { repo.setFavorite(doc.id, !doc.isFavorite) }
    fun setGrid(grid: Boolean) = viewModelScope.launch { settingsRepo.setGrid(grid) }
    fun setSort(sort: SortOrder) = viewModelScope.launch { settingsRepo.setSort(sort) }

    private fun guarded(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                message.value = HomeMessage.ERROR
            } finally {
                busy.value = false
            }
        }
    }
}
