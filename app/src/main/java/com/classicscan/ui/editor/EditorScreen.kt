package com.classicscan.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import com.classicscan.ui.LocalWindowLayout
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.classicscan.image.adjustMatrix
import com.classicscan.ui.ZoomState
import com.classicscan.ui.zoomable
import com.classicscan.ui.zoomed
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classicscan.R
import com.classicscan.data.PageEntity
import com.classicscan.image.FULL_QUAD
import com.classicscan.image.Filter
import com.classicscan.image.ImageProcessor
import com.classicscan.image.Quad
import com.classicscan.image.clampQuad
import com.classicscan.image.rotated
import com.classicscan.ui.ConfirmDialog
import com.classicscan.ui.components.AppButton
import com.classicscan.ui.components.AppChip
import com.classicscan.ui.components.CircleButton
import com.classicscan.ui.components.bounceClick
import com.classicscan.ui.theme.LocalScanColors
import com.classicscan.ui.theme.ThumbShape
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

fun filterLabel(f: Filter) = when (f) {
    Filter.ORIGINAL -> R.string.filter_original
    Filter.AUTO -> R.string.filter_auto
    Filter.GRAYSCALE -> R.string.filter_grayscale
    Filter.BW -> R.string.filter_bw
    Filter.COLOR -> R.string.filter_color
    Filter.WHITEBOARD -> R.string.filter_whiteboard
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(onBack: () -> Unit, vm: EditorViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    val leave = { if (s.dirty) confirmLeave = true else onBack() }
    BackHandler { leave() }

    val side = LocalWindowLayout.current.sideBySide
    val contextPanel: @Composable () -> Unit = {
                    s.page?.let { page ->
                        AnimatedContent(s.tool, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "panel") { tool ->
                            when (tool) {
                                Tool.CROP -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                                    AppChip(false, vm::autoDetect) { Icon(Icons.Outlined.AutoFixHigh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.auto_detect)) }
                                    AppChip(s.crop == null, vm::fullPage) { Icon(Icons.Outlined.CropFree, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.full_page)) }
                                }
                                Tool.FILTER -> Column {
                                    FilterRow(page, s, vm::setFilter)
                                    TextButton(onClick = { vm.applyFilterToAll(onBack) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                        Icon(Icons.Outlined.DoneAll, null); Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.apply_to_all_pages, stringResource(filterLabel(s.filter))))
                                    }
                                }
                                Tool.ADJUST -> AdjustPanel(s, vm)
                                Tool.DRAW -> DrawPanel(s, vm)
                                Tool.NONE -> Spacer(Modifier.height(0.dp))
                            }
                        }
                    }
    }
    val tools: @Composable () -> Unit = {
                        ToolButton(Icons.Outlined.Crop, R.string.crop, s.tool == Tool.CROP) { vm.tool(Tool.CROP) }
                        ToolButton(Icons.Outlined.RotateLeft, R.string.rotate_left, false) { vm.rotate(-90) }
                        ToolButton(Icons.Outlined.RotateRight, R.string.rotate_right, false) { vm.rotate(90) }
                        ToolButton(Icons.Outlined.DocumentScanner, R.string.filter, s.tool == Tool.FILTER) { vm.tool(Tool.FILTER) }
                        ToolButton(Icons.Outlined.Tune, R.string.adjust, s.tool == Tool.ADJUST) { vm.tool(Tool.ADJUST) }
                        ToolButton(Icons.Outlined.Draw, R.string.sign_draw, s.tool == Tool.DRAW) { vm.tool(Tool.DRAW) }
                        ToolButton(Icons.Outlined.RestartAlt, R.string.reset, false) { vm.reset() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { CircleButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), leave, Modifier.padding(start = 8.dp)) },
                title = { Text(stringResource(R.string.edit_page)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    AppButton(stringResource(R.string.done), { vm.save(onBack) }, Modifier.padding(end = 12.dp), enabled = s.page != null && !s.saving)
                },
            )
        },
        bottomBar = {
            if (!side) Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                Column(Modifier.navigationBarsPadding().padding(top = 8.dp)) {
                    contextPanel()
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) { tools() }
                }
            }
        },
    ) { padding ->
      Row(Modifier.padding(padding).fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight().padding(16.dp), contentAlignment = Alignment.Center) {
            val page = s.page
            when {
                s.missing -> Text(stringResource(R.string.page_missing), textAlign = TextAlign.Center)
                page == null -> CircularProgressIndicator()
                s.tool == Tool.CROP -> CropView(page, s.crop ?: FULL_QUAD, s.rotation, vm::setCrop)
                s.tool == Tool.DRAW -> DrawView(page, s, vm::addStroke)
                else -> Preview(page, s)
            }
            if (s.noEdgesFound) {
                Text(
                    stringResource(R.string.edges_not_found_editor),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.TopCenter).clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 14.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        if (side) Surface(
            Modifier.width(360.dp).fillMaxHeight().navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) { tools() }
                Spacer(Modifier.height(8.dp))
                contextPanel()
            }
        }
      }
    }

    if (confirmLeave) {
        ConfirmDialog(
            stringResource(R.string.discard_changes_title), stringResource(R.string.discard_changes_body),
            stringResource(R.string.discard), onBack, { confirmLeave = false }, icon = Icons.Outlined.EditOff,
        )
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: Int, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.width(72.dp).clip(RoundedCornerShape(20.dp)).selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp, 32.dp).clip(RoundedCornerShape(50))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = color) }
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun rendered(page: PageEntity, crop: Quad?, rotation: Int, filter: Filter, maxDim: Int): ImageBitmap? {
    val processor = koinInject<ImageProcessor>()
    val bmp by produceState<ImageBitmap?>(null, page.id, crop?.contentToString(), rotation, filter, maxDim) {
        value = runCatching { processor.render(page, maxDim, crop, rotation, filter, 0f, 0f, emptyList()) }.getOrNull()?.asImageBitmap()
    }
    return bmp
}

