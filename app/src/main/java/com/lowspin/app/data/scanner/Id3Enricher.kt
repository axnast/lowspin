package com.lowspin.app.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.lowspin.app.data.db.SongDao
import com.lowspin.app.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Second scan phase. Reads embedded ID3 tags for tracks that have not been
 * enriched yet and fills in gaps MediaStore leaves behind (track/disc numbers,
 * clean titles, release years).
 */
class Id3Enricher(
    private val context: Context,
    private val songDao: SongDao
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun enrich(onProgress: (done: Int, total: Int) -> Unit) =
        withContext(Dispatchers.IO) {
            val pending = songDao.getPendingEnrichment()
            if (pending.isEmpty()) return@withContext

            val parallel = Dispatchers.IO.limitedParallelism(2)
            var done = 0

            pending.chunked(CHUNK_SIZE).forEach { chunk ->
                coroutineScope {
                    chunk.forEach { song ->
                        launch(parallel) { enrichOne(song) }
                    }
                }
                done += chunk.size
                onProgress(done, pending.size)
            }
        }

    private suspend fun enrichOne(song: SongEntity) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(song.contentUri))

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }

            var track = parseNumber(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            )
            var disc = parseNumber(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            )
            if (track >= 1000) {
                disc = track / 1000
                track %= 1000
            }

            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.take(4)
                ?.toIntOrNull()
                ?: song.year

            songDao.applyId3(
                id = song.id,
                title = title ?: song.title,
                artist = artist ?: song.artist,
                album = album ?: song.album,
                trackNumber = track.takeIf { it > 0 } ?: song.trackNumber,
                discNumber = disc.takeIf { it > 0 } ?: song.discNumber,
                year = year
            )
        } catch (_: Exception) {
            // A single unreadable file must never abort the whole scan.
            songDao.markEnrichmentAttempted(song.id)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun parseNumber(raw: String?): Int {
        val leading = raw?.trim()?.substringBefore('/')?.trim().orEmpty()
        return leading.toIntOrNull() ?: 0
    }

    private companion object {
        const val CHUNK_SIZE = 40
    }
}
