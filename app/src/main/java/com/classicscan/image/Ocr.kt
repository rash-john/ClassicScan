package com.classicscan.image

import android.graphics.Bitmap
import com.classicscan.data.DocumentRepository
import com.classicscan.export.TextBox
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TextRecognizer {
    private val client by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun recognize(bitmap: Bitmap): Text = suspendCancellableCoroutine { c ->
        client.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { c.resume(it) }
            .addOnFailureListener { c.resumeWithException(it) }
    }

    suspend fun lines(bitmap: Bitmap): List<TextBox> = recognize(bitmap).textBlocks.flatMap { it.lines }.mapNotNull { line ->
        val b = line.boundingBox ?: return@mapNotNull null
        TextBox(line.text, b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
    }
}

class TextIndexer(
    private val repo: DocumentRepository,
    private val processor: ImageProcessor,
    private val recognizer: TextRecognizer,
) {
    fun start(scope: CoroutineScope) = scope.launch {
        repo.observeUnindexedCount().distinctUntilChanged().collectLatest { count ->
            if (count == 0) return@collectLatest
            for (page in repo.unindexedPages()) {
                val text = try {
                    val bmp = processor.render(page, 2000, useCache = false)
                    bmp?.let { recognizer.recognize(it).text.trim().also { _ -> it.recycle() } } ?: ""
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ""
                }
                repo.setOcrText(page, text)
            }
        }
    }
}
