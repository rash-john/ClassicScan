package com.classicscan.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.classicscan.R
import com.classicscan.data.SettingsRepository
import com.classicscan.ui.components.AppButton
import com.classicscan.ui.components.IconTile
import com.classicscan.ui.components.animationsEnabled
import com.classicscan.ui.components.popIn
import com.classicscan.ui.theme.LocalScanColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppMark(size: Int = 44) {
    val brand = LocalScanColors.current
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.32f).dp)).background(Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd))),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.DocumentScanner, null, tint = brand.onHero, modifier = Modifier.size((size * 0.55f).dp)) }
}

@Composable
fun ScanIllustration(modifier: Modifier) {
    val brand = LocalScanColors.current
    val paper = MaterialTheme.colorScheme.surfaceContainerLowest
    val lines = MaterialTheme.colorScheme.outlineVariant
    val t = if (animationsEnabled()) rememberInfiniteTransition(label = "scan").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "scan",
    ).value else 0.5f
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val pw = h * 0.62f; val ph = h * 0.82f
        val left = (w - pw) / 2; val top = (h - ph) / 2
        drawCircle(brand.confetti[0], h * 0.05f, Offset(left - h * 0.12f, top + h * 0.1f))
        drawCircle(brand.confetti[1], h * 0.035f, Offset(left + pw + h * 0.14f, top + ph * 0.8f))
        drawCircle(brand.confetti[2], h * 0.025f, Offset(left + pw + h * 0.1f, top + h * 0.02f))
        rotate(-4f, Offset(w / 2, h / 2)) {
            drawRoundRect(paper, Offset(left, top), Size(pw, ph), CornerRadius(24f))
            for (i in 0 until 6) {
                val lw = if (i == 0) pw * 0.5f else pw * (0.72f - (i % 3) * 0.1f)
                drawRoundRect(lines, Offset(left + pw * 0.12f, top + ph * (0.16f + i * 0.12f)), Size(lw, ph * 0.035f), CornerRadius(8f))
            }
        }
        val scanY = top + ph * (0.08f + 0.84f * t)
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, brand.heroStart.copy(alpha = 0.35f)), startY = scanY - ph * 0.18f, endY = scanY),
            Offset(left - 12f, scanY - ph * 0.18f), Size(pw + 24f, ph * 0.18f),
        )
        drawLine(Brush.horizontalGradient(listOf(brand.heroStart, brand.heroEnd)), Offset(left - 12f, scanY), Offset(left + pw + 12f, scanY), strokeWidth = 6f)
        val c = h * 0.09f; val pad = 22f
        val stroke = Stroke(10f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val brush = Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd))
        listOf(
            Offset(left - pad, top - pad) to Pair(1, 1), Offset(left + pw + pad, top - pad) to Pair(-1, 1),
            Offset(left - pad, top + ph + pad) to Pair(1, -1), Offset(left + pw + pad, top + ph + pad) to Pair(-1, -1),
        ).forEach { (o, d) ->
            drawLine(brush, o, Offset(o.x + c * d.first, o.y), stroke.width, cap = stroke.cap)
            drawLine(brush, o, Offset(o.x, o.y + c * d.second), stroke.width, cap = stroke.cap)
        }
    }
}

