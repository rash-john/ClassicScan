package com.classicscan.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.classicscan.codes.CodesScreen
import com.classicscan.ui.camera.ScanMode
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import com.classicscan.ACTION_IMPORT
import com.classicscan.ACTION_SCAN
import com.classicscan.data.DocumentRepository
import org.koin.compose.koinInject
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.classicscan.data.Settings
import com.classicscan.ui.camera.CameraScreen
import com.classicscan.ui.editor.EditorScreen
import com.classicscan.ui.home.HomeScreen
import com.classicscan.ui.session.SessionScreen
import com.classicscan.ui.settings.SettingsScreen
import com.classicscan.ui.licenses.LicensesScreen
import com.classicscan.ui.viewer.ViewerScreen
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object SettingsRoute
@Serializable object LicensesRoute
@Serializable data class CameraRoute(val docId: String, val mode: String = "DOCUMENT")
@Serializable object CodesRoute
@Serializable data class SessionRoute(val docId: String)
@Serializable data class EditorRoute(val pageId: String)
@Serializable data class ViewerRoute(val docId: String)

@Serializable object OnboardingRoute

@Composable
fun AppNav(settings: Settings, pendingAction: String?, onActionHandled: () -> Unit) {
    val nav = rememberNavController()
    val repo = koinInject<DocumentRepository>()
    var importRequested by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pendingAction, settings.onboarded) {
        if (!settings.onboarded) return@LaunchedEffect
        when (pendingAction) {
            ACTION_SCAN -> nav.navigate(CameraRoute(repo.draftId())) { popUpTo<HomeRoute>() }
            ACTION_IMPORT -> { nav.popBackStack<HomeRoute>(inclusive = false); importRequested = true }
            else -> return@LaunchedEffect
        }
        onActionHandled()
    }
    NavHost(
        nav, startDestination = if (settings.onboarded) HomeRoute else OnboardingRoute,

        enterTransition = { slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320)) },
        exitTransition = { slideOutHorizontally(tween(320)) { -it / 6 } + fadeOut(tween(200)) },
        popEnterTransition = { slideInHorizontally(tween(320)) { -it / 6 } + fadeIn(tween(320)) },
        popExitTransition = { slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(200)) },
    ) {
        composable<OnboardingRoute> { SideSafe {
            OnboardingScreen(onDone = { nav.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } } })
        } }
        composable<HomeRoute> { SideSafe {
            HomeScreen(
                settings = settings,
                importRequested = importRequested,
                onImportHandled = { importRequested = false },
                onScan = { id, mode -> nav.navigate(CameraRoute(id, mode.name)) },
                onCodes = { nav.navigate(CodesRoute) },
                onReview = { nav.navigate(SessionRoute(it)) },
                onOpen = { nav.navigate(ViewerRoute(it)) },
                onSettings = { nav.navigate(SettingsRoute) },
            )
        } }
        composable<CameraRoute> {
            val docId = it.toRoute<CameraRoute>().docId
            CameraScreen(
                autoCapture = settings.autoCapture,
                showGrid = settings.cameraGrid,
                onCodes = { nav.navigate(CodesRoute) },
                onBack = { nav.popBackStack() },
                onReview = {
                    nav.navigate(SessionRoute(docId)) { popUpTo<CameraRoute> { inclusive = true }; launchSingleTop = true }
                },
            )
        }
        composable<SessionRoute> { SideSafe {
            val docId = it.toRoute<SessionRoute>().docId
            SessionScreen(
                onBack = { nav.popBackStack() },
                onAddPages = { nav.navigate(CameraRoute(docId)) },
                onEdit = { pageId -> nav.navigate(EditorRoute(pageId)) },
                onSaved = { nav.navigate(ViewerRoute(docId)) { popUpTo<HomeRoute>() } },
            )
        } }
        composable<EditorRoute> { SideSafe { EditorScreen(onBack = { nav.popBackStack() }) } }
        composable<ViewerRoute> { SideSafe {
            val docId = it.toRoute<ViewerRoute>().docId
            ViewerScreen(
                settings = settings,
                onBack = { nav.popBackStack() },
                onOrganize = { nav.navigate(SessionRoute(docId)) },
                onAddPage = { nav.navigate(CameraRoute(docId)) },
                onEditPage = { pageId -> nav.navigate(EditorRoute(pageId)) },
                onOpenCopy = { id -> nav.navigate(ViewerRoute(id)) { popUpTo<HomeRoute>() } },
            )
        } }
        composable<CodesRoute> { SideSafe {
            CodesScreen(onBack = { nav.popBackStack() }, onScanCode = {
                scope.launch { nav.navigate(CameraRoute(repo.draftId(), ScanMode.QR.name)) { popUpTo<HomeRoute>() } }
            })
        } }
        composable<SettingsRoute> { SideSafe { SettingsScreen(settings = settings, onBack = { nav.popBackStack() }, onLicenses = { nav.navigate(LicensesRoute) }) } }
        composable<LicensesRoute> { SideSafe { LicensesScreen(onBack = { nav.popBackStack() }) } }
    }
}

@Composable
private fun SideSafe(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) { content() }
}
