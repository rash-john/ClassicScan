package com.classicscan.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.classicscan.ui.theme.CardShape
import com.classicscan.ui.theme.LocalScanColors
import com.classicscan.ui.theme.onColor

@Composable
fun ScanCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: Int = 20, content: @Composable ColumnScope.() -> Unit) {
    val glow = if (LocalScanColors.current.isDark) 0.dp else 12.dp
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    Column(
        modifier.fillMaxWidth().shadow(glow, CardShape, ambientColor = tint, spotColor = tint)
            .clip(CardShape).background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier).padding(padding.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        action?.invoke()
    }
}

@Composable
fun ScreenHeader(title: String, eyebrow: String? = null, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            eyebrow?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        }
        actions()
    }
}

@Composable
fun IconTile(icon: ImageVector, color: Color, size: Int = 44) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.36f).dp)).background(Brush.linearGradient(listOf(lerp(color, Color.White, 0.18f), color))),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = color.onColor(), modifier = Modifier.size((size * 0.52f).dp)) }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val brand = LocalScanColors.current
    val bob = if (animationsEnabled()) rememberInfiniteTransition(label = "bob").animateFloat(
        -6f, 6f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "bob",
    ).value else 0f
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.offset(x = (-52).dp, y = (-44).dp + bob.dp).size(18.dp).clip(CircleShape).background(brand.confetti[0]))
            Box(Modifier.offset(x = 56.dp, y = 40.dp - bob.dp).size(12.dp).clip(CircleShape).background(brand.confetti[1]))
            Box(Modifier.offset(x = 50.dp, y = (-50).dp - bob.dp).size(8.dp).clip(CircleShape).background(brand.confetti[2]))
            Box(
                Modifier.offset(y = bob.dp).size(108.dp).shadow(16.dp, CircleShape, spotColor = brand.heroStart, ambientColor = brand.heroStart)
                    .clip(CircleShape).background(Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd))),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = brand.onHero, modifier = Modifier.size(48.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}
