package com.lowspin.app.data.lyrics

import android.util.LruCache
import com.lowspin.app.data.db.SongEntity

private data class Cached(val result: Lyrics?)

/** Memoises parsed lyrics per song/version so a song is parsed only once. */
class LyricsRepository(
    private val source: LyricsSource
) {
    private val cache = object : LruCache<Long, Cached>(64) {}

    suspend fun load(song: SongEntity): Lyrics? {
        cache.get(song.id)?.let { return it.result }
        val result = source.load(song)
        cache.put(song.id, Cached(result))
        return result
    }
}
