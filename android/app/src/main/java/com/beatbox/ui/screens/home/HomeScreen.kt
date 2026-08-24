package com.beatbox.ui.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatbox.ui.components.*

@Composable
fun HomeScreen(
    onMusicClick: (String) -> Unit,
    onSeeAll: (String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header with greeting
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Good day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "BeatBox",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            }
        }

        if (state.isLoading) {
            LoadingState()
            return@Column
        }

        if (state.error != null && state.trending.isEmpty()) {
            ErrorState(message = state.error!!, onRetry = { viewModel.loadData() })
            return@Column
        }

        // Featured
        if (state.featured.isNotEmpty()) {
            SectionHeader("Featured", onSeeAll = { onSeeAll("featured") })
            HorizontalMusicList(state.featured, onMusicClick)
        }

        // Recently Played
        if (state.recentlyPlayed.isNotEmpty()) {
            SectionHeader("Recently Played", onSeeAll = { onSeeAll("recent") })
            HorizontalMusicList(state.recentlyPlayed, onMusicClick)
        }

        // Trending
        if (state.trending.isNotEmpty()) {
            SectionHeader("Trending Now", onSeeAll = { onSeeAll("trending") })
            VerticalMusicList(state.trending, onMusicClick)
        }

        // New Releases
        if (state.newReleases.isNotEmpty()) {
            SectionHeader("New Releases", onSeeAll = { onSeeAll("new") })
            HorizontalMusicList(state.newReleases, onMusicClick)
        }

        // Recommended
        if (state.recommended.isNotEmpty()) {
            SectionHeader("Recommended for You", onSeeAll = { onSeeAll("recommended") })
            VerticalMusicList(state.recommended, onMusicClick)
        }

        Spacer(Modifier.height(80.dp)) // Space for mini player
    }
}

@Composable
private fun HorizontalMusicList(
    music: List<com.beatbox.data.dto.MusicDto>,
    onMusicClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        music.forEach { track ->
            MusicGridItem(
                music = track,
                onClick = { onMusicClick(track.id) },
                onPlay = { onMusicClick(track.id) },
            )
        }
    }
}

@Composable
private fun VerticalMusicList(
    music: List<com.beatbox.data.dto.MusicDto>,
    onMusicClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        music.forEach { track ->
            MusicCard(
                music = track,
                onClick = { onMusicClick(track.id) },
                onPlay = { onMusicClick(track.id) },
            )
        }
    }
}
