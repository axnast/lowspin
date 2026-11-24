package com.lowspin.app.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.lowspin.app.data.db.SongEntity

const val ARTWORK_AUTHORITY = "com.lowspin.app.art"

fun SongEntity.artworkUri(): Uri = Uri.parse("content://$ARTWORK_AUTHORITY/$id")

/** MediaStore MIME types are often generic (audio/mpeg); derive from the file name. */
private fun mimeTypeFor(song: SongEntity): String? {
    val name = song.matchKey.substringAfterLast('/').ifBlank { song.title }
    return when {
        name.endsWith(".flac") -> "audio/flac"
        name.endsWith(".wav") || name.endsWith(".wave") -> "audio/wav"
        name.endsWith(".aif") || name.endsWith(".aiff") -> "audio/aiff"
        name.endsWith(".m4a") -> "audio/mp4"
        name.endsWith(".ogg") -> "audio/ogg"
        name.endsWith(".opus") -> "audio/opus"
        else -> song.mimeType.takeIf { it.startsWith("audio/") }
    }
}

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
        .setMimeType(mimeTypeFor(this))
        .setMediaMetadata(metadata)
        .build()
}
