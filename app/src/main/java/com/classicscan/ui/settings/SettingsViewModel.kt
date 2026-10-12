package com.classicscan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classicscan.data.DocumentRepository
import com.classicscan.data.SettingsRepository
import com.classicscan.data.ThemeMode
import com.classicscan.export.Exporter
import com.classicscan.export.PageSize
import com.classicscan.export.Quality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val repo: DocumentRepository,
    private val exporter: Exporter,
) : ViewModel() {
    val storage = MutableStateFlow<Pair<Long, Long>?>(null)

    init { refreshStorage() }

    private fun refreshStorage() = viewModelScope.launch {
        storage.value = withContext(Dispatchers.IO) { repo.storageBytes() to exporter.cacheBytes() }
    }

    fun setTheme(v: ThemeMode) = viewModelScope.launch { settings.setTheme(v) }
    fun setAccent(v: Int) = viewModelScope.launch { settings.setAccent(v) }
    fun setDynamic(v: Boolean) = viewModelScope.launch { settings.setDynamicColor(v) }
    fun setAmoled(v: Boolean) = viewModelScope.launch { settings.setAmoled(v) }
    fun setAutoCapture(v: Boolean) = viewModelScope.launch { settings.setAutoCapture(v) }
    fun setSearchable(v: Boolean) = viewModelScope.launch { settings.setSearchablePdf(v) }
    fun setAppLock(v: Boolean) = viewModelScope.launch { settings.setAppLock(v) }
    fun setPageSize(v: PageSize) = viewModelScope.launch { settings.setPageSize(v) }
    fun setQuality(v: Quality) = viewModelScope.launch { settings.setQuality(v) }

    fun clearCache() = viewModelScope.launch {
        withContext(Dispatchers.IO) { exporter.clearCache() }
        refreshStorage()
    }
}
