package com.lowspin.app.ui.screens.hidden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowspin.app.data.db.HiddenSongEntity
import com.lowspin.app.data.repository.MusicRepository
import com.lowspin.app.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HiddenSongsViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    val hiddenSongs: StateFlow<List<HiddenSongEntity>> = repository.hiddenSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun restore(song: HiddenSongEntity) {
        viewModelScope.launch { repository.unhideSong(song) }
    }

    fun restoreAll() {
        viewModelScope.launch { repository.restoreAllHidden() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HiddenSongsViewModel(container.musicRepository) }
        }
    }
}
