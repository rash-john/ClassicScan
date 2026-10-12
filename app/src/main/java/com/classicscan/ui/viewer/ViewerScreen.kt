package com.classicscan.ui.viewer

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.calculateCentroid
import com.classicscan.ui.ZoomState
import com.classicscan.ui.zoomable
import com.classicscan.ui.zoomed
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize as PagerPageSize
import androidx.compose.foundation.layout.fillMaxHeight
import com.classicscan.ui.LocalWindowLayout
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.classicscan.ui.components.AppButton
import com.classicscan.ui.components.AppChip
import com.classicscan.ui.components.AppMenu
import com.classicscan.ui.components.bounceClick
import com.classicscan.ui.theme.CardShape
import com.classicscan.ui.theme.LocalScanColors
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classicscan.R
import com.classicscan.data.PageEntity
import com.classicscan.data.Settings
import com.classicscan.export.ExportFormat
import com.classicscan.export.PageSize
import com.classicscan.export.Quality
import com.classicscan.export.sanitizeFileName
import com.classicscan.ui.ConfirmDialog
import com.classicscan.ui.NameDialog
import com.classicscan.ui.PageImage
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    settings: Settings,
    onBack: () -> Unit,
    onOrganize: () -> Unit,
    onAddPage: () -> Unit,
    onEditPage: (String) -> Unit,
    onOpenCopy: (String) -> Unit,
    vm: ViewerViewModel = koinViewModel(),
) {
    val doc by vm.document.collectAsStateWithLifecycle()
    val pages by vm.pages.collectAsStateWithLifecycle()
    val export by vm.export.collectAsStateWithLifecycle()
    val list = pages.orEmpty()

    val latest by rememberUpdatedState(list)
    val pager = rememberPagerState { latest.size }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val gone by vm.gone.collectAsStateWithLifecycle()
    LaunchedEffect(gone) { if (gone) onBack() }

    LaunchedEffect(export) {
        val e = export
        if (e is ExportState.Done && e.shareNow) {
            context.startActivity(vm.exporter.shareIntent(e.files, e.format))
            vm.dismissExport()
        }
    }

    var textSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                title = { Text(doc?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    val fav = doc?.isFavorite == true
                    IconButton(onClick = vm::toggleFavorite) {
                        Icon(
                            if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            stringResource(if (fav) R.string.unfavorite else R.string.add_favorite),
                            tint = if (fav) LocalScanColors.current.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options)) }
                        AppMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = { menu = false; renaming = true })
                            DropdownMenuItem(text = { Text(stringResource(R.string.organize_pages)) }, onClick = { menu = false; onOrganize() })
                            val copySuffix = stringResource(R.string.copy_suffix)
                            DropdownMenuItem(text = { Text(stringResource(R.string.duplicate)) }, onClick = { menu = false; vm.duplicate("${doc?.title} $copySuffix", onOpenCopy) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; deleting = true })
                        }
                    }
                },
            )
        },
    ) { padding ->
        val window = LocalWindowLayout.current

        val rail = window.landscape && window.shortHeight

        val spread = (window.expanded || window.book) && list.size > 1
        val toolButtons: @Composable () -> Unit = {
            BarButton(Icons.Outlined.Edit, R.string.edit) { list.getOrNull(pager.currentPage)?.let { onEditPage(it.id) } }
            BarButton(Icons.Outlined.AddAPhoto, R.string.add_page, onAddPage)
            BarButton(Icons.Outlined.TextSnippet, R.string.text) { textSheet = true }
            BarButton(Icons.Outlined.Share, R.string.share) {
                vm.startExport(ExportFormat.PDF, settings.pageSize, settings.quality, settings.searchablePdf, null, shareNow = true)
            }
            BarButton(Icons.Outlined.PictureAsPdf, R.string.export) { sheet = true }
        }
        Row(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (export is ExportState.Running && !sheet) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (pages != null && list.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_pages)) }
                } else {
                    Box(
                        Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).clip(CardShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        var zoomed by remember { mutableStateOf(false) }
                        HorizontalPager(
                            pager, userScrollEnabled = !zoomed, beyondViewportPageCount = 1,
                            pageSize = if (spread) HalfPage else PagerPageSize.Fill, pageSpacing = if (spread) 8.dp else 0.dp,
                            key = { latest.getOrNull(it)?.id ?: it },
                        ) { i ->
                            latest.getOrNull(i)?.let { ZoomablePage(it, i, latest.size) { z -> zoomed = z } }
                        }
                        val first = pager.currentPage + 1
                        Text(
                            if (spread && first < list.size) stringResource(R.string.page_range, first, first + 1, list.size)
                            else stringResource(R.string.page_of, first, list.size),
                            style = MaterialTheme.typography.labelLarge, color = Color.White,
                            modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }

                    if (list.size > 1 && !window.shortHeight) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(list, key = { _, p -> p.id }) { i, p ->
                                val selected = i == pager.currentPage
                                PageImage(
                                    p, 160, stringResource(R.string.page_n, i + 1),
                                    Modifier.size(44.dp, 58.dp).clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .border(if (selected) 2.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                        .bounceClick { scope.launch { pager.animateScrollToPage(i) } },
                                    ContentScale.Crop,
                                )
                            }
                        }
                    }
                }

                if (!rail) Row(
                    Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) { toolButtons() }
            }
            if (rail) Column(
                Modifier.fillMaxHeight().navigationBarsPadding().padding(end = 12.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally,
            ) { toolButtons() }
        }
    }

    if (textSheet) list.getOrNull(pager.currentPage)?.let { TextSheet(it, pager.currentPage) { textSheet = false } }

    if (sheet) ExportSheet(vm, settings, export, pager.currentPage, list.size) { sheet = false; vm.dismissExport() }
    if (export is ExportState.Failed && !sheet) {
        com.classicscan.ui.components.AppDialog(
            Icons.Outlined.ErrorOutline, stringResource(R.string.export_failed), vm::dismissExport,
            confirm = stringResource(R.string.ok), onConfirm = vm::dismissExport, danger = true,
            body = stringResource(R.string.export_failed_body),
        )
    }
    if (renaming) NameDialog(stringResource(R.string.rename), doc?.title.orEmpty(), vm::rename, { renaming = false })
    if (deleting) {
        ConfirmDialog(
            stringResource(R.string.delete_title, doc?.title.orEmpty()), stringResource(R.string.delete_body),
            stringResource(R.string.delete), { vm.delete(onBack) }, { deleting = false },
        )
    }
}

