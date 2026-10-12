package com.classicscan.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.classicscan.ui.theme.LocalScanColors

@Composable
fun AppDialog(
    icon: ImageVector,
    title: String,
    onDismiss: () -> Unit,
    confirm: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismiss: String? = null,
    danger: Boolean = false,
    confirmEnabled: Boolean = true,
    body: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val brand = LocalScanColors.current
    val error = MaterialTheme.colorScheme.error
    val badge = if (danger) listOf(lerp(error, Color.White, 0.15f), error) else listOf(brand.heroStart, brand.heroEnd)
    val pop = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }

    val compact = com.classicscan.ui.LocalWindowLayout.current.shortHeight
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .shadow(10.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!compact) Box(
                Modifier.size(64.dp).shadow(12.dp, CircleShape, ambientColor = badge[1], spotColor = badge[1])
                    .clip(CircleShape).background(Brush.linearGradient(badge)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.onError else brand.onHero, modifier = Modifier.size(30.dp)) }
            if (!compact) Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            body?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            content?.let {
                Spacer(Modifier.height(16.dp))
                Column(Modifier.fillMaxWidth(), content = it)
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dismiss != null) AppButton(dismiss, onDismiss, Modifier.weight(1f), tonal = true)
                if (confirm != null && onConfirm != null) {
                    AppButton(confirm, onConfirm, Modifier.weight(1f), enabled = confirmEnabled, danger = danger)
                }
            }
        }
    }
}
