package com.beatbox.data.repository

import com.beatbox.data.api.BeatBoxApi
import com.beatbox.data.dto.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class MusicRepository @Inject constructor(
    private val api: BeatBoxApi,
) {
    // CRITICAL: Music upload is FREE. This method does NOT call any premium endpoint.
    suspend fun uploadMusic(
        audioFile: File,
        title: String,
        artistName: String,
        albumName: String? = null,
        genre: String? = null,
        description: String? = null,
        categoryId: String? = null,
        artworkFile: File? = null,
    ): ApiResult<MusicDto> {
        val audioRequestBody = audioFile.asRequestBody("audio/*".toMediaType())
        val audioPart = MultipartBody.Part.createFormData("audio", audioFile.name, audioRequestBody)

        val titlePart = title.toRequestBody("text/plain".toMediaType())
        val artistPart = artistName.toRequestBody("text/plain".toMediaType())
        val albumPart = albumName?.toRequestBody("text/plain".toMediaType())
        val genrePart = genre?.toRequestBody("text/plain".toMediaType())
        val descPart = description?.toRequestBody("text/plain".toMediaType())
        val categoryPart = categoryId?.toRequestBody("text/plain".toMediaType())

        val artworkPart = artworkFile?.let {
            val artworkRequestBody = it.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("artwork", it.name, artworkRequestBody)
        }

        return safeApiCall {
            api.uploadMusic(
                audio = audioPart,
                title = titlePart,
                artistName = artistPart,
                albumName = albumPart,
                genre = genrePart,
                description = descPart,
                categoryId = categoryPart,
                artwork = artworkPart,
            )
        }
    }

    suspend fun updateMusic(
        musicId: String,
        title: String? = null,
        artistName: String? = null,
        albumName: String? = null,
        genre: String? = null,
        description: String? = null,
        categoryId: String? = null,
        isPublished: Boolean? = null,
        artworkFile: File? = null,
    ): ApiResult<MusicDto> {
        val titlePart = title?.toRequestBody("text/plain".toMediaType())
        val artistPart = artistName?.toRequestBody("text/plain".toMediaType())
        val albumPart = albumName?.toRequestBody("text/plain".toMediaType())
        val genrePart = genre?.toRequestBody("text/plain".toMediaType())
        val descPart = description?.toRequestBody("text/plain".toMediaType())
        val categoryPart = categoryId?.toRequestBody("text/plain".toMediaType())
        val publishedPart = isPublished?.toString()?.toRequestBody("text/plain".toMediaType())

        val artworkPart = artworkFile?.let {
            val artworkRequestBody = it.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("artwork", it.name, artworkRequestBody)
        }

        return safeApiCall {
            api.updateMusic(
                id = musicId,
                title = titlePart,
                artistName = artistPart,
                albumName = albumPart,
                genre = genrePart,
                description = descPart,
                categoryId = categoryPart,
                isPublished = publishedPart,
                artwork = artworkPart,
            )
        }
    }

    suspend fun deleteMusic(musicId: String): ApiResult<Any> {
        return safeApiCall { api.deleteMusic(musicId) }
    }

    suspend fun getMusic(musicId: String): ApiResult<MusicDto> {
        return safeApiCall { api.getMusic(musicId) }
    }

    suspend fun getMyUploads(page: Int = 1, limit: Int = 20): ApiResult<List<MusicDto>> {
        return safeApiCallList { api.getMyUploads(page, limit) }
    }

    suspend fun getUserMusic(userId: String, page: Int = 1, limit: Int = 20): ApiResult<List<MusicDto>> {
        return safeApiCallList { api.getUserMusic(userId, page, limit) }
    }

    suspend fun recordPlay(musicId: String): ApiResult<Any> {
        return safeApiCall { api.recordPlay(musicId) }
    }

    suspend fun togglePublish(musicId: String, isPublished: Boolean): ApiResult<MusicDto> {
        return safeApiCall { api.togglePublish(musicId, mapOf("isPublished" to isPublished)) }
    }

    suspend fun searchMusic(query: String, page: Int = 1, limit: Int = 20, genre: String? = null, sort: String? = null): ApiResult<List<MusicDto>> {
        return safeApiCallList { api.searchMusic(query, page, limit, genre, null, sort) }
    }

    suspend fun getTrending(limit: Int = 20): ApiResult<List<MusicDto>> {
        return safeApiCallList { api.getTrending(limit) }
    }

    suspend fun getNewReleases(limit: Int = 20): ApiResult<List<MusicDto>> {
        return safeApiCallList { api.getNewReleases(limit) }
    }

    suspend fun getFeatured(limit: Int = 20): ApiResult<List<MusicDto>> {
        return safeApiCallList { api.getFeatured(limit) }
    }

    suspend fun getRecommended(limit: Int = 20): ApiResult<List<MusicDto>> {
        return safeApiCallList { api.getRecommended(limit) }
    }

    suspend fun getRecentlyPlayed(limit: Int = 20): ApiResult<List<MusicDto>> {
        return safeApiCallList { api.getRecentlyPlayed(limit) }
    }
}
