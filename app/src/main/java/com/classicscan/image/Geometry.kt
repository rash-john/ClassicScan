package com.classicscan.image

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

typealias Quad = FloatArray

val FULL_QUAD: Quad get() = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)

fun Quad.encode(): String = joinToString(",")

fun decodeQuad(s: String?): Quad? =
    s?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 8 }?.toFloatArray()

fun rotateNorm(x: Float, y: Float, degrees: Int): Pair<Float, Float> = when (((degrees % 360) + 360) % 360) {
    90 -> (1 - y) to x
    180 -> (1 - x) to (1 - y)
    270 -> y to (1 - x)
    else -> x to y
}

fun Quad.rotated(degrees: Int): Quad = FloatArray(8).also { out ->
    for (i in 0 until 4) {
        val (x, y) = rotateNorm(this[i * 2], this[i * 2 + 1], degrees)
        out[i * 2] = x; out[i * 2 + 1] = y
    }
}

fun warpedSize(q: Quad, w: Int, h: Int): Pair<Int, Int> {
    fun d(a: Int, b: Int) = hypot((q[a * 2] - q[b * 2]) * w, (q[a * 2 + 1] - q[b * 2 + 1]) * h)
    val outW = max(d(0, 1), d(3, 2)).toInt().coerceAtLeast(1)
    val outH = max(d(0, 3), d(1, 2)).toInt().coerceAtLeast(1)
    return outW to outH
}

private fun otsu(lum: IntArray): Int {
    val hist = IntArray(256)
    for (v in lum) hist[v]++
    val total = lum.size
    var sum = 0.0
    for (i in 0..255) sum += i * hist[i].toDouble()
    var sumB = 0.0; var wB = 0; var best = 0.0; var threshold = 128
    for (t in 0..255) {
        wB += hist[t]; if (wB == 0) continue
        val wF = total - wB; if (wF == 0) break
        sumB += t * hist[t].toDouble()
        val mB = sumB / wB; val mF = (sum - sumB) / wF
        val between = wB.toDouble() * wF * (mB - mF) * (mB - mF)
        if (between > best) { best = between; threshold = t }
    }
    return threshold
}

fun detectQuad(lum: IntArray, w: Int, h: Int): Quad? {
    if (w < 8 || h < 8 || lum.size < w * h) return null
    val b = blur(lum, w, h)
    val t = otsu(b)
    return fitBlob(BooleanArray(w * h) { b[it] > t }, w, h) ?: fitBlob(enclosedByEdges(b, w, h), w, h)
}

private fun blur(src: IntArray, w: Int, h: Int): IntArray {
    val tmp = IntArray(w * h)
    for (y in 0 until h) for (x in 0 until w) {
        val i = y * w + x
        tmp[i] = (src[if (x > 0) i - 1 else i] + 2 * src[i] + src[if (x < w - 1) i + 1 else i]) shr 2
    }
    return IntArray(w * h) { i ->
        val y = i / w
        (tmp[if (y > 0) i - w else i] + 2 * tmp[i] + tmp[if (y < h - 1) i + w else i]) shr 2
    }
}

private fun enclosedByEdges(b: IntArray, w: Int, h: Int): BooleanArray {
    val g = IntArray(w * h)
    var sum = 0L
    for (y in 1 until h - 1) for (x in 1 until w - 1) {
        val i = y * w + x
        g[i] = kotlin.math.abs(b[i + 1] - b[i - 1]) + kotlin.math.abs(b[i + w] - b[i - w])
        sum += g[i]
    }
    val t = max(10L, sum * 3 / (w * h)).toInt()
    val edge = BooleanArray(w * h)
    for (y in 1 until h - 1) for (x in 1 until w - 1) {
        val i = y * w + x
        if (g[i] > t) { edge[i] = true; edge[i - 1] = true; edge[i + 1] = true; edge[i - w] = true; edge[i + w] = true }
    }
    val outside = BooleanArray(w * h)
    val stack = IntArray(w * h)
    var sp = 0
    fun push(p: Int) { if (!edge[p] && !outside[p]) { outside[p] = true; stack[sp++] = p } }
    for (x in 0 until w) { push(x); push((h - 1) * w + x) }
    for (y in 0 until h) { push(y * w); push(y * w + w - 1) }
    while (sp > 0) {
        val p = stack[--sp]; val x = p % w; val y = p / w
        if (x > 0) push(p - 1)
        if (x < w - 1) push(p + 1)
        if (y > 0) push(p - w)
        if (y < h - 1) push(p + w)
    }
    return BooleanArray(w * h) { !outside[it] }
}