private val HalfPage = object : PagerPageSize {
    override fun androidx.compose.ui.unit.Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int) = (availableSpace - pageSpacing) / 2
}

@Composable
private fun BarButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(20.dp)).bounceClick(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextSheet(page: PageEntity, index: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val text = page.ocrText
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.page_text, index + 1), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.ocr_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            when {
                text == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.reading_text))
                }
                text.isBlank() -> Text(stringResource(R.string.no_text_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    Box(
                        Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh).verticalScroll(rememberScrollState()).padding(16.dp),
                    ) { SelectionContainer { Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Default)) } }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppButton(stringResource(R.string.copy_text), { clipboard.setText(AnnotatedString(text)); onDismiss() }, icon = Icons.Outlined.ContentCopy)
                        AppButton(stringResource(R.string.share), {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
                        }, tonal = true, icon = Icons.Outlined.Share)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomablePage(page: PageEntity, index: Int, count: Int, onZoomed: (Boolean) -> Unit) {
    val z = remember { ZoomState() }
    val onZoomedNow by rememberUpdatedState(onZoomed)
    LaunchedEffect(z) { snapshotFlow { z.zoom > 1f }.collect { onZoomedNow(it) } }
    DisposableEffect(z) { onDispose { if (z.zoom > 1f) onZoomedNow(false) } }
    val screen = with(LocalDensity.current) { (LocalConfiguration.current.screenHeightDp.dp.toPx() * 1.5f).toInt() }
    val sharp by remember { derivedStateOf { z.zoom > 1.8f } }
    Box(
        Modifier.fillMaxSize().clipToBounds().zoomable(z).pointerInput(z) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val fingers = event.changes.count { it.pressed }
                    if (fingers >= 2 || (fingers == 1 && z.zoom > 1f)) {
                        z.transform(event.calculateCentroid(), event.calculatePan(), event.calculateZoom())
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            }
        },
    ) {
        PageImage(
            page, if (sharp) (screen * 2).coerceAtMost(2600) else screen, stringResource(R.string.page_of, index + 1, count),
            Modifier.fillMaxSize().zoomed(z).padding(12.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(vm: ViewerViewModel, settings: Settings, state: ExportState, current: Int, pageCount: Int, onDismiss: () -> Unit) {
    var format by rememberSaveable { mutableStateOf(ExportFormat.PDF) }
    var size by rememberSaveable { mutableStateOf(settings.pageSize) }
    var quality by rememberSaveable { mutableStateOf(settings.quality) }
    var onlyCurrent by rememberSaveable { mutableStateOf(false) }
    var searchable by rememberSaveable { mutableStateOf(settings.searchablePdf) }
    val context = LocalContext.current
    val doc by vm.document.collectAsStateWithLifecycle()
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(format.mime)) { vm.saveTo(it, folder = false) }
    val saveFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { vm.saveTo(it, folder = true) }

    ModalBottomSheet(onDismissRequest = { if (state !is ExportState.Running) onDismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.export), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            when (state) {
                is ExportState.Running -> {
                    Text(
                        stringResource(R.string.exporting_progress, state.done, state.total),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator({ if (state.total == 0) 0f else state.done.toFloat() / state.total }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = vm::cancelExport) { Text(stringResource(R.string.cancel)) }
                }
                is ExportState.Done -> {
                    Text(stringResource(R.string.export_ready), color = LocalScanColors.current.success, style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.files.joinToString { it.name } + " · " + com.classicscan.ui.formatSize(state.files.sumOf { it.length() }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppButton(stringResource(R.string.share), { context.startActivity(vm.exporter.shareIntent(state.files, state.format)) }, icon = Icons.Outlined.Share)
                        AppButton(stringResource(R.string.save_to_device), {
                            if (state.files.size == 1) saveFile.launch(state.files.first().name) else saveFolder.launch(null)
                        }, tonal = true)
                    }
                }
                ExportState.Saved -> {
                    Text(stringResource(R.string.saved_to_device), color = LocalScanColors.current.success, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.export_copy_note), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    AppButton(stringResource(R.string.done), onDismiss)
                }
                else -> {
                    if (state is ExportState.Failed) {
                        Text(stringResource(R.string.export_failed_body), color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }
                    Section(R.string.format)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportFormat.entries.forEach { f ->
                            AppChip(format == f, { format = f }) { Text(f.ext.uppercase()) }
                        }
                    }
                    if (format == ExportFormat.PDF) {
                        Section(R.string.page_size)
                        PageSize.entries.forEach { s -> RadioRow(stringResource(pageSizeLabel(s)), size == s) { size = s } }
                    }
                    if (format == ExportFormat.PDF) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .toggleable(searchable, role = Role.Switch) { searchable = it }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.ManageSearch, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.searchable_pdf), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.searchable_pdf_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(searchable, null)
                        }
                    }
                    if (format != ExportFormat.PNG) {
                        Section(R.string.quality)
                        Quality.entries.forEach { q -> RadioRow(stringResource(qualityLabel(q)), quality == q) { quality = q } }
                    }
                    if (pageCount > 1) {
                        Section(R.string.pages)
                        RadioRow(stringResource(R.string.all_pages, pageCount), !onlyCurrent) { onlyCurrent = false }
                        RadioRow(stringResource(R.string.current_page, current + 1), onlyCurrent) { onlyCurrent = true }
                    }
                    Spacer(Modifier.height(16.dp))
                    AppButton(
                        stringResource(R.string.create_file, format.ext.uppercase()),
                        { vm.startExport(format, size, quality, searchable && format == ExportFormat.PDF, if (onlyCurrent) current else null) },
                        Modifier.fillMaxWidth(),
                    )
                    Text(
                        sanitizeFileName(doc?.title.orEmpty()) + "." + format.ext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(label: Int) {
    Spacer(Modifier.height(12.dp))
    Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
}

@Composable
fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.padding(start = 12.dp))
        Text(label)
    }
}

fun pageSizeLabel(s: PageSize) = when (s) {
    PageSize.AUTO -> R.string.size_auto
    PageSize.A4 -> R.string.size_a4
    PageSize.LETTER -> R.string.size_letter
    PageSize.ORIGINAL -> R.string.size_original
}

fun qualityLabel(q: Quality) = when (q) {
    Quality.SMALL -> R.string.quality_small
    Quality.BALANCED -> R.string.quality_balanced
    Quality.HIGH -> R.string.quality_high
}

