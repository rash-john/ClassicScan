package com.classicscan.ui.session

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.classicscan.ui.components.AppButton
import com.classicscan.ui.components.bounceClick
import com.classicscan.ui.components.popIn
import com.classicscan.ui.theme.ThumbShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classicscan.R
import com.classicscan.data.PageEntity
import com.classicscan.ui.ConfirmDialog
import com.classicscan.ui.NameDialog
import com.classicscan.ui.PageImage
import com.classicscan.ui.suggestedTitle
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onBack: () -> Unit,
    onAddPages: () -> Unit,
    onEdit: (String) -> Unit,
    onSaved: () -> Unit,
    vm: SessionViewModel = koinViewModel(),
) {
    val doc by vm.document.collectAsStateWithLifecycle()
    val pages by vm.pages.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    val suggestion by vm.suggestion.collectAsStateWithLifecycle()
    var naming by remember { mutableStateOf(false) }
    var discarding by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { vm.import(it) }
    val draft = vm.isDraft
    val list = pages.orEmpty()

    val failedText = stringResource(R.string.generic_error)
    LaunchedEffect(failed) {
        if (failed) { snackbar.showSnackbar(failedText); vm.failed.value = false }
    }

    val gone by vm.gone.collectAsStateWithLifecycle()
    LaunchedEffect(gone) { if (gone) onBack() }
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                title = {
                    Text(
                        if (draft) pluralStringResource(R.plurals.review_pages, list.size, list.size)
                        else doc?.title.orEmpty(),
                        maxLines = 1,
                    )
                },
                actions = {
                    if (draft) {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options)) }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.discard_scan_title)) }, onClick = { menu = false; discarding = true })
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                Column(Modifier.navigationBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onAddPages, enabled = !busy) {
                            Icon(Icons.Outlined.AddAPhoto, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.add_page))
                        }
                        TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy) {
                            Icon(Icons.Outlined.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.import_images))
                        }
                        Spacer(Modifier.weight(1f))
                        AppButton(
                            stringResource(if (draft) R.string.save else R.string.done),
                            { if (draft) naming = true else onBack() },
                            enabled = !busy && list.isNotEmpty(),
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (pages != null && list.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_pages), style = MaterialTheme.typography.bodyLarge)
                }
            }
            LazyVerticalGrid(
                GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(list, key = { _, p -> p.id }) { i, page ->
                    PageCell(page, i, list.size, vm, onEdit)
                }
            }
        }
    }

    val scanPrefix = stringResource(R.string.default_title_prefix)
    if (naming) {
        NameDialog(stringResource(R.string.save_document), remember { suggestedTitle(scanPrefix) }, { vm.save(it, onSaved) }, { naming = false }, suggestion)
    }
    if (discarding) {
        ConfirmDialog(
            stringResource(R.string.discard_scan_title), stringResource(R.string.discard_scan_body),
            stringResource(R.string.discard), { vm.discard(onBack) }, { discarding = false },
        )
    }
}

@Composable
private fun PageCell(page: PageEntity, index: Int, count: Int, vm: SessionViewModel, onEdit: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val label = stringResource(R.string.page_n, index + 1)
    Column(Modifier.popIn(index)) {
        PageImage(
            page, 400, label,
            Modifier.fillMaxWidth().aspectRatio(0.77f).clip(ThumbShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), ThumbShape)
                .bounceClick(onClickLabel = stringResource(R.string.edit)) { onEdit(page.id) },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 4.dp))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options_for, label)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = { menu = false; onEdit(page.id) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.rotate_right)) }, onClick = { menu = false; vm.rotate(page, 90) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.rotate_left)) }, onClick = { menu = false; vm.rotate(page, -90) })
                    if (index > 0) DropdownMenuItem(text = { Text(stringResource(R.string.move_earlier)) }, onClick = { menu = false; vm.move(index, index - 1) })
                    if (index < count - 1) DropdownMenuItem(text = { Text(stringResource(R.string.move_later)) }, onClick = { menu = false; vm.move(index, index + 1) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.duplicate)) }, onClick = { menu = false; vm.duplicate(page) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; deleting = true })
                }
            }
        }
    }
    if (deleting) {
        ConfirmDialog(
            stringResource(R.string.delete_page_title, index + 1), stringResource(R.string.delete_page_body),
            stringResource(R.string.delete), { vm.delete(page) }, { deleting = false },
        )
    }
}
