package com.lowspin.app.ui.screens.hidden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.ui.components.pressScaleClickable
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary

@Composable
fun HiddenSongsScreen(
    viewModel: HiddenSongsViewModel,
    onBack: () -> Unit
) {
    val hidden by viewModel.hiddenSongs.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = LowspinTextPrimary
                )
            }
            Text(
                text = "Hidden songs",
                style = MaterialTheme.typography.titleLarge,
                color = LowspinTextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (hidden.isNotEmpty()) {
                Text(
                    text = "Restore all",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .pressScaleClickable(onClick = viewModel::restoreAll)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Text(
            text = "These songs stay on your phone. Restoring one brings it back into your library on the next scan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (hidden.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.HideSource,
                    contentDescription = null,
                    tint = LowspinTextMuted,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Nothing hidden",
                    style = MaterialTheme.typography.titleMedium,
                    color = LowspinTextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Use a song's menu and choose \u201CRemove from Lowspin\u201D to hide it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                items(items = hidden, key = { it.songId }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = LowspinTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.restore(song) }) {
                            Icon(
                                imageVector = Icons.Rounded.Restore,
                                contentDescription = "Restore",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
