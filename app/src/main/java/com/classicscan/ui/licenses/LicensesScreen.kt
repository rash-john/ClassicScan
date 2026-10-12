package com.classicscan.ui.licenses

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classicscan.R
import com.classicscan.ui.Centered
import com.classicscan.ui.components.CircleButton
import com.classicscan.ui.components.IconTile
import com.classicscan.ui.components.ScanCard
import com.classicscan.ui.components.SectionTitle
import com.classicscan.ui.components.popIn

private const val APP_NOTICE = """Copyright 2026 ClassicScan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""

private data class Library(val name: String, val purpose: Int, val copyright: String, val license: String, val icon: ImageVector, val color: Color)

private val LIBRARIES = listOf(
    Library("AndroidX", R.string.lib_androidx, "Copyright The Android Open Source Project", "Apache License 2.0", Icons.Filled.Inventory2, Color(0xFF16A34A)),
    Library("Material Components", R.string.lib_material, "Copyright The Android Open Source Project", "Apache License 2.0", Icons.Filled.Visibility, Color(0xFF2F6BFF)),
    Library("Koin", R.string.lib_koin, "Copyright the Koin project", "Apache License 2.0", Icons.Filled.Hub, Color(0xFFFF9F43)),
    Library("Google ML Kit", R.string.lib_mlkit, "Copyright Google LLC", "ML Kit Terms of Service", Icons.Filled.DocumentScanner, Color(0xFF6C5CE7)),
    Library("Sniglet", R.string.lib_sniglet, "Copyright The Sniglet Project Authors", "SIL Open Font License 1.1", Icons.Filled.FontDownload, Color(0xFFE84393)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val fullText = remember { reflow(context.resources.openRawResource(R.raw.apache_license).bufferedReader().use { it.readText() }) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.licenses), maxLines = 1) },
                navigationIcon = { CircleButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack, Modifier.padding(start = 8.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Centered(modifier = Modifier.padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                item {
                    ScanCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).popIn(0)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.classicscan.ui.AppMark(44)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                                Text("Apache License 2.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        CodeBlock(remember { reflow(APP_NOTICE) })
                        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.padding(top = 8.dp)) {
                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(if (expanded) R.string.hide_full_license else R.string.read_full_license))
                        }
                        AnimatedVisibility(expanded) { CodeBlock(fullText) }
                    }
                }
                item {
                    SectionTitle(stringResource(R.string.third_party), Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp))
                }
                items(LIBRARIES, key = { it.name }) { lib ->
                    ScanCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).popIn(1), padding = 16) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(lib.icon, lib.color, 44)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(lib.name, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(lib.purpose), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(lib.copyright, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            lib.license, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

fun reflow(text: String): String = text.split(Regex("\\n\\s*\\n"))
    .map { para -> para.lines().joinToString(" ") { it.trim() }.trim() }
    .filter { it.isNotEmpty() }
    .joinToString("\n\n")

@Composable
private fun CodeBlock(text: String) {
    SelectionContainer {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 17.sp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp),
        )
    }
}