private fun fitBlob(mask: BooleanArray, w: Int, h: Int): Quad? {
    val area = w * h
    val label = IntArray(area)
    val stack = IntArray(area)
    var bestLabel = 0; var bestSize = 0; var next = 0
    for (start in 0 until area) {
        if (!mask[start] || label[start] != 0) continue
        next++
        var sp = 0; var size = 0
        stack[sp++] = start; label[start] = next
        while (sp > 0) {
            val p = stack[--sp]; size++
            val x = p % w; val y = p / w
            if (x > 0 && mask[p - 1] && label[p - 1] == 0) { label[p - 1] = next; stack[sp++] = p - 1 }
            if (x < w - 1 && mask[p + 1] && label[p + 1] == 0) { label[p + 1] = next; stack[sp++] = p + 1 }
            if (y > 0 && mask[p - w] && label[p - w] == 0) { label[p - w] = next; stack[sp++] = p - w }
            if (y < h - 1 && mask[p + w] && label[p + w] == 0) { label[p + w] = next; stack[sp++] = p + w }
        }
        if (size > bestSize) { bestSize = size; bestLabel = next }
    }
    if (bestSize < area * 0.08) return null

    val pts = ArrayList<FloatArray>(h * 2)
    for (y in 0 until h) {
        var lo = -1; var hi = -1
        for (x in 0 until w) if (label[y * w + x] == bestLabel) { if (lo < 0) lo = x; hi = x }
        if (lo >= 0) { pts += floatArrayOf(lo + 0.5f, y + 0.5f); if (hi != lo) pts += floatArrayOf(hi + 0.5f, y + 0.5f) }
    }
    val hull = convexHull(pts)
    if (hull.size < 4) return null
    val hullArea = shoelace(hull)
    if (hullArea < area * 0.12f || hullArea > area * 0.97f || bestSize / hullArea < 0.6f) return null

    val corners = maxAreaQuad(hull)
    val quad = FloatArray(8)
    corners.forEachIndexed { i, c -> quad[i * 2] = hull[c][0] / w; quad[i * 2 + 1] = hull[c][1] / h }
    if (polygonArea(quad) * area / hullArea < 0.9f) return null
    return orderQuad(quad)
}

private fun convexHull(points: List<FloatArray>): List<FloatArray> {
    val p = points.sortedWith(compareBy<FloatArray>({ it[0] }, { it[1] }))
    if (p.size < 3) return p
    fun cross(o: FloatArray, a: FloatArray, b: FloatArray) = (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
    val hull = ArrayList<FloatArray>(p.size)
    for (pass in 0..1) {
        val base = hull.size
        for (pt in if (pass == 0) p else p.asReversed()) {
            while (hull.size >= base + 2 && cross(hull[hull.size - 2], hull[hull.size - 1], pt) <= 0) hull.removeAt(hull.size - 1)
            hull += pt
        }
        hull.removeAt(hull.size - 1)
    }
    return hull
}

private fun shoelace(poly: List<FloatArray>): Float {
    var a = 0f
    for (i in poly.indices) { val j = (i + 1) % poly.size; a += poly[i][0] * poly[j][1] - poly[j][0] * poly[i][1] }
    return kotlin.math.abs(a) / 2f
}

private fun maxAreaQuad(hull: List<FloatArray>): IntArray {
    val n = hull.size
    fun tri(a: Int, b: Int, c: Int): Float {
        val p = hull[a]; val q = hull[b]; val r = hull[c]
        return kotlin.math.abs((q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0]))
    }
    val extremes = intArrayOf(
        hull.indices.minBy { hull[it][0] + hull[it][1] }, hull.indices.maxBy { hull[it][0] - hull[it][1] },
        hull.indices.maxBy { hull[it][0] + hull[it][1] }, hull.indices.minBy { hull[it][0] - hull[it][1] },
    ).distinct().sorted()
    val idx = if (extremes.size == 4) extremes.toIntArray() else IntArray(4) { it * n / 4 }
    for (iter in 0 until 10) {
        var changed = false
        for (k in 0 until 4) {
            val prev = idx[(k + 3) % 4]; val next = idx[(k + 1) % 4]
            var best = idx[k]; var bestA = tri(prev, best, next)
            var j = (prev + 1) % n
            while (j != next) { val a = tri(prev, j, next); if (a > bestA) { bestA = a; best = j }; j = (j + 1) % n }
            if (best != idx[k]) { idx[k] = best; changed = true }
        }
        if (!changed) break
    }
    return idx
}

