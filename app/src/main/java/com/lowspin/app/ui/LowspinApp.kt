package com.lowspin.app.ui

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.di.AppContainer
import com.lowspin.app.ui.components.CreatePlaylistDialog
import com.lowspin.app.ui.components.MiniPlayer
import com.lowspin.app.ui.components.SongOptionsSheet
import com.lowspin.app.ui.screens.about.AboutScreen
import com.lowspin.app.ui.screens.folders.FolderManagerScreen
import com.lowspin.app.ui.screens.folders.FolderManagerViewModel
import com.lowspin.app.ui.screens.hidden.HiddenSongsScreen
import com.lowspin.app.ui.screens.hidden.HiddenSongsViewModel
import com.lowspin.app.ui.screens.library.LibraryScreen
import com.lowspin.app.ui.screens.library.LibraryViewModel
import com.lowspin.app.ui.screens.liked.LikedSongsScreen
import com.lowspin.app.ui.screens.liked.LikedViewModel
import com.lowspin.app.ui.screens.nowplaying.NowPlayingScreen
import com.lowspin.app.ui.screens.permission.PermissionScreen
import com.lowspin.app.ui.screens.playlist.PlaylistDetailScreen
import com.lowspin.app.ui.screens.playlist.PlaylistViewModel
import com.lowspin.app.ui.screens.search.SearchScreen
import com.lowspin.app.ui.screens.search.SearchViewModel
import com.lowspin.app.ui.theme.LowspinBackground
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.ui.theme.LowspinTextSecondary
import kotlinx.coroutines.launch

