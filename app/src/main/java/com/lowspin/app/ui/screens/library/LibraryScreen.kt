package com.lowspin.app.ui.screens.library

import com.lowspin.app.ui.theme.LowspinSurfaceHigh
import com.lowspin.app.ui.theme.LowspinOnAccent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lowspin.app.data.db.PlaylistSummary
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.scanner.ScanState
import com.lowspin.app.ui.components.LikedSongsCard
import com.lowspin.app.ui.components.NewPlaylistCard
import com.lowspin.app.ui.components.PlaylistCard
import com.lowspin.app.ui.components.SectionHeader
import com.lowspin.app.ui.components.SongRow
import com.lowspin.app.ui.components.VinylGlyph
import com.lowspin.app.ui.components.pressScaleClickable
import com.lowspin.app.ui.theme.LowspinAccent
import com.lowspin.app.ui.theme.LowspinSurfaceElevated
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.ui.theme.LowspinTextSecondary

@Composable
fun LibraryScreen(
    songs: List<SongEntity>,
    playlists: List<PlaylistSummary>,
    likedCount: Int,
    scanState: ScanState,
    currentSongId: Long?,
    onSongClick: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onLikedClick: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onSearchClick: () -> Unit,
    onManageFolders: () -> Unit,
    onHiddenSongs: () -> Unit,
    onAbout: () -> Unit
) {
    val scanning = scanState is ScanState.Scanning || scanState is ScanState.Enriching
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    val filteredCount = (scanState as? ScanState.Complete)?.filteredCount ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LibraryHeader(
            songCount = songs.size,
            onSearchClick = onSearchClick,
            menuOpen = menuOpen,
            onMenuOpenChange = { menuOpen = it },
            scanning = scanning,
            onRefresh = onRefresh,
            onManageFolders = onManageFolders,
            onHiddenSongs = onHiddenSongs,
            onAbout = onAbout
        )

        ScanBanner(scanState)

        if (filteredCount > 0) {
            FilteredBanner(count = filteredCount, onManage = onManageFolders)
        }

        LibraryTabs(selected = tab, onSelect = { tab = it })

        Box(Modifier.weight(1f)) {
            if (tab == 0) {
                SongsTab(
                    songs = songs,
                    scanState = scanState,
                    currentSongId = currentSongId,
                    onSongClick = onSongClick,
                    onSongOptions = onSongOptions,
                    onRefresh = onRefresh
                )
            } else {
                PlaylistsTab(
                    playlists = playlists,
                    likedCount = likedCount,
                    onPlaylistClick = onPlaylistClick,
                    onLikedClick = onLikedClick,
                    onCreatePlaylist = onCreatePlaylist
                )
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    songCount: Int,
    onSearchClick: () -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    scanning: Boolean,
    onRefresh: () -> Unit,
    onManageFolders: () -> Unit,
    onHiddenSongs: () -> Unit,
    onAbout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium,
                color = LowspinTextPrimary
            )
            Text(
                text = if (songCount == 0) "Local music" else "$songCount songs",
                style = MaterialTheme.typography.bodySmall,
                color = LowspinTextSecondary
            )
        }
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = LowspinTextPrimary
            )
        }
        Box {
            IconButton(onClick = { onMenuOpenChange(true) }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    tint = LowspinTextSecondary
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { onMenuOpenChange(false) },
                containerColor = LowspinSurfaceHigh.copy(alpha = 0.94f)
            ) {
                DropdownMenuItem(
                    text = { Text("Rescan library", color = LowspinTextPrimary) },
                    enabled = !scanning,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = if (scanning) LowspinTextMuted else LowspinTextSecondary
                        )
                    },
                    onClick = {
                        onMenuOpenChange(false)
                        onRefresh()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Manage folders", color = LowspinTextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.FolderOff,
                            contentDescription = null,
                            tint = LowspinTextSecondary
                        )
                    },
                    onClick = {
                        onMenuOpenChange(false)
                        onManageFolders()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Hidden songs", color = LowspinTextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.HideSource,
                            contentDescription = null,
                            tint = LowspinTextSecondary
                        )
                    },
                    onClick = {
                        onMenuOpenChange(false)
                        onHiddenSongs()
                    }
                )
                DropdownMenuItem(
                    text = { Text("About Lowspin", color = LowspinTextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = LowspinTextSecondary
                        )
                    },
                    onClick = {
                        onMenuOpenChange(false)
                        onAbout()
                    }
                )
            }
        }
    }
}

