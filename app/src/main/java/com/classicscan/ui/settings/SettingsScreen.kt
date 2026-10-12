package com.classicscan.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.classicscan.ui.InfoDialog
import com.classicscan.ui.Centered
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classicscan.BuildConfig
import com.classicscan.R
import com.classicscan.data.Settings
import com.classicscan.data.ThemeMode
import com.classicscan.export.PageSize
import com.classicscan.export.Quality
import com.classicscan.ui.components.AppChip
import com.classicscan.ui.components.AppToggle
import com.classicscan.ui.components.CircleButton
import com.classicscan.ui.components.IconTile
import com.classicscan.ui.components.ScanCard
import com.classicscan.ui.components.ScreenHeader
import com.classicscan.ui.components.SectionTitle
import com.classicscan.ui.components.bounceClick
import com.classicscan.ui.components.popIn
import com.classicscan.ui.formatSize
import com.classicscan.ui.theme.ACCENTS
import com.classicscan.ui.theme.LocalScanColors
import com.classicscan.ui.theme.dynamicColorSupported
import com.classicscan.ui.theme.onColor
import com.classicscan.ui.viewer.pageSizeLabel
import com.classicscan.ui.viewer.qualityLabel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: Settings, onBack: () -> Unit, onLicenses: () -> Unit, vm: SettingsViewModel = koinViewModel()) {
    val storage by vm.storage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val brand = LocalScanColors.current
    var info by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val noLockText = stringResource(R.string.app_lock_unavailable)
    val cacheCleared = stringResource(R.string.cache_cleared)

    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings), maxLines = 1) },
                navigationIcon = { CircleButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack, Modifier.padding(start = 8.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scroll,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Centered(modifier = Modifier.padding(padding)) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Group(R.string.appearance, 0) {
                ThemePicker(settings.theme, vm::setTheme)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Filled.Palette, brand.heroStart, 40)
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.accent), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                AccentPicker(settings.accent, enabled = !(settings.dynamicColor && dynamicColorSupported), onPick = vm::setAccent)
                if (dynamicColorSupported) {
                    SwitchRow(Icons.Filled.Wallpaper, brand.heroEnd, R.string.dynamic_color, R.string.dynamic_color_summary, settings.dynamicColor, vm::setDynamic)
                }
                SwitchRow(Icons.Filled.DarkMode, Color(0xFF3B3B98), R.string.amoled, R.string.amoled_summary, settings.amoled, vm::setAmoled)
            }

            Group(R.string.language, 1) {
                val english = AppCompatDelegate.getApplicationLocales().toLanguageTags().startsWith("en")
                ChoiceRow(Icons.Filled.PhoneAndroid, brand.heroStart, R.string.language_system, R.string.language_system_summary, !english) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
                ChoiceRow(Icons.Filled.Translate, brand.heroEnd, R.string.language_english, R.string.language_english_summary, english) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                }
            }

            Group(R.string.scanning, 2) {
                SwitchRow(Icons.Filled.AutoMode, Color(0xFF00B894), R.string.auto_capture_setting, R.string.auto_capture_summary, settings.autoCapture, vm::setAutoCapture)
                SwitchRow(Icons.Filled.ManageSearch, Color(0xFF6C5CE7), R.string.searchable_pdf, R.string.searchable_pdf_body, settings.searchablePdf, vm::setSearchable)
                Label(R.string.default_page_size)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PageSize.entries.forEach { s -> AppChip(settings.pageSize == s, { vm.setPageSize(s) }) { Text(stringResource(pageSizeShort(s))) } }
                }
                Label(R.string.default_quality)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Quality.entries.forEach { q -> AppChip(settings.quality == q, { vm.setQuality(q) }) { Text(stringResource(qualityLabel(q))) } }
                }
            }

            Group(R.string.privacy_security, 3) {
                SwitchRow(Icons.Filled.Fingerprint, Color(0xFFE84393), R.string.app_lock, R.string.app_lock_summary, settings.appLock) { on ->
                    val ok = BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                    if (!on || ok) vm.setAppLock(on) else scope.launch { snackbar.showSnackbar(noLockText) }
                }
                LinkRow(Icons.Filled.Folder, Color(0xFF0984E3), R.string.where_stored, stringResource(R.string.where_stored_summary)) { info = R.string.where_stored to R.string.where_stored_body }
                LinkRow(Icons.Filled.PrivacyTip, Color(0xFF16A085), R.string.privacy, stringResource(R.string.privacy_summary)) { info = R.string.privacy to R.string.privacy_body }
            }

            Group(R.string.storage, 4) {
                LinkRow(Icons.Filled.Storage, Color(0xFFFF9F43), R.string.documents_storage, storage?.let { formatSize(it.first) } ?: "…", null)
                LinkRow(Icons.Filled.CleaningServices, Color(0xFFFF6B6B), R.string.clear_cache, stringResource(R.string.clear_cache_summary, storage?.let { formatSize(it.second) } ?: "…")) {
                    vm.clearCache(); scope.launch { snackbar.showSnackbar(cacheCleared) }
                }
            }

            Group(R.string.about, 5) {
                LinkRow(Icons.Filled.Gavel, Color(0xFF8D8BA0), R.string.licenses, stringResource(R.string.licenses_summary), onLicenses)
                LinkRow(Icons.Filled.Feedback, Color(0xFF9B5DE5), R.string.send_feedback, stringResource(R.string.send_feedback_summary)) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, "ClassicScan ${BuildConfig.VERSION_NAME} feedback")
                    runCatching { context.startActivity(intent) }
                }
                LinkRow(Icons.Filled.Info, Color(0xFF607D8B), R.string.version, BuildConfig.VERSION_NAME, null)
            }
        } }
    }

    info?.let { (title, body) ->
        InfoDialog(Icons.Filled.Info, stringResource(title), stringResource(body), { info = null })
    }
}