private fun adjustFilter(brightness: Float, contrast: Float): ColorFilter? =
    if (brightness == 0f && contrast == 0f) null else ColorFilter.colorMatrix(ColorMatrix(adjustMatrix(brightness, contrast)))

private fun DrawScope.drawInk(r: FloatArray, color: Int, width: Float, pts: List<Float>) {
    if (pts.size < 4) return
    val path = Path().apply {
        moveTo(r[0] + pts[0] * r[2], r[1] + pts[1] * r[3])
        var i = 2
        while (i + 1 < pts.size) { lineTo(r[0] + pts[i] * r[2], r[1] + pts[i + 1] * r[3]); i += 2 }
    }
    drawPath(path, Color(color), style = Stroke(width * r[2], cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun Preview(page: PageEntity, s: EditState) {
    val image = rendered(page, s.crop, s.rotation, s.filter, 1600) ?: return CircularProgressIndicator()
    val z = remember { ZoomState() }
    BoxWithConstraints(
        Modifier.fillMaxSize().clipToBounds().zoomable(z).pointerInput(z) {
            detectTransformGestures { centroid, pan, zoomChange, _ -> z.transform(centroid, pan, zoomChange) }
        },
    ) {
        val r = fit(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), image.width, image.height)
        Box(Modifier.fillMaxSize().zoomed(z)) {
            Image(image, stringResource(R.string.page_preview), Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = adjustFilter(s.brightness, s.contrast))
            if (s.strokes.isNotEmpty()) Canvas(Modifier.fillMaxSize()) { s.strokes.forEach { drawInk(r, it.color, it.width, it.points) } }
        }
    }
}

@Composable
private fun FilterRow(page: PageEntity, s: EditState, onSelect: (Filter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Filter.entries.forEach { f ->
            val selected = f == s.filter
            Column(
                Modifier.width(76.dp).selectable(selected = selected, role = Role.RadioButton) { onSelect(f) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(64.dp, 80.dp).clip(ThumbShape).border(
                        BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        ThumbShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = rendered(page, s.crop, s.rotation, f, 200)
                    if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp))
                }
                Text(stringResource(filterLabel(f)), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AdjustPanel(s: EditState, vm: EditorViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        SliderRow(Icons.Outlined.Brightness6, R.string.brightness, s.brightness, vm::setBrightness)
        SliderRow(Icons.Outlined.Contrast, R.string.contrast, s.contrast, vm::setContrast)
        if (s.brightness != 0f || s.contrast != 0f) TextButton(onClick = vm::resetAdjust, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.reset_adjustments))
        }
    }
}

@Composable
private fun SliderRow(icon: ImageVector, label: Int, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(88.dp), maxLines = 1)
        Slider(value, onChange, Modifier.weight(1f), valueRange = -1f..1f)
        Text("${(value * 100).roundToInt()}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun DrawPanel(s: EditState, vm: EditorViewModel) {
    val names = listOf(R.string.pen_black, R.string.pen_blue, R.string.pen_red, R.string.pen_green)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PEN_COLORS.forEachIndexed { i, c ->
                val on = s.penColor == c
                val name = stringResource(names[i])
                Box(
                    Modifier.padding(end = 10.dp).size(36.dp).clip(CircleShape)
                        .border(if (on) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(if (on) 5.dp else 2.dp).clip(CircleShape).background(Color(c))
                        .bounceClick(role = Role.RadioButton) { vm.setPen(color = c) }.semantics { contentDescription = name },
                )
            }
            Spacer(Modifier.weight(1f))
            CircleButton(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.undo), vm::undoStroke)
            Spacer(Modifier.width(6.dp))
            CircleButton(Icons.Outlined.DeleteSweep, stringResource(R.string.clear_ink), vm::clearInk)
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.pen_fine, R.string.pen_medium, R.string.pen_bold).forEachIndexed { i, label ->
                AppChip(s.penWidth == PEN_WIDTHS[i], { vm.setPen(width = PEN_WIDTHS[i]) }) { Text(stringResource(label)) }
            }
        }
        Text(stringResource(R.string.draw_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun fit(bw: Float, bh: Float, iw: Int, ih: Int): FloatArray {
    val scale = minOf(bw / iw, bh / ih)
    val w = iw * scale; val h = ih * scale
    return floatArrayOf((bw - w) / 2, (bh - h) / 2, w, h)
}

@Composable
private fun DrawView(page: PageEntity, s: EditState, onStroke: (List<Float>) -> Unit) {
    val bmp = rendered(page, s.crop, s.rotation, s.filter, 1600) ?: return CircularProgressIndicator()
    val current = remember { mutableStateListOf<Float>() }
    val penColor by rememberUpdatedState(s.penColor)
    val penWidth by rememberUpdatedState(s.penWidth)
    val label = stringResource(R.string.draw_area)
    val z = remember { ZoomState() }
    BoxWithConstraints(
        Modifier.fillMaxSize().clipToBounds().onSizeChanged { z.size = Size(it.width.toFloat(), it.height.toFloat()) }
            .semantics { contentDescription = label },
    ) {
        val r = fit(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), bmp.width, bmp.height)
        Box(Modifier.fillMaxSize().zoomed(z)) {
            Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = adjustFilter(s.brightness, s.contrast))
            Canvas(Modifier.fillMaxSize()) {
                s.strokes.forEach { drawInk(r, it.color, it.width, it.points) }
                drawInk(r, penColor, penWidth, current)
            }
        }
        Box(
            Modifier.fillMaxSize().pointerInput(r[2], r[3]) {
                fun norm(o: Offset) = z.toContent(o).let { listOf(((it.x - r[0]) / r[2]).coerceIn(0f, 1f), ((it.y - r[1]) / r[3]).coerceIn(0f, 1f)) }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    current.clear(); current.addAll(norm(down.position))
                    var pinching = false
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            pinching = true
                            current.clear()
                            z.transform(event.calculateCentroid(), event.calculatePan(), event.calculateZoom())
                        } else if (pressed.size == 1 && !pinching) {
                            current.addAll(norm(pressed[0].position))
                        }
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (!pinching) onStroke(current.toList())
                    current.clear()
                }
            },
        )
    }
}

