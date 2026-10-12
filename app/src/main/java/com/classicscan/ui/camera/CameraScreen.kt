package com.classicscan.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CoPresent
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classicscan.R
import com.classicscan.codes.Barcodes
import com.classicscan.codes.CodeSheet
import com.classicscan.data.SettingsRepository
import com.classicscan.image.FULL_QUAD
import com.classicscan.image.Quad
import com.classicscan.image.alignQuad
import com.classicscan.image.detectQuad
import com.classicscan.image.lerpQuad
import com.classicscan.image.maxCornerDelta
import com.classicscan.image.orderQuad
import com.classicscan.image.quadPoint
import com.classicscan.image.rotated
import com.classicscan.ui.LocalWindowLayout
import com.classicscan.ui.PageImage
import com.classicscan.ui.components.animationsEnabled
import com.classicscan.ui.components.bounceClick
import com.classicscan.ui.theme.LocalScanColors
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    autoCapture: Boolean,
    showGrid: Boolean,
    onCodes: () -> Unit,
    onBack: () -> Unit,
    onReview: () -> Unit,
    vm: CameraViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var hasPermission by remember { mutableStateOf(granted()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it; asked = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasPermission = granted() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { vm.import(it) }
    val importImages = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }

    Box(Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        when {
            !hasCamera -> Notice(
                Icons.Outlined.NoPhotography, stringResource(R.string.no_camera_title), stringResource(R.string.no_camera_body),
                stringResource(R.string.import_images), importImages, onBack,
            )

            !hasPermission -> {
                val permanentlyDenied = asked && activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false
                Notice(
                    Icons.Outlined.PhotoCamera,
                    stringResource(R.string.camera_permission_title),
                    stringResource(if (permanentlyDenied) R.string.camera_permission_denied else R.string.camera_permission_body),
                    stringResource(if (permanentlyDenied) R.string.open_settings else R.string.allow_camera),
                    {
                        if (permanentlyDenied) {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                        } else {
                            permission.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onBack,
                    secondary = stringResource(R.string.import_images) to importImages,
                )
            }

            else -> CameraContent(vm, autoCapture, showGrid, onBack, onReview, onCodes, importImages)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun CameraContent(
    vm: CameraViewModel, autoCapture: Boolean, showGrid: Boolean,
    onBack: () -> Unit, onReview: () -> Unit, onCodes: () -> Unit, importImages: () -> Unit,
) {
    val settingsRepo = koinInject<SettingsRepository>()
    val brand = LocalScanColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pages by vm.pages.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    val idSides by vm.idSides.collectAsStateWithLifecycle()
    val code by vm.code.collectAsStateWithLifecycle()
    val currentMode by rememberUpdatedState(mode)
    val codeShowing by rememberUpdatedState(code != null)

    var shownQuad by remember { mutableStateOf<Quad?>(null) }
    var codeBox by remember { mutableStateOf<FloatArray?>(null) }
    var frameAspect by remember { mutableStateOf(3f / 4f) }
    var cameraError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var hasFlash by remember { mutableStateOf(false) }
    var flashMode by rememberSaveable { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var shutterBusy by remember { mutableStateOf(false) }

    var stableSince by remember { mutableLongStateOf(0L) }
    var misses by remember { mutableIntStateOf(0) }
    var armed by remember { mutableStateOf(true) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    val flashAlpha = remember { Animatable(0f) }

    val codePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { vm.scanImage(it) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER } }
    val selector = remember {
        ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build()
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(95)
            .setResolutionSelector(selector)
            .build()
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analysis = remember {
        ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(
                        androidx.camera.core.resolutionselector.ResolutionStrategy(
                            android.util.Size(1280, 960),
                            androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    DisposableEffect(Unit) { onDispose { analysis.clearAnalyzer(); analysisExecutor.shutdown() } }

    LaunchedEffect(lifecycleOwner, retry) {
        cameraError = false
        try {
            val provider = ProcessCameraProvider.awaitInstance(context)
            val preview = Preview.Builder().setResolutionSelector(selector).build().also { it.surfaceProvider = previewView.surfaceProvider }
            analysis.setAnalyzer(analysisExecutor) { image ->
                if (currentMode == ScanMode.QR) {
                    val media = image.image
                    if (media == null || codeShowing) {
                        image.close(); return@setAnalyzer
                    }
                    val rotation = image.imageInfo.rotationDegrees
                    val uw = if (rotation % 180 == 0) image.width else image.height
                    val uh = if (rotation % 180 == 0) image.height else image.width
                    val task = runCatching { Barcodes.client.process(InputImage.fromMediaImage(media, rotation)) }
                        .onFailure { image.close() }.getOrNull() ?: return@setAnalyzer
                    task
                        .addOnSuccessListener { codes ->
                            val hit = codes.firstOrNull { it.rawValue != null }
                            previewView.post {
                                frameAspect = uw.toFloat() / uh
                                codeBox = hit?.boundingBox?.let { b -> floatArrayOf(b.left.toFloat() / uw, b.top.toFloat() / uh, b.right.toFloat() / uw, b.bottom.toFloat() / uh) }
                                if (hit != null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.onCode(Barcodes.formatName(hit.format), hit.rawValue!!)
                                }
                            }
                        }
                        .addOnCompleteListener { image.close() }
                } else {
                    val (q, aspect) = runCatching { analyze(image) }.getOrDefault(null to frameAspect)
                    previewView.post {
                        val now = SystemClock.elapsedRealtime()
                        frameAspect = aspect
                        codeBox = null
                        val prev = shownQuad
                        if (q == null) {
                            stableSince = now
                            holdProgress = 0f
                            if (++misses >= 6) {
                                shownQuad = null; armed = true
                            }
                            return@post
                        }
                        misses = 0
                        val target = prev?.let { alignQuad(q, it) } ?: q
                        val delta = prev?.let { maxCornerDelta(it, target) } ?: Float.MAX_VALUE
                        if (delta > 0.08f) armed = true
                        if (delta > 0.02f) stableSince = now
                        shownQuad = when {
                            prev == null || delta > 0.15f -> target
                            delta < 0.004f -> prev
                            else -> lerpQuad(prev, target, 0.3f)
                        }
                        holdProgress = if (armed) ((now - stableSince).toFloat() / AUTO_HOLD_MS).coerceIn(0f, 1f) else 0f
                    }
                }
            }
            provider.unbindAll()
            val camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, analysis)
            hasFlash = camera.cameraInfo.hasFlashUnit()
            imageCapture.flashMode = if (hasFlash) flashMode else ImageCapture.FLASH_MODE_OFF
        } catch (_: Exception) {
            cameraError = true
        }
    }
    LaunchedEffect(mode) { shownQuad = null; misses = 0; codeBox = null }

    fun capture() {
        if (shutterBusy || mode == ScanMode.QR) return
        shutterBusy = true
        armed = false
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch { flashAlpha.snapTo(0.6f); flashAlpha.animateTo(0f) }
        val file = vm.newFile()
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    shutterBusy = false
                    vm.onCaptured(file, frameAspect, shownQuad, onIdDone = onReview)
                }

                override fun onError(exception: ImageCaptureException) {
                    shutterBusy = false
                    file.delete()
                    vm.error.value = CameraError.CAPTURE
                }
            },
        )
    }

    LaunchedEffect(holdProgress, autoCapture) {
        if (autoCapture && mode != ScanMode.QR && holdProgress >= 1f && !shutterBusy && !cameraError) capture()
    }

    val sweep = if (animationsEnabled()) rememberInfiniteTransition(label = "sweep").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "sweep",
    ).value else 0.5f

    val toolButtons: @Composable () -> Unit = {
        if (mode != ScanMode.QR) {
            if (mode != ScanMode.ID_CARD) GlassButton(
                if (showGrid) Icons.Outlined.GridOn else Icons.Outlined.GridOff,
                stringResource(if (showGrid) R.string.grid_on else R.string.grid_off),
                { scope.launch { settingsRepo.setCameraGrid(!showGrid) } },
                active = showGrid,
            )
            AutoChip(autoCapture) { scope.launch { settingsRepo.setAutoCapture(!autoCapture) } }
        }
        if (hasFlash) {
            val (icon, label) = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> Icons.Outlined.FlashOn to R.string.flash_on
                ImageCapture.FLASH_MODE_AUTO -> Icons.Outlined.FlashAuto to R.string.flash_auto
                else -> Icons.Outlined.FlashOff to R.string.flash_off
            }
            GlassButton(icon, stringResource(label), {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                imageCapture.flashMode = flashMode
            }, active = flashMode != ImageCapture.FLASH_MODE_OFF)
        }
    }
    val preview: @Composable (Modifier) -> Unit = { m ->
        BoxWithConstraints(m) {
            AndroidView({ previewView }, Modifier.fillMaxSize())
            if (!cameraError) Canvas(Modifier.fillMaxSize()) {
                val boxAspect = size.width / size.height
                val (w, h) = if (frameAspect > boxAspect) size.width to size.width / frameAspect else size.height * frameAspect to size.height
                val left = (size.width - w) / 2
                val top = (size.height - h) / 2
                fun p(x: Float, y: Float) = Offset(left + x * w, top + y * h)
                val gridColor = Color.White.copy(alpha = 0.38f)
                val thin = 1.dp.toPx()
                when (mode) {
                    ScanMode.QR -> {
                        val side = minOf(w, h) * 0.68f
                        val win = androidx.compose.ui.geometry.Rect(Offset(left + (w - side) / 2, top + (h - side) / 2), Size(side, side))
                        val dim = Path().apply {
                            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                            addRoundRect(androidx.compose.ui.geometry.RoundRect(win, CornerRadius(28.dp.toPx())))
                            fillType = PathFillType.EvenOdd
                        }
                        drawPath(dim, Color.Black.copy(alpha = 0.5f))
                        val grad = Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd), win.topLeft, win.bottomRight)
                        val arm = side * 0.16f
                        val sw = 6.dp.toPx()
                        listOf(win.topLeft to (1 to 1), win.topRight to (-1 to 1), win.bottomLeft to (1 to -1), win.bottomRight to (-1 to -1)).forEach { (o, d) ->
                            drawLine(grad, o, Offset(o.x + arm * d.first, o.y), sw, StrokeCap.Round)
                            drawLine(grad, o, Offset(o.x, o.y + arm * d.second), sw, StrokeCap.Round)
                        }
                        val ly = win.top + side * (0.08f + 0.84f * sweep)
                        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, brand.heroEnd, Color.Transparent), win.left, win.right), Offset(win.left + 12f, ly), Offset(win.right - 12f, ly), 3.dp.toPx())
                        codeBox?.let { b ->
                            drawRoundRect(brand.heroStart, p(b[0], b[1]), Size((b[2] - b[0]) * w, (b[3] - b[1]) * h), CornerRadius(12.dp.toPx()), style = Stroke(3.dp.toPx()))
                        }
                    }

                    ScanMode.ID_CARD -> {
                        val g = idCardGuide(frameAspect)
                        val card = androidx.compose.ui.geometry.Rect(p(g[0], g[1]), p(g[4], g[5]))
                        val dim = Path().apply {
                            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                            addRoundRect(androidx.compose.ui.geometry.RoundRect(card, CornerRadius(18.dp.toPx())))
                            fillType = PathFillType.EvenOdd
                        }
                        drawPath(dim, Color.Black.copy(alpha = 0.45f))
                        drawRoundRect(
                            Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)), card.topLeft, card.size, CornerRadius(18.dp.toPx()),
                            style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)))
                        )
                    }

                    else -> {
                        val q = shownQuad

                        if (showGrid) {
                            val g = q ?: FULL_QUAD
                            for (i in 1..2) {
                                val t = i / 3f
                                val (ax, ay) = quadPoint(g, t, 0f); val (bx, by) = quadPoint(g, t, 1f)
                                drawLine(gridColor, p(ax, ay), p(bx, by), thin)
                                val (cx, cy) = quadPoint(g, 0f, t); val (dx, dy) = quadPoint(g, 1f, t)
                                drawLine(gridColor, p(cx, cy), p(dx, dy), thin)
                            }
                        }
                        if (q != null) {
                            val path = Path().apply {
                                moveTo(p(q[0], q[1]).x, p(q[0], q[1]).y)
                                for (i in 1 until 4) lineTo(p(q[i * 2], q[i * 2 + 1]).x, p(q[i * 2], q[i * 2 + 1]).y)
                                close()
                            }
                            drawPath(path, brand.heroStart.copy(alpha = 0.18f))
                            drawPath(path, Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)), style = Stroke(3.dp.toPx(), join = StrokeJoin.Round))
                            if (mode == ScanMode.BOOK) {
                                val (ax, ay) = quadPoint(q, 0.5f, 0f); val (bx, by) = quadPoint(q, 0.5f, 1f)
                                drawLine(Color.White, p(ax, ay), p(bx, by), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                            }
                            for (i in 0 until 4) drawCircle(Color.White, 5.dp.toPx(), p(q[i * 2], q[i * 2 + 1]))
                        }
                    }
                }
            }
            Box(Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = flashAlpha.value)))
            if (cameraError) {
                Notice(
                    Icons.Outlined.NoPhotography, stringResource(R.string.camera_error_title), stringResource(R.string.camera_error_body),
                    stringResource(R.string.retry), { retry++ }, null,
                    secondary = stringResource(R.string.import_images) to importImages,
                )
            } else {
                val hint = when (mode) {
                    ScanMode.QR -> R.string.hint_qr
                    ScanMode.ID_CARD -> if (idSides == 0) R.string.hint_id_front else R.string.hint_id_back
                    ScanMode.BOOK -> if (shownQuad == null) R.string.hint_book else R.string.hint_book_found
                    ScanMode.WHITEBOARD -> if (shownQuad == null) R.string.hint_whiteboard else R.string.edges_found
                    ScanMode.DOCUMENT -> when {
                        shownQuad == null -> R.string.edges_not_found
                        autoCapture && armed -> R.string.hold_steady
                        else -> R.string.edges_found
                    }
                }
                Text(
                    stringResource(hint),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            error?.let {
                Snackbar(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    action = { TextButton(onClick = { vm.error.value = null }) { Text(stringResource(R.string.ok)) } },
                ) {
                    Text(
                        stringResource(
                            when (it) {
                                CameraError.CAPTURE -> R.string.capture_failed
                                CameraError.IMPORT -> R.string.import_failed
                                CameraError.NO_CODE -> R.string.no_code_in_image
                            }
                        )
                    )
                }
            }
        }
    }
    val controls: @Composable (Boolean) -> Unit = { vertical ->
        Line(vertical, Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            val qr = mode == ScanMode.QR
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
                GlassButton(Icons.Outlined.PhotoLibrary, stringResource(R.string.gallery), {
                    if (qr) codePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else importImages()
                }, size = 56)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(if (qr) R.string.from_photo else R.string.gallery), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            val shutterLabel = stringResource(if (qr) R.string.hint_qr else R.string.capture_page)
            val ring = Brush.sweepGradient(listOf(brand.heroStart, brand.heroEnd, brand.heroStart))
            Box(
                Modifier
                    .size(80.dp)
                    .drawBehind {
                        val w = 5.dp.toPx()
                        drawCircle(Color.White.copy(alpha = 0.35f), size.minDimension / 2 - w / 2, style = Stroke(w))

                        val start = if (qr) 360f * sweep - 90f else -90f
                        val arc = when {
                            qr -> 90f; autoCapture -> 360f * holdProgress; else -> 360f
                        }
                        drawArc(ring, start, arc, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
                    }
                    .padding(9.dp)
                    .clip(CircleShape)
                    .background(if (qr) Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)) else Brush.linearGradient(listOf(if (shutterBusy || cameraError) Color.Gray else Color.White, if (shutterBusy || cameraError) Color.Gray else Color.White)))
                    .semantics { contentDescription = shutterLabel; role = Role.Button }
                    .bounceClick(enabled = !shutterBusy && !cameraError && !qr) { capture() },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    qr -> Icon(Icons.Outlined.QrCodeScanner, null, tint = brand.onHero, modifier = Modifier.size(30.dp))
                    shutterBusy -> CircularProgressIndicator(Modifier.size(32.dp), color = brand.heroStart)
                    mode == ScanMode.ID_CARD -> Text(if (idSides == 0) "1" else "2", style = MaterialTheme.typography.titleLarge, color = brand.heroStart)
                }
            }
            if (qr) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
                    GlassButton(Icons.Outlined.History, stringResource(R.string.scanned_codes), onCodes, size = 56)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.history), color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                val doneLabel = pluralStringResource(R.plurals.review_pages, pages.size, pages.size)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(88.dp)
                        .clickable(enabled = pages.isNotEmpty() && saving == 0, onClickLabel = doneLabel) { onReview() },
                ) {
                    BadgedBox(badge = { if (pages.isNotEmpty()) Badge(containerColor = brand.heroStart, contentColor = brand.onHero) { Text("${pages.size}") } }) {
                        Box(
                            Modifier
                                .size(48.dp, 60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(BorderStroke(2.dp, Color.White), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                saving > 0 -> CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                pages.isNotEmpty() -> PageImage(pages.last(), 200, null, Modifier.fillMaxSize(), ContentScale.Crop)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.done), color = if (pages.isNotEmpty()) Color.White else Color.Gray, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    val window = LocalWindowLayout.current
    val density = LocalDensity.current
    var rootTop by remember { mutableIntStateOf(0) }
    Box(Modifier
        .fillMaxSize()
        .onGloballyPositioned { rootTop = it.positionInWindow().y.toInt() }) {
        when {
            window.tabletop && window.hingeTopPx > rootTop -> Column(Modifier.fillMaxSize()) {
                Box(Modifier
                    .fillMaxWidth()
                    .height(with(density) { (window.hingeTopPx - rootTop).toDp() })) {
                    preview(Modifier.fillMaxSize())
                    Row(Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassButton(Icons.Outlined.Close, stringResource(R.string.close_camera), onBack)
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { toolButtons() }
                    }
                }
                Column(Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(), verticalArrangement = Arrangement.Center) {
                    ModeCarousel(mode, vertical = false) { vm.mode.value = it }
                    controls(false)
                }
            }

            window.landscape -> Row(Modifier
                .fillMaxSize()
                .safeDrawingPadding()) {
                Column(Modifier
                    .fillMaxHeight()
                    .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassButton(Icons.Outlined.Close, stringResource(R.string.close_camera), onBack)
                    Spacer(Modifier.weight(1f))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) { toolButtons() }
                }
                preview(Modifier
                    .weight(1f)
                    .fillMaxHeight())
                ModeCarousel(mode, vertical = true, modifier = Modifier.fillMaxHeight()) { vm.mode.value = it }
                controls(true)
            }

            else -> Column(Modifier
                .fillMaxSize()
                .safeDrawingPadding()) {
                Row(Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassButton(Icons.Outlined.Close, stringResource(R.string.close_camera), onBack)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { toolButtons() }
                }
                preview(Modifier
                    .weight(1f)
                    .fillMaxWidth())
                ModeCarousel(mode, vertical = false) { vm.mode.value = it }
                controls(false)
            }
        }
    }

    code?.let { CodeSheet(it.raw, it.format, vm::dismissCode) }
}

private const val AUTO_HOLD_MS = 1200L

private fun modeLabel(m: ScanMode) = when (m) {
    ScanMode.DOCUMENT -> R.string.mode_document
    ScanMode.BOOK -> R.string.mode_book
    ScanMode.ID_CARD -> R.string.mode_id_card
    ScanMode.WHITEBOARD -> R.string.mode_whiteboard
    ScanMode.QR -> R.string.mode_qr
}

fun modeIcon(m: ScanMode): androidx.compose.ui.graphics.vector.ImageVector = when (m) {
    ScanMode.DOCUMENT -> Icons.Outlined.Description
    ScanMode.BOOK -> Icons.AutoMirrored.Outlined.MenuBook
    ScanMode.ID_CARD -> Icons.Outlined.Badge
    ScanMode.WHITEBOARD -> Icons.Outlined.CoPresent
    ScanMode.QR -> Icons.Outlined.QrCodeScanner
}

@Composable
private fun ModeCarousel(selected: ScanMode, vertical: Boolean, modifier: Modifier = Modifier, onSelect: (ScanMode) -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(selected) { state.animateScrollToItem(maxOf(0, selected.ordinal - 1)) }
    if (vertical) LazyColumn(
        modifier.padding(horizontal = 4.dp),
        state = state,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) { items(ScanMode.entries) { ModeChip(it, it == selected, onSelect) } }
    else LazyRow(
        modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { items(ScanMode.entries) { ModeChip(it, it == selected, onSelect) } }
}

@Composable
private fun Line(vertical: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    if (vertical) Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) { content() }
    else Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { content() }
}

@Composable
private fun ModeChip(m: ScanMode, on: Boolean, onSelect: (ScanMode) -> Unit) {
    val brand = LocalScanColors.current
    val label = stringResource(modeLabel(m))
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f))))
            .bounceClick(role = Role.Tab) { onSelect(m) }
            .semantics { this.selected = on }
            .heightIn(min = 40.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(modeIcon(m), null, tint = if (on) brand.onHero else Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (on) brand.onHero else Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, active: Boolean = false, size: Int = 44) {
    val brand = LocalScanColors.current
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (active) Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.16f))))
            .bounceClick(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (active) brand.onHero else Color.White, modifier = Modifier.size((size * 0.5f).dp)) }
}

