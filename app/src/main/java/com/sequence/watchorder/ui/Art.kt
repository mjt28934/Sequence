package com.sequence.watchorder.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
    /**
     * Decoded art is the largest thing this app keeps, so the budget follows the process heap
     * instead of a fixed number: a phone with a small heap evicts sooner, a large one holds the
     * whole 37-banner set and never re-decodes while scrolling.
     *
     * Tests pin this so every asset stays resident and screenshots always show real artwork.
     */
    var budgetKbOverride: Int? = null

    private fun heapBudgetKb(): Int {
        val heapMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
        return (heapMb / 6).coerceIn(24, 80) * 1024
    }

    private val cache: LruCache<String, ImageBitmap> by lazy {
        object : LruCache<String, ImageBitmap>(budgetKbOverride ?: heapBudgetKb()) {
            override fun sizeOf(key: String, value: ImageBitmap): Int =
                (value.width * value.height * 4) / 1024
        }
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
    val cached = remember(kind, id) { ArtRepository.peek(kind, id) }
    var image by remember(kind, id) { mutableStateOf(cached) }

    // Art that is already in memory appears instantly; art decoded on demand fades in with a small
    // settle, so a card never pops into place mid-scroll.
    val reveal = remember(kind, id) { Animatable(1f) }
    LaunchedEffect(kind, id) {
        if (image == null) {
            val loaded = withContext(Dispatchers.IO) { ArtRepository.load(context, kind, id) }
            if (loaded != null) {
                if (Motion.enabled) reveal.snapTo(0f)
                image = loaded
                if (Motion.enabled) {
                    reveal.animateTo(1f, tween(durationMillis = 240, easing = LinearOutSlowInEasing))
                }
            }
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
                    .graphicsLayer {
                        val p = reveal.value
                        this.alpha = alpha * p
                        // A 3% overscale that settles as it fades stops the edges from looking stiff.
                        val s = 1f + 0.03f * (1f - p)
                        scaleX = s
                        scaleY = s
                    },
                contentScale = contentScale,
            )
        }
    }
}
