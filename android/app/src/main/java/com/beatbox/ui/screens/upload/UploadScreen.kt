package com.beatbox.ui.screens.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.beatbox.ui.components.ErrorState
import com.beatbox.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@Composable
fun UploadScreen(
    onUploadSuccess: () -> Unit,
    viewModel: UploadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.success) {
        if (state.success) {
            onUploadSuccess()
            viewModel.reset()
        }
    }

    // File pickers
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast("/") ?: "audio_file"
            viewModel.setAudioFile(it, fileName)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast("/") ?: "artwork"
            viewModel.setArtworkFile(it, fileName)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Title
        Text(
            text = "Upload Music",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        // FREE notice - prominently displayed
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BrandSuccess.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = BrandSuccess,
                    modifier = Modifier.size(32.dp),
                )
                Column {
                    Text(
                        text = "Upload is FREE",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandSuccess,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Music upload is always free. No subscription required. Uploads do not count toward your 5 free premium uses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandSuccess,
                    )
                }
            }
        }

        // Audio file selection
        OutlinedButton(
            onClick = { audioPicker.launch("audio/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.AudioFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(state.audioFileName ?: "Select Audio File (MP3, WAV, M4A, AAC)")
        }

        // Artwork selection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { imagePicker.launch("image/*") },
            contentAlignment = Alignment.Center,
        ) {
            if (state.artworkUri != null) {
                AsyncImage(
                    model = state.artworkUri,
                    contentDescription = "Artwork preview",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Select Cover Artwork (Optional)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Form fields
        OutlinedTextField(
            value = state.title,
            onValueChange = { viewModel.updateField("title", it) },
            label = { Text("Title *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.artistName,
            onValueChange = { viewModel.updateField("artistName", it) },
            label = { Text("Artist Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.albumName,
            onValueChange = { viewModel.updateField("albumName", it) },
            label = { Text("Album (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.genre,
            onValueChange = { viewModel.updateField("genre", it) },
            label = { Text("Genre (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = { viewModel.updateField("description", it) },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
        )

        // Error message
        if (state.error != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // Upload button
        Button(
            onClick = {
                // Copy files from Uri to temp files
                val audioFile = state.audioUri?.let { uri ->
                    copyUriToFile(context, uri, "temp_audio_${System.currentTimeMillis()}")
                }
                val artworkFile = state.artworkUri?.let { uri ->
                    copyUriToFile(context, uri, "temp_artwork_${System.currentTimeMillis()}")
                }

                if (audioFile != null) {
                    viewModel.uploadMusic(
                        audioFile = audioFile,
                        artworkFile = artworkFile,
                        title = state.title.trim(),
                        artistName = state.artistName.trim(),
                        albumName = state.albumName.ifBlank { null },
                        genre = state.genre.ifBlank { null },
                        description = state.description.ifBlank { null },
                        categoryId = state.categoryId,
                    )
                }
            },
            enabled = !state.isLoading && viewModel.isValid(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Upload Music (Free)", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

private fun copyUriToFile(context: android.content.Context, uri: Uri, prefix: String): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile(prefix, ".tmp", context.cacheDir)
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        tempFile
    } catch (e: Exception) {
        null
    }
}
