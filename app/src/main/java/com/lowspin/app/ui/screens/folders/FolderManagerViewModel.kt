package com.lowspin.app.ui.screens.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowspin.app.data.repository.MusicRepository
import com.lowspin.app.data.scanner.ScanState
import com.lowspin.app.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FolderRow(
    val folderPath: String,
    val folderName: String,
    val songCount: Int,
    val excluded: Boolean
)

class FolderManagerViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    val rows: StateFlow<List<FolderRow>> = combine(
        repository.folders,
        repository.excludedFolders
    ) { folders, excluded ->
        val excludedByPath = excluded.associateBy { it.folderPath }
        val included = folders.map { folder ->
            FolderRow(
                folderPath = folder.folderPath,
                folderName = folder.folderName,
                songCount = folder.songCount,
                excluded = false
            )
        }
        val hidden = excluded.map { folder ->
            FolderRow(
                folderPath = folder.folderPath,
                folderName = folder.folderName,
                songCount = folder.songCount,
                excluded = true
            )
        }
        included + hidden
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val scanState: StateFlow<ScanState> = repository.scanState

    val scanning: Boolean get() = scanState.value is ScanState.Scanning ||
        scanState.value is ScanState.Enriching

    fun setExcluded(row: FolderRow, excluded: Boolean) {
        viewModelScope.launch {
            repository.setFolderExcluded(
                folderPath = row.folderPath,
                folderName = row.folderName,
                songCount = row.songCount,
                excluded = excluded
            )
        }
    }

    fun refresh() {
        repository.refreshLibrary()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { FolderManagerViewModel(container.musicRepository) }
        }
    }
}