@Composable
private fun CropView(page: PageEntity, crop: Quad, rotation: Int, onChange: (Quad) -> Unit) {
    val image = rendered(page, null, rotation, Filter.ORIGINAL, 1600) ?: return CircularProgressIndicator()
    val density = LocalDensity.current
    val handleRadius = with(density) { 11.dp.toPx() }
    val touchSlop = with(density) { 40.dp.toPx() }
    val loupeRadius = with(density) { 58.dp.toPx() }
    val cropLabel = stringResource(R.string.crop_area)
    val brand = LocalScanColors.current
    val accent = brand.heroStart
    val z = remember { ZoomState() }
    var live by remember(crop, rotation) { mutableStateOf(crop.rotated(rotation)) }
    var active by remember { mutableIntStateOf(-1) }
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
        val r = fit(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), image.width, image.height)
        val left = r[0]; val top = r[1]; val w = r[2]; val h = r[3]
        Image(image, null, Modifier.fillMaxSize().zoomed(z), contentScale = ContentScale.Fit)

        fun handle(d: FloatArray, i: Int): Offset = if (i < 4) Offset(left + d[i * 2] * w, top + d[i * 2 + 1] * h) else {
            val a = i - 4; val b = (a + 1) % 4
            Offset(left + (d[a * 2] + d[b * 2]) / 2 * w, top + (d[a * 2 + 1] + d[b * 2 + 1]) / 2 * h)
        }

        Canvas(
            Modifier.fillMaxSize()
                .semantics { contentDescription = cropLabel }
                .zoomable(z)
                .pointerInput(rotation, w, h) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        active = (0 until 8).minBy { (z.toScreen(handle(live, it)) - down.position).getDistance() }
                            .takeIf { (z.toScreen(handle(live, it)) - down.position).getDistance() < touchSlop } ?: -1
                        var moved = false
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size >= 2) {
                                active = -1
                                z.transform(event.calculateCentroid(), event.calculatePan(), event.calculateZoom())
                                event.changes.forEach { it.consume() }
                            } else if (pressed.size == 1) {
                                val change = pressed[0]
                                val delta = change.position - change.previousPosition
                                if (active >= 0) {
                                    val d = live.copyOf()
                                    val corners = if (active < 4) listOf(active) else listOf(active - 4, (active - 3) % 4)
                                    corners.forEach { c -> d[c * 2] += delta.x / z.zoom / w; d[c * 2 + 1] += delta.y / z.zoom / h }
                                    live = clampQuad(d)
                                    moved = true
                                    change.consume()
                                } else if (z.zoom > 1f && delta != Offset.Zero) {
                                    z.transform(change.position, delta, 1f)
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        active = -1
                        if (moved) onChange(live.rotated(360 - rotation))
                    }
                },
        ) {
            val d = live
            fun pt(i: Int) = z.toScreen(Offset(left + d[i * 2] * w, top + d[i * 2 + 1] * h))
            val quad = Path().apply { moveTo(pt(0).x, pt(0).y); for (i in 1 until 4) lineTo(pt(i).x, pt(i).y); close() }

            val outside = Path().apply { addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)); addPath(quad); fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd }
            drawPath(outside, Color.Black.copy(alpha = 0.45f))
            drawPath(quad, Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)), style = Stroke(2.5.dp.toPx(), join = StrokeJoin.Round))
            for (i in 0 until 4) {
                drawCircle(Color.White, handleRadius, pt(i))
                drawCircle(accent, handleRadius, pt(i), style = Stroke(3.dp.toPx()))
            }
            for (i in 4 until 8) {
                val m = z.toScreen(handle(d, i))
                drawCircle(Color.White, handleRadius * 0.62f, m)
                drawCircle(accent, handleRadius * 0.62f, m, style = Stroke(2.dp.toPx()))
            }

            if (active >= 0) {
                val focus = handle(d, active)
                val screenFocus = z.toScreen(focus)
                val zoom = 2.6f * z.zoom
                val contentQuad = Path().apply {
                    moveTo(left + d[0] * w, top + d[1] * h); for (i in 1 until 4) lineTo(left + d[i * 2] * w, top + d[i * 2 + 1] * h); close()
                }

                val cx = if (screenFocus.x < size.width / 2) size.width - loupeRadius - 12.dp.toPx() else loupeRadius + 12.dp.toPx()
                val center = Offset(cx, loupeRadius + 12.dp.toPx())
                val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, loupeRadius)) }
                drawCircle(Color.Black.copy(alpha = 0.25f), loupeRadius + 6.dp.toPx(), center + Offset(0f, 3.dp.toPx()))
                clipPath(circle) {
                    drawRect(Color.Black)
                    withTransform({
                        translate(center.x - focus.x * zoom, center.y - focus.y * zoom)
                        scale(zoom, zoom, Offset.Zero)
                    }) {
                        drawImage(image, dstOffset = IntOffset(left.roundToInt(), top.roundToInt()), dstSize = IntSize(w.roundToInt(), h.roundToInt()))
                        drawPath(contentQuad, accent, style = Stroke(2.dp.toPx() / zoom))
                    }
                    val arm = 10.dp.toPx()
                    drawLine(Color.White, center - Offset(arm, 0f), center + Offset(arm, 0f), 2.dp.toPx())
                    drawLine(Color.White, center - Offset(0f, arm), center + Offset(0f, arm), 2.dp.toPx())
                    drawCircle(accent, 3.dp.toPx(), center)
                }
                drawCircle(Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd), center - Offset(loupeRadius, loupeRadius), center + Offset(loupeRadius, loupeRadius)), loupeRadius, center, style = Stroke(4.dp.toPx()))
            }
        }
    }
}