@Composable
private fun AutoChip(on: Boolean, onToggle: () -> Unit) {
    val brand = LocalScanColors.current
    val label = stringResource(R.string.auto_capture)
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) Brush.linearGradient(listOf(brand.heroStart, brand.heroEnd)) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.16f))))
            .semantics { stateDescription = if (on) "on" else "off" }
            .bounceClick(role = Role.Switch, onClick = onToggle)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.AutoMode, null, tint = if (on) brand.onHero else Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (on) brand.onHero else Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

private fun analyze(image: ImageProxy): Pair<Quad?, Float> = image.use {
    val plane = it.planes[0]
    val buf = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val step = maxOf(1, it.width / 160)
    val half = step / 2
    val w = it.width / step
    val h = it.height / step
    val lum = IntArray(w * h)
    fun px(x: Int, y: Int) = buf.get(y * rowStride + x * pixelStride).toInt() and 0xFF
    for (y in 0 until h) {
        val y0 = y * step
        val y1 = y0 + half
        for (x in 0 until w) {
            val x0 = x * step
            val x1 = x0 + half
            lum[y * w + x] = (px(x0, y0) + px(x1, y0) + px(x0, y1) + px(x1, y1)) shr 2
        }
    }
    val rotation = it.imageInfo.rotationDegrees
    val q = detectQuad(lum, w, h)?.rotated(rotation)?.let(::orderQuad)
    val aspect = if (rotation % 180 == 0) it.width.toFloat() / it.height else it.height.toFloat() / it.width
    q to aspect
}

@Composable
private fun Notice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    onBack: (() -> Unit)?,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Box(Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) { Icon(Icons.Outlined.Close, stringResource(R.string.close_camera)) }
        }
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            com.classicscan.ui.components.IconTile(icon, LocalScanColors.current.heroStart, 72)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            com.classicscan.ui.components.AppButton(action, onAction)
            secondary?.let { (label, act) ->
                Spacer(Modifier.height(12.dp))
                com.classicscan.ui.components.AppButton(label, act, tonal = true)
            }
        }
    }
}
