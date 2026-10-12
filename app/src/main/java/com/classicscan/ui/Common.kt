package com.classicscan.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import android.graphics.Bitmap
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.classicscan.ui.components.AppDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.classicscan.R
import com.classicscan.data.PageEntity
import com.classicscan.image.ImageProcessor
import org.koin.compose.koinInject

@Composable
fun PageImage(
    page: PageEntity,
    maxDim: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val processor = koinInject<ImageProcessor>()
    val state by produceState<Result<Bitmap?>?>(null, page.id, page.editVersion, maxDim) {
        value = runCatching { processor.render(page, maxDim) }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val bmp = state?.getOrNull()
        when {
            state == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            bmp == null -> Icon(
                Icons.Outlined.BrokenImage,
                contentDescription = stringResource(R.string.image_unavailable),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> Image(
                bmp.asImageBitmap(), contentDescription, contentScale = contentScale,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}

@Composable
fun formatDate(millis: Long): String {
    val ctx = LocalContext.current
    return DateUtils.formatDateTime(ctx, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH)
}

@Composable
fun formatSize(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

@Composable
fun ConfirmDialog(
    title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit,
    icon: ImageVector = Icons.Outlined.DeleteOutline, danger: Boolean = true,
) {
    AppDialog(
        icon, title, onDismiss, confirm = confirm, onConfirm = { onDismiss(); onConfirm() },
        dismiss = stringResource(R.string.cancel), danger = danger, body = text,
    )
}

@Composable
fun InfoDialog(icon: ImageVector, title: String, text: String, onDismiss: () -> Unit) {
    AppDialog(icon, title, onDismiss, confirm = stringResource(R.string.got_it), onConfirm = onDismiss, body = text)
}

@Composable
fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit, suggestion: String? = null) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }

    val autoFocus = !LocalWindowLayout.current.shortHeight
    LaunchedEffect(Unit) { if (autoFocus) focus.requestFocus() }
    val submit = { if (value.text.isNotBlank()) { onDismiss(); onConfirm(value.text.trim()) } }
    AppDialog(
        Icons.Outlined.DriveFileRenameOutline, title, onDismiss,
        confirm = stringResource(R.string.save), onConfirm = submit,
        dismiss = stringResource(R.string.cancel), confirmEnabled = value.text.isNotBlank(),
    ) {
        TextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            shape = RoundedCornerShape(18.dp),
            label = { Text(stringResource(R.string.document_name)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )

        if (suggestion != null && suggestion != value.text) {
            AssistChip(
                onClick = { value = TextFieldValue(suggestion, TextRange(suggestion.length)) },
                label = { Text(stringResource(R.string.use_suggestion, suggestion), maxLines = 1) },
                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

fun suggestedTitle(prefix: String, millis: Long = System.currentTimeMillis()): String =
    "$prefix " + java.text.SimpleDateFormat("d MMM yyyy HH.mm", java.util.Locale.getDefault()).format(millis)

val ThumbSize = 72.dp

class ZoomState {
    var zoom by mutableFloatStateOf(1f)
    var tx by mutableFloatStateOf(0f)
    var ty by mutableFloatStateOf(0f)
    var size = Size.Zero

    fun toContent(o: Offset) = Offset(size.width / 2 + (o.x - size.width / 2 - tx) / zoom, size.height / 2 + (o.y - size.height / 2 - ty) / zoom)

    fun toScreen(o: Offset) = Offset(size.width / 2 + tx + (o.x - size.width / 2) * zoom, size.height / 2 + ty + (o.y - size.height / 2) * zoom)

    fun transform(centroid: Offset, pan: Offset, zoomChange: Float) {
        val c = Offset(size.width / 2, size.height / 2)
        val anchor = (centroid - c - Offset(tx, ty)) / zoom
        zoom = (zoom * zoomChange).coerceIn(1f, 6f)
        val t = centroid - c - anchor * zoom + pan
        val mx = (zoom - 1) * size.width / 2; val my = (zoom - 1) * size.height / 2
        tx = t.x.coerceIn(-mx, mx); ty = t.y.coerceIn(-my, my)
    }

    fun reset() { zoom = 1f; tx = 0f; ty = 0f }
}

fun Modifier.zoomable(z: ZoomState) = onSizeChanged { z.size = Size(it.width.toFloat(), it.height.toFloat()) }
    .pointerInput(z) {
        detectTapGestures(onDoubleTap = { pos -> if (z.zoom > 1f) z.reset() else z.transform(pos, Offset.Zero, 2.5f) })
    }

fun Modifier.zoomed(z: ZoomState) = graphicsLayer { scaleX = z.zoom; scaleY = z.zoom; translationX = z.tx; translationY = z.ty }
