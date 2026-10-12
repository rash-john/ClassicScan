package com.classicscan.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.History
import com.classicscan.ui.camera.ScanMode
import com.classicscan.ui.camera.modeIcon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classicscan.R
import com.classicscan.data.DocumentEntity
import com.classicscan.data.DocumentRepository
import com.classicscan.data.PageEntity
import com.classicscan.data.Settings
import com.classicscan.data.SortOrder
import com.classicscan.ui.ConfirmDialog
import com.classicscan.ui.LocalWindowLayout
import com.classicscan.ui.NameDialog
import com.classicscan.ui.PageImage
import com.classicscan.ui.components.AppChip
import com.classicscan.ui.components.AppMenu
import com.classicscan.ui.components.CircleButton
import com.classicscan.ui.components.EmptyState
import com.classicscan.ui.components.ScanCard
import com.classicscan.ui.components.ScreenHeader
import com.classicscan.ui.components.bounceClick
import com.classicscan.ui.components.popIn
import com.classicscan.ui.formatDate
import com.classicscan.ui.formatSize
import com.classicscan.ui.theme.LocalScanColors
import com.classicscan.ui.theme.ThumbShape
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalTime

@Composable
fun HomeScreen(
    settings: Settings,
    importRequested: Boolean,
    onImportHandled: () -> Unit,
    onScan: (String, ScanMode) -> Unit,
    onCodes: () -> Unit,
    onReview: (String) -> Unit,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = koinViewModel(),
) {
    val docs by vm.documents.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val unindexed by vm.unindexed.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val favoritesOnly by vm.favoritesOnly.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        vm.import(uris, onReview)
    }
    val importImages = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    LaunchedEffect(importRequested) { if (importRequested) { onImportHandled(); importImages() } }

    val messageText = message?.let {
        stringResource(
            when (it) {
                HomeMessage.IMPORT_FAILED -> R.string.import_failed
                HomeMessage.IMPORT_PARTIAL -> R.string.import_partial
                HomeMessage.DELETED -> R.string.document_deleted
                HomeMessage.DUPLICATED -> R.string.document_duplicated
                HomeMessage.ERROR -> R.string.generic_error
            }
        )
    }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            vm.message.value = null
        }
    }

    val window = LocalWindowLayout.current
    val wide = window.medium || window.landscape
    val greeting = stringResource(
        when (LocalTime.now().hour) { in 5..11 -> R.string.greeting_morning; in 12..16 -> R.string.greeting_afternoon; else -> R.string.greeting_evening },
    )
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            if (settings.grid) GridCells.Adaptive(156.dp) else if (wide) GridCells.Adaptive(380.dp) else GridCells.Fixed(1),
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(if (settings.grid) 12.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(if (settings.grid) 12.dp else 0.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (searching) {
                    SearchBar(query, { vm.query.value = it }) { searching = false; vm.query.value = "" }
                } else {
                    ScreenHeader(stringResource(R.string.documents), greeting) {
                        CircleButton(Icons.Filled.Search, stringResource(R.string.search), { searching = true })
                        Spacer(Modifier.width(6.dp))
                        CircleButton(Icons.Filled.Settings, stringResource(R.string.settings), onSettings)
                    }
                }
            }
            if (!searching) item(span = { GridItemSpan(maxLineSpan) }) {
                val tools = @Composable { m: Modifier -> Tools({ mode -> vm.scan { onScan(it, mode) } }, onCodes, m.popIn(1)) }
                if (wide) Row(verticalAlignment = Alignment.CenterVertically) {
                    Hero(stats, unindexed, busy, importImages, Modifier.weight(1f).popIn(0))
                    tools(Modifier.weight(1f))
                } else Column {
                    Hero(stats, unindexed, busy, importImages, Modifier.popIn(0))
                    tools(Modifier)
                }
            }
            draft?.let { d ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DraftCard(d, onResume = { onReview(d.id) }, onDiscard = { vm.discardDraft(d.id) })
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                FilterRow(settings, favoritesOnly, { vm.favoritesOnly.value = it }, vm::setSort, { vm.setGrid(!settings.grid) })
            }
            val list = docs
            when {
                list == null -> Unit
                list.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    when {
                        query.isNotBlank() -> EmptyState(Icons.Filled.SearchOff, stringResource(R.string.no_results), stringResource(R.string.no_results_body))
                        favoritesOnly -> EmptyState(Icons.Filled.StarBorder, stringResource(R.string.no_favorites), stringResource(R.string.no_favorites_body))
                        else -> EmptyState(Icons.Filled.Description, stringResource(R.string.empty_title), stringResource(R.string.empty_body))
                    }
                }
                settings.grid -> items(list, key = { it.doc.id }) { item ->
                    Box(Modifier.padding(horizontal = 6.dp).animateItem()) { DocumentCell(item, vm, onOpen) }
                }
                else -> items(list, key = { it.doc.id }) { item ->
                    DocumentRow(item, vm, onOpen, Modifier.animateItem())
                }
            }
        }
        val brand = LocalScanColors.current
        FloatingActionButton(
            onClick = { if (!busy) vm.scan { onScan(it, ScanMode.DOCUMENT) } },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 20.dp).size(68.dp),
            shape = RoundedCornerShape(22.dp),
            containerColor = brand.heroStart,
            contentColor = brand.onHero,
        ) {
            Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.CameraAlt, stringResource(R.string.scan), Modifier.size(30.dp)) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 100.dp))
    }
}

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.close_search), onClose)
        Spacer(Modifier.width(8.dp))
        TextField(
            value = query, onValueChange = onChange, singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f).focusRequester(focus),
        )
    }
}

