package com.example.testaipokehit.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.testaipokehit.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

/**
 * Загрузка арта для виджета.
 *
 * В приложении изображения показывает Coil, но виджет рисуется через RemoteViews,
 * поэтому картинка скачивается OkHttp-клиентом приложения и кэшируется:
 * в памяти (LruCache) и в `cacheDir` — чтобы перерисовка виджета не ждала сеть.
 */
object PokemonArtworkLoader {

    private const val CACHE_DIR = "widget_artwork"
    private const val MEMORY_CACHE_KB = 8 * 1024

    /** Больше не нужно: у виджета осмысленно держать только последние арты. */
    private const val MAX_DISK_FILES = 24

    private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    /**
     * Возвращает арт покемона, уменьшенный примерно до [targetPx] по большей стороне,
     * либо `null`, если загрузка не удалась (нет сети, 404, битая картинка).
     */
    suspend fun load(context: Context, url: String, targetPx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val key = "$url@$targetPx"
            memoryCache.get(key)?.let { return@withContext it }

            val file = cacheFile(context, key)
            val bitmap = decodeFile(file, targetPx)
                ?: runCatching {
                    val bytes = RetrofitClient.okHttpClient
                        .newCall(Request.Builder().url(url).build())
                        .execute()
                        .use { response -> if (response.isSuccessful) response.body?.bytes() else null }
                    val decoded = bytes?.let { decodeBytes(it, targetPx) }
                    if (decoded != null) {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { out ->
                            decoded.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        pruneDiskCache(file.parentFile)
                    }
                    decoded
                }.getOrNull()

            bitmap?.also { memoryCache.put(key, it) }
        }

    private fun cacheFile(context: Context, key: String): File =
        File(File(context.cacheDir, CACHE_DIR), "${key.hashCode().toUInt().toString(16)}.png")

    /** Подрезаем кэш по времени изменения: иначе он разросся бы на все 1010 артов. */
    private fun pruneDiskCache(dir: File) {
        val files = dir.listFiles().orEmpty()
        if (files.size <= MAX_DISK_FILES) return
        files.sortedBy(File::lastModified)
            .take(files.size - MAX_DISK_FILES)
            .forEach(File::delete)
    }

    private fun decodeBytes(bytes: ByteArray, targetPx: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        decodeBytes(bytes, targetPx, sampleSizeFor(bounds.outWidth, bounds.outHeight, targetPx))
    }.getOrNull()

    private fun decodeBytes(bytes: ByteArray, targetPx: Int, sampleSize: Int): Bitmap? =
        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            decodeOptions(sampleSize)
        )?.let { scaleTo(it, targetPx) }

    private fun decodeFile(file: File, targetPx: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        BitmapFactory.decodeFile(
            file.absolutePath,
            decodeOptions(sampleSizeFor(bounds.outWidth, bounds.outHeight, targetPx))
        )?.let { scaleTo(it, targetPx) }
    }.getOrNull()

    private fun decodeOptions(sampleSize: Int) = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    /** Сначала уменьшаем примерно вдвое относительно цели, затем точно — так экономим память. */
    private fun sampleSizeFor(width: Int, height: Int, targetPx: Int): Int {
        if (width <= 0 || height <= 0) return 1
        val limit = targetPx * 2
        var sample = 1
        while (width / sample > limit || height / sample > limit) sample *= 2
        return sample
    }

    private fun scaleTo(bitmap: Bitmap, targetPx: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= targetPx) return bitmap
        val ratio = targetPx.toFloat() / largest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }
}