@Composable
fun LowspinApp(container: AppContainer) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val requiredPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var granted by remember { mutableStateOf(hasPermission(context, requiredPermission)) }
    var requestedOnce by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var notificationAsked by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result
        if (!result) {
            val canAskAgain = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, requiredPermission)
            } ?: false
            permanentlyDenied = requestedOnce && !canAskAgain
        } else {
            permanentlyDenied = false
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, requiredPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = hasPermission(context, requiredPermission)
                granted = nowGranted
                if (nowGranted) permanentlyDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!granted) {
        PermissionScreen(
            permanentlyDenied = permanentlyDenied,
            onGrantClick = {
                requestedOnce = true
                permissionLauncher.launch(requiredPermission)
            },
            onOpenSettings = { openAppSettings(context) }
        )
        return
    }

    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.factory(container)
    )
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentPlaylistId = backStackEntry?.arguments?.getLong("playlistId")?.takeIf { it > 0L }

    val songs by libraryViewModel.songs.collectAsStateWithLifecycle()
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val scanState by libraryViewModel.scanState.collectAsStateWithLifecycle()
    val playerState by libraryViewModel.playerState.collectAsStateWithLifecycle()

    val currentSong = playerState.currentSong
    val currentSongId = currentSong?.id
    val isLiked = remember(songs, currentSongId) {
        songs.firstOrNull { it.id == currentSongId }?.isLiked == true
    }

    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var optionsSong by remember { mutableStateOf<SongEntity?>(null) }
    var songToHide by remember { mutableStateOf<SongEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        libraryViewModel.onLibraryAccessGranted()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationAsked) {
            notificationAsked = true
            val grantedNotification = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!grantedNotification) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Stable callback identities keep screen composables skippable while the
    // player ticker publishes a new state every 500 ms.
    val onPlaySong = remember(libraryViewModel, songs) {
        { song: SongEntity ->
            val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            libraryViewModel.playQueue(songs, index, "Your library")
        }
    }
    val onPlayResults = remember(libraryViewModel) {
        { song: SongEntity, list: List<SongEntity> ->
            val index = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            libraryViewModel.playQueue(list, index, "Search results")
        }
    }
    val onOpenOptions = remember { { song: SongEntity -> optionsSong = song } }
    val onBack = remember(navController) {
        {
            navController.popBackStack()
            Unit
        }
    }
    val onOpenPlaylist = remember(navController) { { id: Long -> navController.navigate("playlist/$id") } }
    val onOpenSearch = remember(navController) { { navController.navigate("search") } }
    val onOpenLiked = remember(navController) { { navController.navigate("liked") } }
    val onOpenFolders = remember(navController) { { navController.navigate("folders") } }
    val onOpenHidden = remember(navController) { { navController.navigate("hidden") } }
    val onOpenAbout = remember(navController) { { navController.navigate("about") } }
    val onRequestCreatePlaylist = remember { { showCreatePlaylistDialog = true } }
    val onRefresh = remember(libraryViewModel) { { libraryViewModel.refreshLibrary() } }
    val onTogglePlayPause = remember(libraryViewModel) { { libraryViewModel.togglePlayPause() } }
    val onNext = remember(libraryViewModel) { { libraryViewModel.next() } }
    val onPrevious = remember(libraryViewModel) { { libraryViewModel.previous() } }
    val onSeek = remember(libraryViewModel) { { position: Long -> libraryViewModel.seekTo(position) } }
    val onToggleShuffle = remember(libraryViewModel, playerState.shuffleEnabled) {
        { libraryViewModel.setShuffle(!playerState.shuffleEnabled) }
    }
    val onCycleRepeat = remember(libraryViewModel) { { libraryViewModel.cycleRepeat() } }
    val onTogglePlayerLike = remember(libraryViewModel, currentSongId, isLiked) {
        { libraryViewModel.toggleLike(currentSongId ?: -1L, !isLiked) }
    }

    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
    BackHandler(enabled = !showNowPlaying && songToHide != null) { songToHide = null }
    BackHandler(enabled = !showNowPlaying && songToHide == null && optionsSong != null) {
        optionsSong = null
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(LowspinBackground)
    ) {
        Scaffold(
            containerColor = LowspinBackground,
            bottomBar = {
                if (currentSong != null) {
                    MiniPlayer(
                        state = playerState,
                        isLiked = isLiked,
                        onExpand = { showNowPlaying = true },
                        onTogglePlayPause = onTogglePlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleLike = onTogglePlayerLike,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 8.dp)
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "library",
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable("library") {
                    LibraryScreen(
                        songs = songs,
                        playlists = playlists,
                        likedCount = remember(songs) { songs.count { it.isLiked } },
                        scanState = scanState,
                        currentSongId = currentSongId,
                        onSongClick = onPlaySong,
                        onSongOptions = onOpenOptions,
                        onPlaylistClick = onOpenPlaylist,
                        onLikedClick = onOpenLiked,
                        onCreatePlaylist = onRequestCreatePlaylist,
                        onRefresh = onRefresh,
                        onSearchClick = onOpenSearch,
                        onManageFolders = onOpenFolders,
                        onHiddenSongs = onOpenHidden,
                        onAbout = onOpenAbout
                    )
                }
                composable("search") {
                    val searchViewModel: SearchViewModel = viewModel(
                        factory = SearchViewModel.factory(container)
                    )
                    SearchScreen(
                        viewModel = searchViewModel,
                        currentSongId = currentSongId,
                        onBack = onBack,
                        onPlaySong = onPlayResults,
                        onSongOptions = onOpenOptions,
                        onPlaylistClick = onOpenPlaylist
                    )
                }
                composable(
                    route = "playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { entry ->
                    val playlistId = entry.arguments?.getLong("playlistId") ?: return@composable
                    val playlistViewModel: PlaylistViewModel = viewModel(
                        key = "playlist-$playlistId",
                        factory = PlaylistViewModel.factory(container, playlistId)
                    )
                    PlaylistDetailScreen(
                        viewModel = playlistViewModel,
                        currentSongId = currentSongId,
                        onBack = onBack,
                        onSongOptions = onOpenOptions
                    )
                }
                composable("liked") {
                    val likedViewModel: LikedViewModel = viewModel(
                        factory = LikedViewModel.factory(container)
                    )
                    LikedSongsScreen(
                        viewModel = likedViewModel,
                        currentSongId = currentSongId,
                        onBack = onBack,
                        onSongOptions = onOpenOptions
                    )
                }
                composable("folders") {
                    val foldersViewModel: FolderManagerViewModel = viewModel(
                        factory = FolderManagerViewModel.factory(container)
                    )
                    FolderManagerScreen(viewModel = foldersViewModel, onBack = onBack)
                }
                composable("hidden") {
                    val hiddenViewModel: HiddenSongsViewModel = viewModel(
                        factory = HiddenSongsViewModel.factory(container)
                    )
                    HiddenSongsScreen(viewModel = hiddenViewModel, onBack = onBack)
                }
                composable("about") {
                    AboutScreen(onBack = onBack)
                }
            }
        }

        AnimatedVisibility(
            visible = showNowPlaying && currentSong != null,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 260, easing = EaseOut)
            ) { fullHeight -> fullHeight } + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 240, easing = EaseOut)
            ) { fullHeight -> fullHeight } + fadeOut(tween(180))
        ) {
            NowPlayingScreen(
                state = playerState,
                isLiked = isLiked,
                onDismiss = { showNowPlaying = false },
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onToggleLike = onTogglePlayerLike,
                onMoreClick = { currentSong?.let(onOpenOptions) }
            )
        }
    }

    optionsSong?.let { song ->
        SongOptionsSheet(
            song = song,
            isLiked = song.isLiked,
            playlists = playlists,
            onDismiss = { optionsSong = null },
            onToggleLike = {
                libraryViewModel.toggleLike(song.id, !song.isLiked)
                optionsSong = null
            },
            onPlayNext = {
                libraryViewModel.playNext(song)
                optionsSong = null
            },
            onAddToQueue = {
                libraryViewModel.addToQueue(song)
                optionsSong = null
            },
            onAddToPlaylist = { playlistId ->
                libraryViewModel.addToPlaylist(playlistId, song.id) { optionsSong = null }
            },
            onCreatePlaylist = { name ->
                libraryViewModel.createPlaylist(name) { playlistId ->
                    libraryViewModel.addToPlaylist(playlistId, song.id) { }
                    optionsSong = null
                }
            },
            onRemoveFromPlaylist = if (currentPlaylistId != null) {
                {
                    scope.launch {
                        container.playlistRepository.removeSong(currentPlaylistId, song.id)
                    }
                    optionsSong = null
                }
            } else null,
            onRemoveFromLibrary = {
                songToHide = song
                optionsSong = null
            }
        )
    }

    songToHide?.let { song ->
        AlertDialog(
            onDismissRequest = { songToHide = null },
            title = {
                Text(
                    text = "Remove from Lowspin?",
                    style = MaterialTheme.typography.titleLarge,
                    color = LowspinTextPrimary
                )
            },
            text = {
                Text(
                    text = "\u201C${song.title}\u201D will disappear from your library and " +
                        "future scans. The file stays on your phone and can be restored " +
                        "from Hidden songs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LowspinTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { container.musicRepository.hideSong(song) }
                        songToHide = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { songToHide = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = LowspinTextSecondary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                showCreatePlaylistDialog = false
                libraryViewModel.createPlaylist(name) { id -> navController.navigate("playlist/$id") }
            }
        )
    }
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun Context.findActivity(): ComponentActivity? {
    var current: Context = this
    while (true) {
        if (current is ComponentActivity) return current
        current = (current as? ContextWrapper)?.baseContext ?: return null
    }
}
