package com.classicscan.codes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classicscan.R
import com.classicscan.data.CodeEntity
import com.classicscan.data.DocumentRepository
import com.classicscan.ui.ConfirmDialog
import com.classicscan.ui.components.AppButton
import com.classicscan.ui.components.CircleButton
import com.classicscan.ui.components.EmptyState
import com.classicscan.ui.components.IconTile
import com.classicscan.ui.components.ScanCard
import com.classicscan.ui.components.bounceClick
import com.classicscan.ui.components.popIn
import com.classicscan.ui.formatDate
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodesScreen(onBack: () -> Unit, onScanCode: () -> Unit) {
    val repo = koinInject<DocumentRepository>()
    val codes by remember { repo.observeCodes() }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf<CodeEntity?>(null) }
    var clearing by remember { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.scanned_codes), maxLines = 1) },
                navigationIcon = { CircleButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack, Modifier.padding(start = 8.dp)) },
                actions = {
                    if (!codes.isNullOrEmpty()) CircleButton(Icons.Filled.DeleteSweep, stringResource(R.string.clear_history), { clearing = true }, Modifier.padding(end = 8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        val list = codes
        when {
            list == null -> Unit
            list.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Filled.QrCodeScanner, stringResource(R.string.no_codes), stringResource(R.string.no_codes_body)) {
                    AppButton(stringResource(R.string.scan_a_code), onScanCode, icon = Icons.Filled.QrCodeScanner)
                }
            }
            else -> com.classicscan.ui.Centered(modifier = Modifier.padding(padding)) { LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { code ->
                    val content = parseCode(code.raw, code.format)
                    val (icon, color, label) = codeLook(content)
                    ScanCard(Modifier.popIn(0).animateItem(), onClick = { open = code }, padding = 14) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(icon, color, 44)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(codeSummary(content, code.raw), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    stringResource(label) + " · " + formatLabel(code.format) + " · " + formatDate(code.createdAt),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                                )
                            }
                            val del = stringResource(R.string.delete)
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).bounceClick { scope.launch { repo.deleteCode(code.id) } }.semantics { contentDescription = del },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            } }
        }
    }
    open?.let { CodeSheet(it.raw, it.format) { open = null } }
    if (clearing) {
        ConfirmDialog(
            stringResource(R.string.clear_history), stringResource(R.string.clear_history_body), stringResource(R.string.clear),
            { scope.launch { repo.clearCodes() } }, { clearing = false }, icon = Icons.Filled.DeleteSweep,
        )
    }
}