@Composable
private fun Hero(stats: LibraryStats?, unindexed: Int, busy: Boolean, onImport: () -> Unit, modifier: Modifier) {
    val brand = LocalScanColors.current
    val shape = RoundedCornerShape(32.dp)
    Column(
        modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
            .shadow(20.dp, shape, ambientColor = brand.heroStart, spotColor = brand.heroStart).clip(shape)
            .background(Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)))
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.16f), size.height * 0.55f, Offset(size.width * 0.95f, size.height * 0.05f))
                drawCircle(Color.White.copy(alpha = 0.1f), size.height * 0.3f, Offset(size.width * 0.72f, size.height * 0.95f))
            }
            .padding(24.dp),
    ) {
        Text(stringResource(R.string.your_library), style = MaterialTheme.typography.titleSmall, color = brand.onHero.copy(alpha = 0.85f))
        Spacer(Modifier.height(4.dp))
        val s = stats ?: LibraryStats(0, 0, 0)
        Text(
            pluralStringResource(R.plurals.document_count, s.documents, s.documents),
            style = if (LocalWindowLayout.current.shortHeight) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall, color = brand.onHero, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            pluralStringResource(R.plurals.page_count, s.pages, s.pages) + " · " + formatSize(s.bytes) + " · " + stringResource(R.string.on_this_device),
            style = MaterialTheme.typography.bodyMedium, color = brand.onHero.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f))
                    .bounceClick(enabled = !busy, onClick = onImport).heightIn(min = 48.dp).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.PhotoLibrary, null, tint = brand.onHero, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.import_photos), style = MaterialTheme.typography.titleSmall, color = brand.onHero)
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(unindexed > 0 || busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = brand.onHero, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (busy) stringResource(R.string.working) else stringResource(R.string.reading_text),
                        style = MaterialTheme.typography.labelMedium, color = brand.onHero,
                    )
                }
            }
        }
    }
}

@Composable
private fun Tools(onMode: (ScanMode) -> Unit, onCodes: () -> Unit, modifier: Modifier) {
    val tools = listOf(
        Triple(ScanMode.DOCUMENT, R.string.mode_document, Color(0xFF2F6BFF)),
        Triple(ScanMode.ID_CARD, R.string.mode_id_card, Color(0xFFE84393)),
        Triple(ScanMode.BOOK, R.string.mode_book, Color(0xFFFF9F43)),
        Triple(ScanMode.QR, R.string.mode_qr, Color(0xFF6C5CE7)),
        Triple(ScanMode.WHITEBOARD, R.string.mode_whiteboard, Color(0xFF00B894)),
    )
    Column(modifier) {
        Text(stringResource(R.string.tools), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(tools) { (mode, label, color) -> ToolTile(modeIcon(mode), color, stringResource(label)) { onMode(mode) } }
            item { ToolTile(Icons.Filled.History, Color(0xFF607D8B), stringResource(R.string.scanned_codes), onCodes) }
        }
    }
}

@Composable
private fun ToolTile(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Column(
        Modifier.width(84.dp).clip(RoundedCornerShape(20.dp)).bounceClick(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        com.classicscan.ui.components.IconTile(icon, color, 56)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DraftCard(draft: DocumentEntity, onResume: () -> Unit, onDiscard: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    ScanCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).popIn(1), padding = 16) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.classicscan.ui.components.IconTile(Icons.Filled.Description, LocalScanColors.current.warning, 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.unsaved_scan_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.page_count, draft.pageCount, draft.pageCount),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { confirm = true }) { Text(stringResource(R.string.discard)) }
            TextButton(onClick = onResume) { Text(stringResource(R.string.resume)) }
        }
    }
    if (confirm) {
        ConfirmDialog(
            stringResource(R.string.discard_scan_title), stringResource(R.string.discard_scan_body),
            stringResource(R.string.discard), onDiscard, { confirm = false },
        )
    }
}

