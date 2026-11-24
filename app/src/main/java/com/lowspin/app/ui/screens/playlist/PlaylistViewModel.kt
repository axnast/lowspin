package com.lowspin.app.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowspin.app.data.db.PlaylistEntity
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.repository.PlaylistRepository
import com.lowspin.app.di.AppContainer
import com.lowspin.app.media.PlaybackController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val playlistId: Long,
    private val repository: PlaylistRepository,
    private val player: PlaybackController
) : ViewModel() {

    val playlist: StateFlow<PlaylistEntity?> = repository.observePlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<List<SongEntity>> = repository.observeSongs(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artSongIds: StateFlow<List<Long>> = repository.observeArtSongIds(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun play(startIndex: Int) {
        val list = songs.value
        if (list.isEmpty()) return
        player.setShuffle(false)
        player.playQueue(
            songs = list,
            startIndex = startIndex.coerceIn(0, list.lastIndex),
            sourceLabel = playlist.value?.name ?: "Playlist"
        )
    }

    fun shufflePlay() {
        val list = songs.value
        if (list.isEmpty()) return
        player.setShuffle(true)
        player.playQueue(
            songs = list,
            startIndex = list.indices.random(),
            sourceLabel = playlist.value?.name ?: "Playlist"
        )
    }

    fun reorder(orderedSongs: List<SongEntity>) {
        viewModelScope.launch {
            repository.reorder(playlistId, orderedSongs.map { it.id })
        }
    }

    fun rename(name: String) {
        viewModelScope.launch { repository.renamePlaylist(playlistId, name) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            onDeleted()
        }
    }

    companion object {
        fun factory(container: AppContainer, playlistId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    PlaylistViewModel(
                        playlistId = playlistId,
                        repository = container.playlistRepository,
                        player = container.playbackController
                    )
                }
            }
    }
}