@Composable
private fun LibraryTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabPill(label = "Songs", selected = selected == 0, onClick = { onSelect(0) })
        TabPill(label = "Playlists", selected = selected == 1, onClick = { onSelect(1) })
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) LowspinAccent else LowspinSurfaceElevated,
        animationSpec = tween(180),
        label = "tabBackground"
    )
    val content by animateColorAsState(
        targetValue = if (selected) LowspinOnAccent else LowspinTextPrimary,
        animationSpec = tween(180),
        label = "tabContent"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content
        )
    }
}

@Composable
private fun SongsTab(
    songs: List<SongEntity>,
    scanState: ScanState,
    currentSongId: Long?,
    onSongClick: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    onRefresh: () -> Unit
) {
    when {
        songs.isEmpty() && scanState !is ScanState.Complete && scanState !is ScanState.Failed ->
            LoadingState()

        songs.isEmpty() -> EmptyLibrary(onRefresh)

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
        ) {
            items(
                items = songs,
                key = { it.id },
                contentType = { "song" }
            ) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    onClick = { onSongClick(song) },
                    onMoreClick = { onSongOptions(song) },
                    onLongClick = { onSongOptions(song) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<PlaylistSummary>,
    likedCount: Int,
    onPlaylistClick: (Long) -> Unit,
    onLikedClick: () -> Unit,
    onCreatePlaylist: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item(key = "liked", contentType = "liked") {
            LikedSongsCard(
                songCount = likedCount,
                onClick = onLikedClick,
                modifier = Modifier.animateItem()
            )
        }
        item(key = "header", contentType = "header") {
            SectionHeader(title = "Your playlists")
        }
        if (playlists.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                Text(
                    text = "Create a playlist to collect the songs you love.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LowspinTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(
            items = playlists,
            key = { it.id },
            contentType = { "playlist" }
        ) { playlist ->
            PlaylistCard(
                name = playlist.name,
                songCount = playlist.songCount,
                artSongId = playlist.artSongId,
                seed = playlist.name,
                onClick = { onPlaylistClick(playlist.id) },
                modifier = Modifier.animateItem()
            )
        }
        item(key = "new", contentType = "new") {
            NewPlaylistCard(
                onClick = onCreatePlaylist,
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun FilteredBanner(count: Int, onManage: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.FolderOff,
            contentDescription = null,
            tint = LowspinTextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (count == 1) {
                "1 recording hidden"
            } else {
                "$count recordings hidden"
            },
            style = MaterialTheme.typography.labelSmall,
            color = LowspinTextMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Manage",
            style = MaterialTheme.typography.labelSmall,
            color = LowspinTextSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .pressScaleClickable(onClick = onManage)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ScanBanner(scanState: ScanState) {
    when (scanState) {
        is ScanState.Scanning -> {
            val fraction = if (scanState.total > 0) {
                scanState.scanned.toFloat() / scanState.total.toFloat()
            } else {
                0f
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = LowspinTextSecondary,
                    trackColor = LowspinSurfaceElevated
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (scanState.total > 0) {
                        "Scanning library\u2026 ${scanState.scanned} of ${scanState.total}"
                    } else {
                        "Scanning library\u2026"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = LowspinTextSecondary
                )
            }
        }

        is ScanState.Enriching -> {
            val fraction = if (scanState.total > 0) {
                scanState.done.toFloat() / scanState.total.toFloat()
            } else {
                0f
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = LowspinTextSecondary,
                    trackColor = LowspinSurfaceElevated
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Reading tags\u2026 ${scanState.done} of ${scanState.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LowspinTextSecondary
                )
            }
        }

        is ScanState.Failed -> {
            Text(
                text = "Scan failed: ${scanState.message}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        else -> Unit
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VinylGlyph(modifier = Modifier.size(48.dp), rotating = true)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Looking for music on this device\u2026",
            style = MaterialTheme.typography.bodyMedium,
            color = LowspinTextSecondary
        )
    }
}

@Composable
private fun EmptyLibrary(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = LowspinTextMuted,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.headlineSmall,
            color = LowspinTextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Copy audio files to your phone, then rescan. Lowspin only reads local files.",
            style = MaterialTheme.typography.bodyMedium,
            color = LowspinTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRefresh,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LowspinSurfaceHigh.copy(alpha = 0.94f),
                contentColor = LowspinTextPrimary
            )
        ) {
            Text("Rescan library", style = MaterialTheme.typography.titleSmall)
        }
    }
}