fun polygonArea(q: Quad): Float {
    var a = 0f
    for (i in 0 until 4) {
        val j = (i + 1) % 4
        a += q[i * 2] * q[j * 2 + 1] - q[j * 2] * q[i * 2 + 1]
    }
    return kotlin.math.abs(a) / 2f
}

fun clampQuad(q: Quad): Quad = FloatArray(8) { min(1f, max(0f, q[it])) }

fun maxCornerDelta(a: Quad, b: Quad): Float {
    var m = 0f
    for (i in 0 until 4) m = max(m, hypot(a[i * 2] - b[i * 2], a[i * 2 + 1] - b[i * 2 + 1]))
    return m
}

fun suggestTitle(text: String?): String? = text?.lineSequence()
    ?.map { it.trim().trim('-', '_', '*', '|', ':', '.', '#').trim() }
    ?.firstOrNull { line -> line.count { it.isLetter() } >= 3 && line.length in 3..60 }

fun splitQuad(q: Quad): Pair<Quad, Quad> {
    val mtX = (q[0] + q[2]) / 2; val mtY = (q[1] + q[3]) / 2
    val mbX = (q[4] + q[6]) / 2; val mbY = (q[5] + q[7]) / 2
    return floatArrayOf(q[0], q[1], mtX, mtY, mbX, mbY, q[6], q[7]) to
        floatArrayOf(mtX, mtY, q[2], q[3], q[4], q[5], mbX, mbY)
}

fun quadPoint(q: Quad, u: Float, v: Float): Pair<Float, Float> {
    val topX = q[0] + (q[2] - q[0]) * u; val topY = q[1] + (q[3] - q[1]) * u
    val botX = q[6] + (q[4] - q[6]) * u; val botY = q[7] + (q[5] - q[7]) * u
    return (topX + (botX - topX) * v) to (topY + (botY - topY) * v)
}

fun lerpQuad(a: Quad, b: Quad, t: Float): Quad = FloatArray(8) { a[it] + (b[it] - a[it]) * t }

fun orderQuad(q: Quad): Quad {
    var signed = 0f
    for (i in 0 until 4) { val j = (i + 1) % 4; signed += q[i * 2] * q[j * 2 + 1] - q[j * 2] * q[i * 2 + 1] }
    val order = if (signed < 0) intArrayOf(0, 3, 2, 1) else intArrayOf(0, 1, 2, 3)
    val first = (0 until 4).minBy { q[order[it] * 2] + q[order[it] * 2 + 1] }
    return FloatArray(8) { val c = order[(first + it / 2) % 4]; q[c * 2 + it % 2] }
}

fun alignQuad(q: Quad, ref: Quad): Quad = (0 until 4).map { r -> FloatArray(8) { q[(it + r * 2) % 8] } }
    .minBy { c -> (0 until 8).sumOf { ((c[it] - ref[it]) * (c[it] - ref[it])).toDouble() } }
