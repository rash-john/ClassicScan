package com.classicscan.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import com.classicscan.data.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

enum class Filter { ORIGINAL, AUTO, GRAYSCALE, BW, COLOR, WHITEBOARD }

fun filterOf(name: String): Filter = Filter.entries.firstOrNull { it.name == name } ?: Filter.AUTO

fun adjustMatrix(brightness: Float, contrast: Float): FloatArray {
    val c = 1f + contrast.coerceIn(-1f, 1f) * 0.8f
    val t = 128f * (1 - c) + brightness.coerceIn(-1f, 1f) * 90f
    return floatArrayOf(c, 0f, 0f, 0f, t, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t, 0f, 0f, 0f, 1f, 0f)
}

class ImageProcessor(private val originalsDir: File) {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun fileOf(page: PageEntity) = File(originalsDir, page.originalFile)

    fun orientedSize(file: File): Pair<Int, Int>? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, o)
        if (o.outWidth <= 0 || o.outHeight <= 0) return null
        val rot = exifRotation(file)
        return if (rot % 180 == 0) o.outWidth to o.outHeight else o.outHeight to o.outWidth
    }

    private fun exifRotation(file: File): Int = runCatching {
        ExifInterface(file).rotationDegrees
    }.getOrDefault(0)

    fun decodeOriented(file: File, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
        val bmp = try {
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (_: OutOfMemoryError) {
            null
        } ?: return null
        val rot = exifRotation(file)
        return if (rot == 0) bmp else rotate(bmp, rot)
    }

    private fun rotate(src: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return src
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
        if (out != src) src.recycle()
        return out
    }

    suspend fun detect(file: File): Quad? = withContext(Dispatchers.Default) {
        val bmp = decodeOriented(file, 320) ?: return@withContext null
        val small = Bitmap.createScaledBitmap(bmp, 160, (160f * bmp.height / bmp.width).roundToInt().coerceAtLeast(8), true)
        val px = IntArray(small.width * small.height)
        small.getPixels(px, 0, small.width, 0, 0, small.width, small.height)
        val lum = IntArray(px.size) { luma(px[it]) }
        detectQuad(lum, small.width, small.height)
    }

    suspend fun render(
        page: PageEntity,
        maxDim: Int,
        crop: Quad? = decodeQuad(page.crop),
        rotation: Int = page.rotation,
        filter: Filter = filterOf(page.filter),
        brightness: Float = page.brightness,
        contrast: Float = page.contrast,
        ink: List<Stroke> = decodeInk(page.ink),
        useCache: Boolean = true,
    ): Bitmap? = withContext(Dispatchers.Default) {
        val key = "${page.id}:${crop?.encode()}:$rotation:$filter:$brightness:$contrast:${ink.hashCode()}:$maxDim"
        if (useCache) cache.get(key)?.let { return@withContext it }

        val decodeDim = crop?.let { q ->
            val (w, h) = page.width to page.height
            val (cw, ch) = warpedSize(q, w, h)
            (maxDim.toLong() * max(w, h) / max(cw, ch).coerceAtLeast(1)).toInt().coerceAtMost(max(w, h))
        } ?: maxDim
        try {
            val baseKey = "${page.id}:base:${crop?.encode()}:$rotation:$maxDim"
            var out = cache.get(baseKey)?.takeIf { useCache }?.copy(Bitmap.Config.ARGB_8888, true) ?: run {
                val src = decodeOriented(fileOf(page), decodeDim) ?: return@withContext null
                ensureActive()
                val base = rotate(if (crop == null) src else warp(src, crop).also { if (it != src) src.recycle() }, rotation)
                if (useCache) base.copy(Bitmap.Config.ARGB_8888, false)?.let { cache.put(baseKey, it) }
                base
            }
            ensureActive()
            out = applyFilter(out, filter)
            ensureActive()
            if (brightness != 0f || contrast != 0f) out = colorMatrix(out, adjust(brightness, contrast))
            if (ink.isNotEmpty()) out = drawInk(out, ink)
            if (useCache) cache.put(key, out)
            out
        } catch (_: OutOfMemoryError) {
            cache.evictAll()
            null
        }
    }

    suspend fun original(page: PageEntity, maxDim: Int): Bitmap? = withContext(Dispatchers.Default) {
        val key = "${page.id}:orig:$maxDim"
        cache.get(key) ?: decodeOriented(fileOf(page), maxDim)?.also { cache.put(key, it) }
    }

    fun evict(pageId: String) {
        cache.snapshot().keys.filter { it.startsWith("$pageId:") }.forEach { cache.remove(it) }
    }

    private fun warp(src: Bitmap, q: Quad): Bitmap {
        val w = src.width; val h = src.height
        val (outW, outH) = warpedSize(q, w, h)
        val from = FloatArray(8) { if (it % 2 == 0) q[it] * w else q[it] * h }
        val to = floatArrayOf(0f, 0f, outW.toFloat(), 0f, outW.toFloat(), outH.toFloat(), 0f, outH.toFloat())
        val m = Matrix()
        if (!m.setPolyToPoly(from, 0, to, 0, 4)) return src
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }

    private fun applyFilter(src: Bitmap, filter: Filter): Bitmap = when (filter) {
        Filter.ORIGINAL -> src
        Filter.GRAYSCALE -> colorMatrix(src, ColorMatrix().apply { setSaturation(0f) })
        Filter.COLOR -> colorMatrix(src, ColorMatrix().apply {
            setSaturation(1.3f)
            postConcat(contrast(1.15f))
        })
        Filter.AUTO -> normalize(src, bw = false)
        Filter.BW -> normalize(src, bw = true)

        Filter.WHITEBOARD -> colorMatrix(normalize(src, bw = false), ColorMatrix().apply { setSaturation(1.6f) })
    }

    private fun adjust(brightness: Float, contrast: Float) = ColorMatrix(adjustMatrix(brightness, contrast))

    private fun drawInk(src: Bitmap, strokes: List<Stroke>): Bitmap {
        val out = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true).also { src.recycle() }
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        for (s in strokes) {
            paint.color = s.color
            paint.strokeWidth = s.width * out.width
            val path = android.graphics.Path()
            path.moveTo(s.points[0] * out.width, s.points[1] * out.height)
            var i = 2
            while (i + 1 < s.points.size) { path.lineTo(s.points[i] * out.width, s.points[i + 1] * out.height); i += 2 }
            canvas.drawPath(path, paint)
        }
        return out
    }

    private fun contrast(c: Float): ColorMatrix {
        val t = 128f * (1 - c)
        return ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, t, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t, 0f, 0f, 0f, 1f, 0f))
    }

    private fun colorMatrix(src: Bitmap, cm: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        src.recycle()
        return out
    }

    private fun normalize(src: Bitmap, bw: Boolean): Bitmap {
        val w = src.width; val h = src.height
        val cell = max(8, max(w, h) / 48)
        val gw = (w + cell - 1) / cell; val gh = (h + cell - 1) / cell
        val bg = paperLevel(src, cell, gw, gh)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val lut = IntArray(1025) { tone(it / 1024f) }
        val black = tone(0.8f)
        val strip = 64
        val buf = IntArray(w * (strip + 2))
        val res = IntArray(w * strip)
        var y0 = 0
        while (y0 < h) {
            val rows = min(strip, h - y0)
            val top = max(0, y0 - 1); val count = min(h, y0 + rows + 1) - top
            src.getPixels(buf, 0, w, 0, top, w, count)
            for (r in 0 until count) {
                val gy = ((top + r + 0.5f) / cell - 0.5f).coerceIn(0f, gh - 1f)
                val gy0 = gy.toInt(); val gy1 = min(gy0 + 1, gh - 1); val fy = gy - gy0
                for (x in 0 until w) {
                    val gx = ((x + 0.5f) / cell - 0.5f).coerceIn(0f, gw - 1f)
                    val gx0 = gx.toInt(); val gx1 = min(gx0 + 1, gw - 1); val fx = gx - gx0
                    val i = r * w + x
                    val p = buf[i]
                    var o = 0xFF
                    for (ch in 0..2) {
                        val a = bg[(gy0 * gw + gx0) * 3 + ch]; val b = bg[(gy0 * gw + gx1) * 3 + ch]
                        val c = bg[(gy1 * gw + gx0) * 3 + ch]; val d = bg[(gy1 * gw + gx1) * 3 + ch]
                        val t = a + (b - a) * fx; val u = c + (d - c) * fx
                        val ratio = ((p shr (16 - ch * 8)) and 0xFF) / (t + (u - t) * fy).coerceAtLeast(40f)
                        o = (o shl 8) or lut[(ratio * 1024).toInt().coerceIn(0, 1024)]
                    }
                    buf[i] = o
                }
            }
            for (dy in 0 until rows) {
                val r = y0 + dy - top
                val up = if (r > 0) r - 1 else r; val down = if (r < count - 1) r + 1 else r
                for (x in 0 until w) {
                    val i = r * w + x
                    val l = if (x > 0) i - 1 else i; val rt = if (x < w - 1) i + 1 else i
                    val n = up * w + x; val s = down * w + x
                    val c = buf[i]
                    res[dy * w + x] = if (bw) {
                        val dark = luma(c) < black
                        val alone = luma(buf[l]) >= black && luma(buf[rt]) >= black && luma(buf[n]) >= black && luma(buf[s]) >= black
                        if (dark && !(alone && luma(c) > 110)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    } else if (luma(c) >= 250) c else {
                        var o = 0xFF
                        for (shift in intArrayOf(16, 8, 0)) {
                            val v = (c shr shift) and 0xFF
                            val blur = (((buf[l] shr shift) and 0xFF) + ((buf[rt] shr shift) and 0xFF) +
                                ((buf[n] shr shift) and 0xFF) + ((buf[s] shr shift) and 0xFF) + 4 * v) shr 3
                            o = (o shl 8) or (v + (v - blur) * 3 / 4).coerceIn(0, 255)
                        }
                        o
                    }
                }
            }
            out.setPixels(res, 0, w, 0, y0, w, rows)
            y0 += rows
        }
        src.recycle()
        return out
    }

    private fun tone(ratio: Float): Int =
        (255f * ((ratio - 0.08f) / 0.78f).coerceIn(0f, 1f).pow(1.8f)).roundToInt()

    private fun paperLevel(src: Bitmap, cell: Int, gw: Int, gh: Int): FloatArray {
        val w = src.width; val h = src.height; val n = gw * gh
        val peak = IntArray(n * 3)
        val strip = 64
        val row = IntArray(w * strip)
        var y0 = 0
        while (y0 < h) {
            val rows = min(strip, h - y0)
            src.getPixels(row, 0, w, 0, y0, w, rows)
            var dy = 0
            while (dy + 1 < rows) {
                val base = ((y0 + dy) / cell) * gw
                var x = 0
                while (x + 1 < w) {
                    val a = row[dy * w + x]; val b = row[dy * w + x + 1]
                    val c = row[(dy + 1) * w + x]; val d = row[(dy + 1) * w + x + 1]
                    val k = (base + x / cell) * 3
                    for (ch in 0..2) {
                        val sh = 16 - ch * 8
                        val v = (((a shr sh) and 0xFF) + ((b shr sh) and 0xFF) + ((c shr sh) and 0xFF) + ((d shr sh) and 0xFF)) shr 2
                        if (v > peak[k + ch]) peak[k + ch] = v
                    }
                    x += 2
                }
                dy += 2
            }
            y0 += rows
        }
        fun lum(i: Int) = (peak[i * 3] * 299 + peak[i * 3 + 1] * 587 + peak[i * 3 + 2] * 114) / 1000
        val bg = FloatArray(n * 3)
        var known = BooleanArray(n) { i ->
            val x = i % gw; val y = i / gw
            var m = 0
            for (yy in max(0, y - 6)..min(gh - 1, y + 6)) for (xx in max(0, x - 6)..min(gw - 1, x + 6)) m = max(m, lum(yy * gw + xx))
            lum(i) >= m * 0.6f
        }
        for (i in 0 until n) if (known[i]) for (ch in 0..2) bg[i * 3 + ch] = peak[i * 3 + ch].toFloat()
        var filling = true
        while (filling) {
            filling = false
            val next = known.copyOf()
            for (i in 0 until n) {
                if (known[i]) continue
                val x = i % gw; val y = i / gw
                var c = 0; var r = 0f; var g = 0f; var b = 0f
                for (yy in max(0, y - 1)..min(gh - 1, y + 1)) for (xx in max(0, x - 1)..min(gw - 1, x + 1)) {
                    val j = yy * gw + xx
                    if (known[j]) { c++; r += bg[j * 3]; g += bg[j * 3 + 1]; b += bg[j * 3 + 2] }
                }
                if (c > 0) { bg[i * 3] = r / c; bg[i * 3 + 1] = g / c; bg[i * 3 + 2] = b / c; next[i] = true; filling = true }
            }
            known = next
        }
        return smoothGrid(smoothGrid(bg, gw, gh), gw, gh)
    }

    private fun smoothGrid(g: FloatArray, gw: Int, gh: Int): FloatArray = FloatArray(g.size) { i ->
        val c = i % 3; val x = (i / 3) % gw; val y = i / 3 / gw
        var sum = 0f; var n = 0
        for (yy in max(0, y - 1)..min(gh - 1, y + 1)) for (xx in max(0, x - 1)..min(gw - 1, x + 1)) { sum += g[(yy * gw + xx) * 3 + c]; n++ }
        sum / n
    }

    companion object {
        fun luma(p: Int) = (((p shr 16) and 0xFF) * 299 + ((p shr 8) and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
    }
}
