package com.classicscan

import android.app.Application
import androidx.room.Room
import com.classicscan.data.AppDatabase
import com.classicscan.data.DocumentRepository
import com.classicscan.data.MIGRATION_1_2
import com.classicscan.data.MIGRATION_2_3
import com.classicscan.data.SettingsRepository
import com.classicscan.export.Exporter
import com.classicscan.image.ImageProcessor
import com.classicscan.image.TextIndexer
import com.classicscan.image.TextRecognizer
import com.classicscan.ui.camera.CameraViewModel
import com.classicscan.ui.editor.EditorViewModel
import com.classicscan.ui.home.HomeViewModel
import com.classicscan.ui.session.SessionViewModel
import com.classicscan.ui.settings.SettingsViewModel
import com.classicscan.ui.viewer.ViewerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.io.File

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val originals = File(filesDir, "originals")
        val appModule = module {
            single { Room.databaseBuilder(get(), AppDatabase::class.java, "classicscan.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build() }
            single { ImageProcessor(originals) }
            single { DocumentRepository(get(), get(), get(), originals) }
            single { SettingsRepository(get()) }
            single { TextRecognizer() }
            single { TextIndexer(get(), get(), get()) }
            single { Exporter(get(), get(), get()) }
            viewModelOf(::HomeViewModel)
            viewModelOf(::CameraViewModel)
            viewModelOf(::SessionViewModel)
            viewModelOf(::EditorViewModel)
            viewModelOf(::ViewerViewModel)
            viewModelOf(::SettingsViewModel)
        }
        val koin = startKoin {
            androidContext(this@App)
            modules(appModule)
        }.koin
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch { koin.get<DocumentRepository>().cleanOrphans() }
        koin.get<TextIndexer>().start(scope)
    }
}
