package com.example.notas.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Images live as files in the app's private storage, and notes refer to them by file name
 * (`![](img_….jpg)`). Imported pictures are scaled down so a few photos don't fill the phone.
 */
class ImageStore(private val context: Context) {
    val dir: File = File(context.filesDir, "images").apply { mkdirs() }

    fun file(name: String) = File(dir, name)

    /** Copies (and shrinks) a picked or captured picture into the app. Returns its file name. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaled(uri) ?: return@runCatching null
            val png = bitmap.hasAlpha()
            val name = "img_${UUID.randomUUID()}.${if (png) "png" else "jpg"}"
            file(name).outputStream().use { out ->
                bitmap.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            name
        }.getOrNull()
    }

    private fun decodeScaled(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder also applies the photo's EXIF rotation.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val scale = minOf(1f, MaxSide / max(w, h).toFloat())
                decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MaxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
    }

    /** A fresh temporary file + content URI for the camera app to write into. */
    fun newCameraTarget(): Pair<File, Uri> {
        val camDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val f = File(camDir, "photo_${System.currentTimeMillis()}.jpg")
        return f to FileProvider.getUriForFile(context, "${context.packageName}.files", f)
    }

    /** Shareable URIs for the given images (so "Compartir" can send them along with the text). */
    fun shareUris(names: List<String>): List<Uri> = names.map { file(it) }.filter { it.exists() }.map {
        FileProvider.getUriForFile(context, "${context.packageName}.files", it)
    }

    /**
     * Deletes image files no note refers to any more. Files younger than a day are kept, so an
     * image added to a note that hasn't been saved yet is never lost.
     */
    suspend fun cleanUp(referenced: Set<String>) = withContext(Dispatchers.IO) {
        val dayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        dir.listFiles()?.forEach { f ->
            if (f.name !in referenced && f.lastModified() < dayAgo) f.delete()
        }
        File(context.cacheDir, "camera").listFiles()?.forEach { if (it.lastModified() < dayAgo) it.delete() }
    }

    private companion object {
        const val MaxSide = 2048f
    }
}
