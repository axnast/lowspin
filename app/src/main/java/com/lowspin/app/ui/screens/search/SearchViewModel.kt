package com.lowspin.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowspin.app.data.db.PlaylistSummary
import com.lowspin.app.data.db.RecentSearchEntity
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.repository.MusicRepository
import com.lowspin.app.data.repository.PlaylistRepository
import com.lowspin.app.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResults(
    val query: String = "",
    val songs: List<SongEntity> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList()
) {
    val isEmpty: Boolean get() = songs.isEmpty() && playlists.isEmpty()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<SearchResults> = _query
        .debounce(180)
        .flatMapLatest { raw ->
            val clean = raw.trim()
            if (clean.isEmpty()) {
                flowOf(SearchResults())
            } else {
                combine(
                    musicRepository.searchSongs(clean),
                    playlistRepository.search(clean)
                ) { songs, playlists ->
                    SearchResults(query = clean, songs = songs, playlists = playlists)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchResults()
        )

    val recentSearches: StateFlow<List<RecentSearchEntity>> =
        musicRepository.recentSearches.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun commitSearch(value: String = _query.value) {
        val clean = value.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { musicRepository.saveRecentSearch(clean) }
    }

    fun removeRecentSearch(value: String) {
        viewModelScope.launch { musicRepository.removeRecentSearch(value) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { musicRepository.clearRecentSearches() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SearchViewModel(
                    musicRepository = container.musicRepository,
                    playlistRepository = container.playlistRepository
                )
            }
        }
    }
}