@Composable
private fun FilterRow(
    settings: Settings, favoritesOnly: Boolean, onFavorites: (Boolean) -> Unit,
    onSort: (SortOrder) -> Unit, onToggleGrid: () -> Unit,
) {
    var sortMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        AppChip(!favoritesOnly, { onFavorites(false) }) { Text(stringResource(R.string.all)) }
        Spacer(Modifier.width(8.dp))
        AppChip(favoritesOnly, { onFavorites(true) }) {
            Icon(Icons.Filled.Star, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.favorites))
        }
        Spacer(Modifier.weight(1f))
        Box {
            CircleButton(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_by, stringResource(sortLabel(settings.sort))), { sortMenu = true })
            AppMenu(sortMenu, { sortMenu = false }) {
                SortOrder.entries.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(stringResource(sortLabel(s))) },
                        leadingIcon = { if (s == settings.sort) Icon(Icons.Filled.Star, null, Modifier.size(16.dp)) },
                        onClick = { onSort(s); sortMenu = false },
                    )
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        if (settings.grid) CircleButton(Icons.AutoMirrored.Filled.ViewList, stringResource(R.string.list_view), onToggleGrid)
        else CircleButton(Icons.Filled.GridView, stringResource(R.string.grid_view), onToggleGrid)
    }
}

fun sortLabel(s: SortOrder) = when (s) {
    SortOrder.MODIFIED -> R.string.sort_modified
    SortOrder.CREATED -> R.string.sort_created
    SortOrder.NAME -> R.string.sort_name
    SortOrder.SIZE -> R.string.sort_size
}

@Composable
private fun CoverThumb(doc: DocumentEntity, modifier: Modifier) {
    val repo = koinInject<DocumentRepository>()
    var cover by remember(doc.coverPageId, doc.updatedAt) { mutableStateOf<PageEntity?>(null) }
    LaunchedEffect(doc.coverPageId, doc.updatedAt) { cover = doc.coverPageId?.let { repo.page(it) } }
    Box(
        modifier.clip(ThumbShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), ThumbShape),
        contentAlignment = Alignment.Center,
    ) {
        cover?.let { PageImage(it, 300, null, Modifier.fillMaxSize(), ContentScale.Crop) }
            ?: Icon(Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Meta(item: DocItem) {
    val doc = item.doc
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (item.textMatch) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.TextSnippet, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.text_match), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(
            pluralStringResource(R.plurals.page_count, doc.pageCount, doc.pageCount) + " · " + formatDate(doc.updatedAt),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DocumentRow(item: DocItem, vm: HomeViewModel, onOpen: (String) -> Unit, modifier: Modifier) {
    val doc = item.doc
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp).clip(RoundedCornerShape(22.dp))
            .bounceClick(onClickLabel = stringResource(R.string.open)) { onOpen(doc.id) }
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverThumb(doc, Modifier.size(56.dp, 72.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(doc.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (doc.isFavorite) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Star, stringResource(R.string.favorite), Modifier.size(16.dp), tint = LocalScanColors.current.warning)
                }
            }
            Spacer(Modifier.height(4.dp))
            Meta(item)
        }
        DocumentMenu(doc, vm, onOpen)
    }
}

@Composable
private fun DocumentCell(item: DocItem, vm: HomeViewModel, onOpen: (String) -> Unit) {
    val doc = item.doc
    ScanCard(Modifier.padding(4.dp), onClick = { onOpen(doc.id) }, padding = 10) {
        Box {
            CoverThumb(doc, Modifier.fillMaxWidth().aspectRatio(0.77f))
            if (doc.isFavorite) Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Star, stringResource(R.string.favorite), Modifier.size(16.dp), tint = LocalScanColors.current.warning) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(top = 8.dp, start = 4.dp)) {
                Text(doc.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Meta(item)
            }
            DocumentMenu(doc, vm, onOpen)
        }
    }
}

@Composable
private fun DocumentMenu(doc: DocumentEntity, vm: HomeViewModel, onOpen: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    val copySuffix = stringResource(R.string.copy_suffix)
    val label = stringResource(R.string.more_options_for, doc.title)
    Box {
        Box(
            Modifier.size(48.dp).clip(CircleShape).bounceClick { menu = true }.semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        AppMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.open)) }, onClick = { menu = false; onOpen(doc.id) })
            DropdownMenuItem(
                text = { Text(stringResource(if (doc.isFavorite) R.string.unfavorite else R.string.add_favorite)) },
                onClick = { menu = false; vm.toggleFavorite(doc) },
            )
            DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = { menu = false; rename = true })
            DropdownMenuItem(text = { Text(stringResource(R.string.duplicate)) }, onClick = { menu = false; vm.duplicate(doc.id, "${doc.title} $copySuffix") })
            DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; delete = true })
        }
    }
    if (rename) NameDialog(stringResource(R.string.rename), doc.title, { vm.rename(doc.id, it) }, { rename = false })
    if (delete) {
        ConfirmDialog(
            stringResource(R.string.delete_title, doc.title), stringResource(R.string.delete_body),
            stringResource(R.string.delete), { vm.delete(doc.id) }, { delete = false },
        )
    }
}

