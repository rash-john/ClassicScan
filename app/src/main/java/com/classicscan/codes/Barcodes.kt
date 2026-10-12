package com.classicscan.codes

import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Barcodes {
    val client: BarcodeScanner by lazy {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())
    }

    suspend fun scan(image: InputImage): List<Barcode> = suspendCancellableCoroutine { c ->
        client.process(image).addOnSuccessListener { c.resume(it) }.addOnFailureListener { c.resumeWithException(it) }
    }

    fun formatName(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        else -> "BARCODE"
    }
}

fun formatLabel(format: String) = when (format) {
    "QR_CODE" -> "QR code"
    "DATA_MATRIX" -> "Data Matrix"
    else -> format.replace('_', '-').let { if (it.startsWith("CODE-")) it.replace("CODE-", "Code ") else it }
}