private data class Intro(val title: Int, val body: Int, val chips: List<Int>, val art: @Composable (Modifier) -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val settings = koinInject<SettingsRepository>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val pages = listOf(
        Intro(R.string.intro1_title, R.string.intro1_body, listOf(R.string.chip_edges, R.string.chip_auto_capture, R.string.chip_multi_page)) { ScanIllustration(it) },
        Intro(R.string.intro2_title, R.string.intro2_body, listOf(R.string.mode_id_card, R.string.mode_book, R.string.mode_qr, R.string.mode_whiteboard)) { ModesIllustration(it) },
        Intro(R.string.intro3_title, R.string.intro3_body, listOf(R.string.chip_search_text, R.string.chip_copy_text, R.string.searchable_pdf)) { TextIllustration(it) },
        Intro(R.string.intro4_title, R.string.intro4_body, listOf(R.string.chip_filters, R.string.adjust, R.string.sign_draw)) { SignIllustration(it) },
        Intro(R.string.intro5_title, R.string.intro5_body, listOf(R.string.chip_offline, R.string.app_lock, R.string.chip_no_account)) { SecureIllustration(it) },
    )
    val pager = rememberPagerState { pages.size }
    val last = pager.currentPage == pages.lastIndex
    val finish: () -> Unit = { scope.launch { settings.setOnboarded(); onDone() } }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp).height(52.dp), verticalAlignment = Alignment.CenterVertically) {
            AppMark(36)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (!last) TextButton(onClick = finish) { Text(stringResource(R.string.skip)) }
        }
        val window = LocalWindowLayout.current
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            val page = pages[i]

            if (window.landscape) Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                page.art(Modifier.weight(1f).fillMaxHeight().padding(16.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { IntroText(page) }
            } else Centered(640.dp) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                page.art(Modifier.fillMaxWidth().height(if (window.medium) 360.dp else 280.dp).padding(vertical = 16.dp))
                IntroText(page)
            } }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val brand = LocalScanColors.current
                repeat(pages.size) { i ->
                    val on = i == pager.currentPage
                    val width by animateDpAsState(if (on) 28.dp else 8.dp, spring(Spring.DampingRatioMediumBouncy), label = "dot")
                    Box(
                        Modifier.size(width, 8.dp).clip(CircleShape)
                            .background(if (on) Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)) else Brush.linearGradient(listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant))),
                    )
                }
            }
            AppButton(
                stringResource(if (last) R.string.get_started else R.string.next),
                { if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                icon = if (last) null else Icons.AutoMirrored.Filled.ArrowForward,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntroText(page: Intro) {
    Text(stringResource(page.title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(12.dp))
    Text(stringResource(page.body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(18.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        page.chips.forEachIndexed { c, chip ->
            Text(
                stringResource(chip), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.popIn(c).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ticker(periodMs: Int, reverse: Boolean = false): Float =
    if (animationsEnabled()) rememberInfiniteTransition(label = "t").animateFloat(
        0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), if (reverse) RepeatMode.Reverse else RepeatMode.Restart), label = "t",
    ).value else 0.6f

@Composable
private fun ModesIllustration(modifier: Modifier) {
    val brand = LocalScanColors.current
    val t = ticker(4000)
    val active = (t * 4).toInt().coerceIn(0, 3)
    val modes = listOf(
        Triple(Icons.Filled.Badge, Color(0xFFE84393), R.string.mode_id_card),
        Triple(Icons.AutoMirrored.Filled.MenuBook, Color(0xFFFF9F43), R.string.mode_book),
        Triple(Icons.Filled.QrCode2, Color(0xFF6C5CE7), R.string.mode_qr),
        Triple(Icons.Filled.CoPresent, Color(0xFF00B894), R.string.mode_whiteboard),
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp)) {
            drawCircle(Brush.radialGradient(listOf(brand.heroStart.copy(alpha = 0.22f), Color.Transparent)), size.minDimension / 2)
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            for (row in 0..1) Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (col in 0..1) {
                    val i = row * 2 + col
                    val (icon, color, label) = modes[i]
                    val scale by animateFloatAsState(if (i == active) 1.12f else 0.92f, spring(Spring.DampingRatioMediumBouncy), label = "m")
                    Column(
                        Modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (i == active) 1f else 0.7f }
                            .size(104.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(if (i == active) 2.dp else 0.dp, color, RoundedCornerShape(28.dp)).padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        IconTile(icon, color, 46)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextIllustration(modifier: Modifier) {
    val brand = LocalScanColors.current
    val paper = MaterialTheme.colorScheme.surfaceContainerLowest
    val ink = MaterialTheme.colorScheme.outline
    val t = ticker(3600)
    Canvas(modifier) {
        val pw = size.height * 0.66f; val ph = size.height * 0.86f
        val left = (size.width - pw) / 2; val top = (size.height - ph) / 2
        drawRoundRect(paper, Offset(left, top), Size(pw, ph), CornerRadius(28f))
        val lines = 6
        val progress = t * lines
        for (i in 0 until lines) {
            val y = top + ph * (0.14f + i * 0.13f)
            val lw = pw * (if (i == 0) 0.5f else 0.74f - (i % 3) * 0.1f)
            drawRoundRect(ink.copy(alpha = 0.45f), Offset(left + pw * 0.12f, y), Size(lw, ph * 0.04f), CornerRadius(8f))
            val f = (progress - i).coerceIn(0f, 1f)
            if (f > 0f) drawRoundRect(
                Brush.horizontalGradient(listOf(brand.heroStart.copy(alpha = 0.45f), brand.heroEnd.copy(alpha = 0.45f))),
                Offset(left + pw * 0.1f, y - ph * 0.018f), Size((lw + pw * 0.04f) * f, ph * 0.076f), CornerRadius(12f),
            )
        }
        val li = progress.toInt().coerceIn(0, lines - 1)
        val lf = (progress - li).coerceIn(0f, 1f)
        val lensCenter = Offset(left + pw * (0.14f + 0.6f * lf), top + ph * (0.16f + li * 0.13f))
        val r = size.height * 0.11f
        drawCircle(Color.White.copy(alpha = 0.35f), r, lensCenter)
        drawCircle(Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)), r, lensCenter, style = Stroke(8f))
        drawLine(brand.heroEnd, lensCenter + Offset(r * 0.7f, r * 0.7f), lensCenter + Offset(r * 1.5f, r * 1.5f), 14f, cap = StrokeCap.Round)
    }
}

@Composable
private fun SignIllustration(modifier: Modifier) {
    val brand = LocalScanColors.current
    val paper = MaterialTheme.colorScheme.surfaceContainerLowest
    val ink = MaterialTheme.colorScheme.outline
    val t = ticker(3000)
    Canvas(modifier) {
        val pw = size.height * 0.66f; val ph = size.height * 0.86f
        val left = (size.width - pw) / 2; val top = (size.height - ph) / 2
        drawRoundRect(paper, Offset(left, top), Size(pw, ph), CornerRadius(28f))
        for (i in 0 until 4) drawRoundRect(ink.copy(alpha = 0.4f), Offset(left + pw * 0.12f, top + ph * (0.14f + i * 0.11f)), Size(pw * (0.72f - (i % 2) * 0.16f), ph * 0.035f), CornerRadius(8f))

        drawRoundRect(Brush.verticalGradient(listOf(brand.heroStart.copy(alpha = 0.18f), Color.Transparent)), Offset(left, top), Size(pw, ph * 0.55f * t), CornerRadius(28f))
        val sig = Path().apply {
            val x0 = left + pw * 0.14f; val y0 = top + ph * 0.78f; val u = pw / 10
            moveTo(x0, y0)
            cubicTo(x0 + u, y0 - u * 2.2f, x0 + u * 1.6f, y0 + u * 0.8f, x0 + u * 2.4f, y0 - u * 0.4f)
            cubicTo(x0 + u * 3f, y0 - u * 1.6f, x0 + u * 3.4f, y0 + u, x0 + u * 4.2f, y0 - u * 0.2f)
            cubicTo(x0 + u * 5f, y0 - u * 1.4f, x0 + u * 5.6f, y0 + u * 0.6f, x0 + u * 6.4f, y0 - u * 0.6f)
            quadraticTo(x0 + u * 7f, y0 - u * 1.2f, x0 + u * 7.4f, y0 + u * 0.2f)
        }
        val measure = PathMeasure().apply { setPath(sig, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * (t * 1.25f).coerceAtMost(1f), part, true)
        drawPath(part, Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)), style = Stroke(9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(ink.copy(alpha = 0.5f), Offset(left + pw * 0.12f, top + ph * 0.84f), Offset(left + pw * 0.86f, top + ph * 0.84f), 3f)
    }
}

@Composable
private fun SecureIllustration(modifier: Modifier) {
    val brand = LocalScanColors.current
    val t = ticker(2400)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            for (k in 0 until 3) {
                val f = (t + k / 3f) % 1f
                drawCircle(brand.heroStart.copy(alpha = 0.35f * (1 - f)), size.height * (0.2f + 0.3f * f), c, style = Stroke(4f))
            }
            val s = size.height * 0.3f
            val shield = Path().apply {
                moveTo(c.x, c.y - s)
                cubicTo(c.x + s * 0.5f, c.y - s * 0.8f, c.x + s * 0.8f, c.y - s * 0.8f, c.x + s * 0.8f, c.y - s * 0.7f)
                cubicTo(c.x + s * 0.8f, c.y + s * 0.2f, c.x + s * 0.5f, c.y + s * 0.75f, c.x, c.y + s)
                cubicTo(c.x - s * 0.5f, c.y + s * 0.75f, c.x - s * 0.8f, c.y + s * 0.2f, c.x - s * 0.8f, c.y - s * 0.7f)
                cubicTo(c.x - s * 0.8f, c.y - s * 0.8f, c.x - s * 0.5f, c.y - s * 0.8f, c.x, c.y - s)
                close()
            }
            drawPath(shield, Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd), Offset(c.x - s, c.y - s), Offset(c.x + s, c.y + s)))
        }
        Icon(Icons.Filled.Lock, null, tint = brand.onHero, modifier = Modifier.size(56.dp))
    }
}

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        com.classicscan.ui.components.EmptyState(
            Icons.Filled.Lock, stringResource(R.string.locked_title), stringResource(R.string.locked_body),
        ) { AppButton(stringResource(R.string.unlock), onUnlock) }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
