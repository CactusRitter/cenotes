package com.cenotes.app.ui

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The text reader of Google Play services is still being downloaded to the phone (it comes once, by itself). */
class TextReaderNotReady : Exception()

/**
 * The text in a picture of a note, read on the phone itself with ML Kit (Latin script: Spanish, English, German…).
 * The picture is never sent anywhere. Empty when there is no text in it.
 */
suspend fun textFromImage(context: Context, file: File): String = suspendCancellableCoroutine { cont ->
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = try {
        InputImage.fromFilePath(context, Uri.fromFile(file))
    } catch (e: Exception) {
        recognizer.close()
        cont.resumeWithException(e)
        return@suspendCancellableCoroutine
    }
    recognizer.process(image)
        .addOnSuccessListener { result -> cont.resume(result.text.trim()) }
        .addOnFailureListener { e ->
            val notReady = e is com.google.mlkit.common.MlKitException && e.errorCode == com.google.mlkit.common.MlKitException.UNAVAILABLE
            cont.resumeWithException(if (notReady) TextReaderNotReady() else e)
        }
        .addOnCompleteListener { recognizer.close() }
}
