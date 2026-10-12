package com.classicscan.image

import java.util.Locale

data class Stroke(val color: Int, val width: Float, val points: List<Float>)

fun encodeInk(strokes: List<Stroke>): String? = strokes.filter { it.points.size >= 4 }.takeIf { it.isNotEmpty() }?.joinToString("|") { s ->
    "${s.color};${String.format(Locale.US, "%.4f", s.width)};" + s.points.joinToString(",") { String.format(Locale.US, "%.4f", it) }
}

fun decodeInk(s: String?): List<Stroke> = s?.split("|")?.mapNotNull { part ->
    val f = part.split(";")
    if (f.size != 3) return@mapNotNull null
    val pts = f[2].split(",").mapNotNull { it.toFloatOrNull() }
    Stroke(f[0].toIntOrNull() ?: return@mapNotNull null, f[1].toFloatOrNull() ?: return@mapNotNull null, pts).takeIf { pts.size >= 4 && pts.size % 2 == 0 }
}.orEmpty()
