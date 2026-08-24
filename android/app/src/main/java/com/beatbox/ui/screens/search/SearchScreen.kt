package com.beatbox.ui.screens.search

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatbox.data.dto.PlaylistDto
import com.beatbox.data.dto.SearchHistoryDto
import com.beatbox.data.dto.SearchUserDto
import com.beatbox.ui.components.*

@Composable
fun SearchScreen(
    onMusicClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Search field
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = {
                Text("Search music, artists, playlists…")
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
        )

        when {
            // Empty query → show search history
            state.query.isBlank() -> {
                if (state.history.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = "Search BeatBox",
                        description = "Find music, artists, and playlists",
                    )
                } else {
                    SearchHistorySection(
                        history = state.history,
                        onItemClick = { viewModel.selectHistoryItem(it) },
                        onClear = { viewModel.clearHistory() },
                    )
                }
            }

            // Loading
            state.isLoading -> {
                LoadingState(message = "Searching…")
            }

            // Error
            state.error != null -> {
                ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.search(state.query) },
                )
            }

            // Results
            state.results != null -> {
                val results = state.results!!
                val isEmpty = results.music.isEmpty() &&
                    results.users.isEmpty() &&
                    results.playlists.isEmpty()

                if (isEmpty) {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No results found",
                        description = "Try a different search term",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (results.music.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Songs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                            items(results.music, key = { it.id }) { track ->
                                MusicCard(
                                    music = track,
                                    onClick = { onMusicClick(track.id) },
                                    onPlay = { onMusicClick(track.id) },
                                )
                            }
                        }

                        if (results.users.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Artists",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                )
                            }
                            items(results.users, key = { it.id }) { user ->
                                UserResultRow(user = user)
                            }
                        }

                        if (results.playlists.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Playlists",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                )
                            }
                            items(results.playlists, key = { it.id }) { playlist ->
                                PlaylistResultRow(playlist = playlist)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistorySection(
    history: List<SearchHistoryDto>,
    onItemClick: (String) -> Unit,
    onClear: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent Searches",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onClear) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }
        items(history, key = { it.id }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item.query) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.query,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.NorthWest,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun UserResultRow(user: SearchUserDto) {
    val profile = user.profile
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(url = profile?.avatarUrl, size = 48.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = profile?.displayName ?: profile?.username ?: "Artist",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val followers = user._count?.followers
            val musicCount = user._count?.music
            val sub = buildString {
                if (followers != null) append("$followers followers")
                if (musicCount != null) {
                    if (isNotEmpty()) append(" • ")
                    append("$musicCount tracks")
                }
            }
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlaylistResultRow(playlist: PlaylistDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp),
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
