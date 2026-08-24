package com.beatbox.ui.screens.discover

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatbox.data.dto.CategoryDto
import com.beatbox.data.dto.MusicDto
import com.beatbox.data.dto.SearchUserDto
import com.beatbox.ui.components.*

@Composable
fun DiscoverScreen(
    onMusicClick: (String) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Discover",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Find your next favorite sound",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.isLoading) {
            LoadingState()
            return@Column
        }

        if (state.error != null &&
            state.trending.isEmpty() &&
            state.newReleases.isEmpty() &&
            state.categories.isEmpty() &&
            state.popularArtists.isEmpty()
        ) {
            ErrorState(message = state.error!!, onRetry = { viewModel.loadData() })
            return@Column
        }

        // Trending Now
        if (state.trending.isNotEmpty()) {
            SectionHeader("Trending Now")
            HorizontalMusicList(state.trending, onMusicClick)
        }

        // New Releases
        if (state.newReleases.isNotEmpty()) {
            SectionHeader("New Releases")
            HorizontalMusicList(state.newReleases, onMusicClick)
        }

        // Categories
        if (state.categories.isNotEmpty()) {
            SectionHeader("Browse Categories")
            CategoryList(state.categories)
        }

        // Popular Artists
        if (state.popularArtists.isNotEmpty()) {
            SectionHeader("Popular Artists")
            ArtistList(state.popularArtists)
        }

        Spacer(Modifier.height(80.dp)) // Space for mini player
    }
}

@Composable
private fun HorizontalMusicList(
    music: List<MusicDto>,
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
private fun CategoryList(categories: List<CategoryDto>) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        categories.forEach { category ->
            CategoryChip(category = category)
        }
    }
}

@Composable
private fun CategoryChip(category: CategoryDto) {
    Surface(
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ArtistList(artists: List<SearchUserDto>) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        artists.forEach { artist ->
            ArtistItem(artist = artist)
        }
    }
}

@Composable
private fun ArtistItem(artist: SearchUserDto) {
    val profile = artist.profile
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Avatar(
            url = profile?.avatarUrl,
            size = 80.dp,
        )
        Text(
            text = profile?.displayName ?: profile?.username ?: "Artist",
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