@Composable
private fun ThemePicker(selected: ThemeMode, onPick: (ThemeMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemeMode.entries.forEach { mode ->
            val on = mode == selected
            val label = stringResource(
                when (mode) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark }
            )
            val lift by animateFloatAsState(if (on) 1f else 0f, spring(Spring.DampingRatioMediumBouncy), label = "lift")
            Column(
                Modifier.weight(1f).graphicsLayer { translationY = -6.dp.toPx() * lift }
                    .clip(RoundedCornerShape(22.dp))
                    .bounceClick(role = Role.RadioButton) { onPick(mode) }
                    .semantics { this.selected = on; contentDescription = label },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    ThemePreview(mode, Modifier.fillMaxWidth().height(116.dp)
                        .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp)))
                    if (on) Box(
                        Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
                }
                Spacer(Modifier.height(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ThemePreview(mode: ThemeMode, modifier: Modifier) {
    val brand = LocalScanColors.current
    androidx.compose.foundation.Canvas(modifier) {
        fun drawMock(bg: Color, card: Color, line: Color) {
            drawRect(bg)
            val w = size.width; val h = size.height
            drawRoundRect(line, Offset(w * 0.12f, h * 0.1f), Size(w * 0.45f, h * 0.07f), CornerRadius(8f))
            drawRoundRect(Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)), Offset(w * 0.12f, h * 0.24f), Size(w * 0.76f, h * 0.26f), CornerRadius(18f))
            for (i in 0 until 2) {
                val y = h * (0.58f + i * 0.18f)
                drawRoundRect(card, Offset(w * 0.12f, y), Size(w * 0.18f, h * 0.13f), CornerRadius(8f))
                drawRoundRect(line, Offset(w * 0.36f, y + h * 0.03f), Size(w * 0.5f, h * 0.05f), CornerRadius(8f))
            }
        }
        val light = Triple(Color(0xFFF6F5FB), Color(0xFFE3E1EE), Color(0xFFCFCCDD))
        val dark = Triple(Color(0xFF16151B), Color(0xFF2A2932), Color(0xFF3B3A45))
        when (mode) {
            ThemeMode.LIGHT -> drawMock(light.first, light.second, light.third)
            ThemeMode.DARK -> drawMock(dark.first, dark.second, dark.third)
            ThemeMode.SYSTEM -> {
                drawMock(light.first, light.second, light.third)
                clipRect(left = size.width / 2) { drawMock(dark.first, dark.second, dark.third) }
            }
        }
    }
}

@Composable
private fun ChoiceRow(icon: ImageVector, color: Color, title: Int, summary: Int, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "choice")
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(20.dp)).background(bg)
            .bounceClick(role = Role.RadioButton, onClick = onClick).semantics { this.selected = selected }
            .heightIn(min = 64.dp).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, color, 40)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

private fun pageSizeShort(s: PageSize) = when (s) {
    PageSize.AUTO -> R.string.size_auto_short
    else -> pageSizeLabel(s)
}

@Composable
private fun Group(title: Int, index: Int, content: @Composable ColumnScope.() -> Unit) {
    ScanCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).popIn(index)) {
        SectionTitle(stringResource(title))
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun Label(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}

@Composable
private fun SwitchRow(icon: ImageVector, color: Color, title: Int, summary: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 64.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, color, 40)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked, null)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, color: Color, title: Int, summary: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .heightIn(min = 64.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, color, 40)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccentPicker(selected: Int, enabled: Boolean, onPick: (Int) -> Unit) {
    val names = listOf(R.string.accent_ocean, R.string.accent_grape, R.string.accent_mint, R.string.accent_coral, R.string.accent_sunset, R.string.accent_berry)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ACCENTS.forEachIndexed { i, a ->
            val on = i == selected && enabled
            val name = stringResource(names[i])
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .then(if (on) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .padding(if (on) 4.dp else 0.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(a.primary, a.partner)), alpha = if (enabled) 1f else 0.35f)
                    .bounceClick(enabled = enabled, role = Role.RadioButton) { onPick(i) }
                    .semantics { contentDescription = name; this.selected = on },
                contentAlignment = Alignment.Center,
            ) { if (on) Icon(Icons.Filled.Check, null, tint = a.primary.onColor(), modifier = Modifier.size(20.dp)) }
        }
    }
}

