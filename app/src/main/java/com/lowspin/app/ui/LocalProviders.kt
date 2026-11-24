package com.lowspin.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.lowspin.app.data.art.AlbumArtRepository

val LocalAlbumArtRepository = staticCompositionLocalOf<AlbumArtRepository> {
    error("AlbumArtRepository has not been provided")
}
