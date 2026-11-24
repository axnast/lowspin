package com.lowspin.app.ui.screens.liked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.repository.MusicRepository
import com.lowspin.app.di.AppContainer
import com.lowspin.app.media.PlaybackController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LikedViewModel(
    repository: MusicRepository,
    private val player: PlaybackController
) : ViewModel() {

    val songs: StateFlow<List<SongEntity>> = repository.likedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun play(startIndex: Int) {
        val list = songs.value
        if (list.isEmpty()) return
        player.setShuffle(false)
        player.playQueue(list, startIndex.coerceIn(0, list.lastIndex), "Liked Songs")
    }

    fun shufflePlay() {
        val list = songs.value
        if (list.isEmpty()) return
        player.setShuffle(true)
        player.playQueue(list, list.indices.random(), "Liked Songs")
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LikedViewModel(
                    repository = container.musicRepository,
                    player = container.playbackController
                )
            }
        }
    }
}
