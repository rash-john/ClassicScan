package com.classicscan.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.map

@Immutable
data class WindowLayout(
    val widthDp: Int,
    val heightDp: Int,
    val tabletop: Boolean = false,
    val book: Boolean = false,
    val hingeTopPx: Int = 0,
    val hingeBottomPx: Int = 0,
) {
    val medium get() = widthDp >= 600
    val expanded get() = widthDp >= 840

    val shortHeight get() = heightDp < 480
    val landscape get() = widthDp > heightDp

    val sideBySide get() = book || expanded || (landscape && shortHeight) || (landscape && medium)
}

val LocalWindowLayout = staticCompositionLocalOf { WindowLayout(400, 800) }

@Composable
fun ProvideWindowLayout(activity: Activity, content: @Composable () -> Unit) {
    val config = LocalConfiguration.current
    val fold by remember(activity) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity)
            .map { info -> info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull() }
    }.collectAsStateWithLifecycle(initialValue = null)
    val half = fold?.state == FoldingFeature.State.HALF_OPENED
    val layout = WindowLayout(
        config.screenWidthDp, config.screenHeightDp,
        tabletop = half && fold?.orientation == FoldingFeature.Orientation.HORIZONTAL,
        book = half && fold?.orientation == FoldingFeature.Orientation.VERTICAL,
        hingeTopPx = fold?.bounds?.top ?: 0,
        hingeBottomPx = fold?.bounds?.bottom ?: 0,
    )
    CompositionLocalProvider(LocalWindowLayout provides layout, content = content)
}

@Composable
fun Centered(maxWidth: Dp = 760.dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxWidth(), content = content)
    }
}
