package com.lowspin.app.di

import android.content.Context
import com.lowspin.app.data.art.AlbumArtRepository
import com.lowspin.app.data.db.LowspinDatabase
import com.lowspin.app.data.repository.MusicRepository
import com.lowspin.app.data.repository.PlaylistRepository
import com.lowspin.app.data.scanner.Id3Enricher
import com.lowspin.app.data.scanner.MediaStoreScanner
import com.lowspin.app.media.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container. One instance per process, owned by the
 * Application. No annotation processing beyond Room.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: LowspinDatabase = LowspinDatabase.get(appContext)

    val albumArtRepository = AlbumArtRepository(appContext, database.songDao())

    private val mediaStoreScanner = MediaStoreScanner(appContext, database.songDao())
    private val id3Enricher = Id3Enricher(appContext, database.songDao())

    val musicRepository = MusicRepository(
        database = database,
        scanner = mediaStoreScanner,
        enricher = id3Enricher,
        scope = applicationScope
    )

    val playlistRepository = PlaylistRepository(database)

    val playbackController = PlaybackController(
        context = appContext,
        scope = applicationScope,
        database = database,
        artRepository = albumArtRepository
    )
}
