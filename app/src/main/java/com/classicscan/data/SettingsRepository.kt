package com.classicscan.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.classicscan.export.PageSize
import com.classicscan.export.Quality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class SortOrder { MODIFIED, CREATED, NAME, SIZE }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val grid: Boolean = false,
    val sort: SortOrder = SortOrder.MODIFIED,
    val pageSize: PageSize = PageSize.AUTO,
    val quality: Quality = Quality.BALANCED,
    val onboarded: Boolean = false,
    val dynamicColor: Boolean = false,
    val amoled: Boolean = false,
    val accent: Int = 0,
    val autoCapture: Boolean = false,
    val appLock: Boolean = false,
    val searchablePdf: Boolean = true,
    val cameraGrid: Boolean = true,
)

private val Context.dataStore by preferencesDataStore("settings")

private inline fun <reified T : Enum<T>> Preferences.enum(key: Preferences.Key<String>, default: T): T =
    this[key]?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default

class SettingsRepository(private val context: Context) {
    private val theme = stringPreferencesKey("theme")
    private val grid = booleanPreferencesKey("grid")
    private val sort = stringPreferencesKey("sort")
    private val pageSize = stringPreferencesKey("pageSize")
    private val quality = stringPreferencesKey("quality")
    private val onboarded = booleanPreferencesKey("introSeen")
    private val dynamicColor = booleanPreferencesKey("dynamicColor")
    private val amoled = booleanPreferencesKey("amoled")
    private val accent = intPreferencesKey("accent")
    private val autoCapture = booleanPreferencesKey("autoCapture")
    private val appLock = booleanPreferencesKey("appLock")
    private val searchablePdf = booleanPreferencesKey("searchablePdf")
    private val cameraGrid = booleanPreferencesKey("cameraGrid")

    val settings: Flow<Settings> = context.dataStore.data.map {
        Settings(
            theme = it.enum(theme, ThemeMode.SYSTEM),
            grid = it[grid] ?: false,
            sort = it.enum(sort, SortOrder.MODIFIED),
            pageSize = it.enum(pageSize, PageSize.AUTO),
            quality = it.enum(quality, Quality.BALANCED),
            onboarded = it[onboarded] ?: false,
            dynamicColor = it[dynamicColor] ?: false,
            amoled = it[amoled] ?: false,
            accent = it[accent] ?: 0,
            autoCapture = it[autoCapture] ?: false,
            appLock = it[appLock] ?: false,
            searchablePdf = it[searchablePdf] ?: true,
            cameraGrid = it[cameraGrid] ?: true,
        )
    }

    suspend fun setTheme(v: ThemeMode) = context.dataStore.edit { it[theme] = v.name }
    suspend fun setGrid(v: Boolean) = context.dataStore.edit { it[grid] = v }
    suspend fun setSort(v: SortOrder) = context.dataStore.edit { it[sort] = v.name }
    suspend fun setPageSize(v: PageSize) = context.dataStore.edit { it[pageSize] = v.name }
    suspend fun setQuality(v: Quality) = context.dataStore.edit { it[quality] = v.name }
    suspend fun setOnboarded() = context.dataStore.edit { it[onboarded] = true }
    suspend fun setDynamicColor(v: Boolean) = context.dataStore.edit { it[dynamicColor] = v }
    suspend fun setAmoled(v: Boolean) = context.dataStore.edit { it[amoled] = v }
    suspend fun setAccent(v: Int) = context.dataStore.edit { it[accent] = v }
    suspend fun setAutoCapture(v: Boolean) = context.dataStore.edit { it[autoCapture] = v }
    suspend fun setAppLock(v: Boolean) = context.dataStore.edit { it[appLock] = v }
    suspend fun setSearchablePdf(v: Boolean) = context.dataStore.edit { it[searchablePdf] = v }
    suspend fun setCameraGrid(v: Boolean) = context.dataStore.edit { it[cameraGrid] = v }
}
