package com.lowspin.app.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.lowspin.app.data.db.SongEntity

const val ARTWORK_AUTHORITY = "com.lowspin.app.art"

fun SongEntity.artworkUri(): Uri = Uri.parse("content://$ARTWORK_AUTHORITY/$id")

fun SongEntity.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(artworkUri())
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .build()

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(contentUri)
        .setMediaMetadata(metadata)
        .build()
}
