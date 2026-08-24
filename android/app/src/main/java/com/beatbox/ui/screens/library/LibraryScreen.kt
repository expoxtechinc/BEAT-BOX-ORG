package com.beatbox.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatbox.data.dto.MusicDto
import com.beatbox.data.dto.PlaylistDto
import com.beatbox.ui.components.*

private val tabs = LibraryTab.entries

@Composable
fun LibraryScreen(
    onMusicClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = tabs[selectedTabIndex]

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Your Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }

        when (selectedTab) {
            LibraryTab.Favorites -> MusicTabContent(
                isLoading = state.isLoadingFavorites,
                error = state.favoritesError,
                music = state.favorites,
                onMusicClick = onMusicClick,
                onRetry = { viewModel.loadFavorites() },
                emptyTitle = "No favorites yet",
                emptyDescription = "Tap the heart on a track to save it here",
                emptyIcon = Icons.Default.FavoriteBorder,
            )

            LibraryTab.Recent -> MusicTabContent(
                isLoading = state.isLoadingRecent,
                error = state.recentError,
                music = state.recentlyPlayed,
                onMusicClick = onMusicClick,
                onRetry = { viewModel.loadRecentlyPlayed() },
                emptyTitle = "Nothing played yet",
                emptyDescription = "Music you listen to will appear here",
                emptyIcon = Icons.Default.History,
            )

            LibraryTab.Playlists -> PlaylistsTabContent(
                isLoading = state.isLoadingPlaylists,
                error = state.playlistsError,
                playlists = state.playlists,
                onPlaylistClick = onPlaylistClick,
                onRetry = { viewModel.loadPlaylists() },
            )

            LibraryTab.Uploads -> MusicTabContent(
                isLoading = state.isLoadingUploads,
                error = state.uploadsError,
                music = state.myUploads,
                onMusicClick = onMusicClick,
                onRetry = { viewModel.loadMyUploads() },
                emptyTitle = "No uploads yet",
                emptyDescription = "Tracks you upload will show up here",
                emptyIcon = Icons.Default.CloudUpload,
            )
        }
    }
}

@Composable
private fun MusicTabContent(
    isLoading: Boolean,
    error: String?,
    music: List<MusicDto>,
    onMusicClick: (String) -> Unit,
    onRetry: () -> Unit,
    emptyTitle: String,
    emptyDescription: String,
    emptyIcon: ImageVector,
) {
    when {
        isLoading -> LoadingState()
        error != null -> ErrorState(message = error, onRetry = onRetry)
        music.isEmpty() -> EmptyState(
            icon = emptyIcon,
            title = emptyTitle,
            description = emptyDescription,
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(music, key = { it.id }) { track ->
                MusicCard(
                    music = track,
                    onClick = { onMusicClick(track.id) },
                    onPlay = { onMusicClick(track.id) },
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    isLoading: Boolean,
    error: String?,
    playlists: List<PlaylistDto>,
    onPlaylistClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        isLoading -> LoadingState()
        error != null -> ErrorState(message = error, onRetry = onRetry)
        playlists.isEmpty() -> EmptyState(
            icon = Icons.Default.QueueMusic,
            title = "No playlists yet",
            description = "Create a playlist to organize your music",
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistCard(playlist = playlist, onClick = { onPlaylistClick(playlist.id) })
            }
        }
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${playlist.items?.size ?: 0} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
