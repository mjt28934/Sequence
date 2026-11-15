package com.sequence.watchorder.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ArtKind { POSTER, COVER }

/**
 * Decodes bundled art once and keeps it in a small in-memory cache.
 *
 * Images already in the cache are handed back synchronously so scrolling never shows a flash of
 * empty space, and so snapshot tests can render artwork without a device.
 */
object ArtRepository {
    private val cache = object : LruCache<String, ImageBitmap>(48 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4) / 1024
    }

    /** Test hook: when set, replaces asset loading entirely. */
    var overrideLoader: ((ArtKind, String) -> ImageBitmap?)? = null

    fun peek(kind: ArtKind, id: String): ImageBitmap? = cache.get(key(kind, id))

    fun load(context: Context, kind: ArtKind, id: String): ImageBitmap? {
        val key = key(kind, id)
        cache.get(key)?.let { return it }
        val loader = overrideLoader
        val bitmap = if (loader != null) {
            loader(kind, id)
        } else {
            val folder = if (kind == ArtKind.POSTER) "posters" else "covers"
            try {
                context.assets.open("art/$folder/$id.webp").use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
        if (bitmap != null) cache.put(key, bitmap)
        return bitmap
    }

    /** Decodes ahead of composition; used by tests before a snapshot is taken. */
    fun preload(context: Context, kind: ArtKind, id: String) {
        load(context, kind, id)
    }

    /** Decodes synchronously through the override loader, with no Context required. */
    fun warm(kind: ArtKind, id: String) {
        val key = key(kind, id)
        if (cache.get(key) != null) return
        val loader = overrideLoader ?: return
        loader(kind, id)?.let { cache.put(key, it) }
    }

    private fun key(kind: ArtKind, id: String) = "${kind.name}:$id"
}

@Composable
fun ArtImage(
    kind: ArtKind,
    id: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
) {
    val context = LocalContext.current
    var image by remember(kind, id) { mutableStateOf(ArtRepository.peek(kind, id)) }
    LaunchedEffect(kind, id) {
        if (image == null) {
            image = withContext(Dispatchers.IO) { ArtRepository.load(context, kind, id) }
        }
    }
    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha),
                contentScale = contentScale,
            )
        }
    }
}
