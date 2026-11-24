package com.lowspin.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lowspin.app.data.art.GradientPlaceholder
import com.lowspin.app.ui.LocalAlbumArtRepository

/**
 * Artwork with a deterministic violet placeholder. No crossfade animation per
 * row — a plain Image keeps list scrolling cheap on low-end devices.
 */
@Composable
fun AlbumArtImage(
    songId: Long,
    seed: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 160,
    shape: Shape = RoundedCornerShape(4.dp),
    showRotatingVinyl: Boolean = false,
    isPlaying: Boolean = false
) {
    val repository = LocalAlbumArtRepository.current
    val bitmap by produceState<Bitmap?>(initialValue = null, songId, sizePx) {
        value = repository.loadBitmap(songId, sizePx)
    }

    Box(modifier.clip(shape)) {
        val art = bitmap
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (showRotatingVinyl) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VinylGlyph(modifier = Modifier.fillMaxSize(0.78f), rotating = isPlaying)
            }
        } else {
            GradientPlaceholderBox(seed = seed, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun GradientPlaceholderBox(seed: String, modifier: Modifier = Modifier) {
    val top = remember(seed) { Color(GradientPlaceholder.topColor(seed)) }
    val bottom = remember(seed) { Color(GradientPlaceholder.bottomColor(seed)) }

    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(top, bottom))),
        contentAlignment = Alignment.Center
    ) {
        VinylGlyph(modifier = Modifier.fillMaxSize(0.62f))
    }
}
