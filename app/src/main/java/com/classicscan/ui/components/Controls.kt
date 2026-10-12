package com.classicscan.ui.components

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classicscan.ui.theme.LocalScanColors
import com.classicscan.ui.theme.onColor

@Composable
fun animationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
}

fun Modifier.bounceClick(enabled: Boolean = true, role: Role = Role.Button, onClickLabel: String? = null, onClick: () -> Unit) = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.92f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(source, indication = null, enabled = enabled, onClickLabel = onClickLabel, role = role, onClick = onClick)
}

fun Modifier.popIn(index: Int = 0) = composed {
    var played by rememberSaveable { mutableStateOf(false) }
    val p = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (played) return@LaunchedEffect
        played = true
        kotlinx.coroutines.delay(40L * index.coerceAtMost(8))
        p.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
    }
    graphicsLayer { alpha = p.value.coerceIn(0f, 1f); translationY = (1f - p.value) * 36.dp.toPx() }
}

@Composable
fun AppButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    icon: ImageVector? = null, tonal: Boolean = false, danger: Boolean = false,
) {
    val brand = LocalScanColors.current
    val shape = RoundedCornerShape(50)
    val error = MaterialTheme.colorScheme.error
    val brush = when {
        tonal -> Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondaryContainer))
        danger -> Brush.linearGradient(listOf(error, androidx.compose.ui.graphics.lerp(error, Color(0xFFFF8A5B), 0.45f)))
        else -> Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd))
    }
    val ink = when {
        tonal -> MaterialTheme.colorScheme.onSecondaryContainer
        danger -> MaterialTheme.colorScheme.onError
        else -> brand.onHero
    }
    val glowColor = if (danger) error else brand.heroStart
    val glow = if (tonal || !enabled) 0.dp else 10.dp
    Box(
        modifier.heightIn(min = 56.dp).graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .shadow(glow, shape, ambientColor = glowColor, spotColor = glowColor)
            .clip(shape).background(brush).bounceClick(enabled, onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) Icon(icon, null, tint = ink, modifier = Modifier.size(22.dp))
            Text(text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = ink, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CircleButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color? = null, container: Color? = null) {
    Box(
        modifier.size(48.dp).clip(CircleShape).background(container ?: MaterialTheme.colorScheme.surfaceContainerHigh)
            .bounceClick(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
fun AppChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1f else 0.97f, spring(Spring.DampingRatioMediumBouncy), label = "chip")
    FilterChip(
        selected, onClick, label, Modifier.graphicsLayer { scaleX = scale; scaleY = scale }, shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = null,
    )
}

@Composable
fun AppToggle(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.primary
    BoxWithConstraints(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(4.dp),
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "toggle")
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().shadow(6.dp, RoundedCornerShape(50), spotColor = fill).clip(RoundedCornerShape(50)).background(fill))
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, text ->
                val ink by animateColorAsState(if (i == selected) fill.onColor() else MaterialTheme.colorScheme.onSurfaceVariant, label = "toggle-ink")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50))
                        .clickable(role = Role.RadioButton) { onSelect(i) }.semantics { this.selected = i == selected },
                    contentAlignment = Alignment.Center,
                ) { Text(text, style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 1) }
            }
        }
    }
}

@Composable
fun AppMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) = DropdownMenu(
    expanded, onDismissRequest, shape = RoundedCornerShape(20.dp), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, content = content,
)
