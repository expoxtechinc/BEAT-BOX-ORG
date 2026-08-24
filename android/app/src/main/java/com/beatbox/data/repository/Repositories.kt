package com.beatbox.data.repository

import com.beatbox.data.api.BeatBoxApi
import com.beatbox.data.dto.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

// =============================================================================
// SOCIAL REPOSITORY
// =============================================================================

class SocialRepository @Inject constructor(
    private val api: BeatBoxApi,
) {
    suspend fun getMyProfile(): ApiResult<Any> = safeApiCall { api.getMyProfile() }

    suspend fun updateProfile(
        displayName: String?, bio: String?, location: String?, website: String?,
        isPublic: Boolean?, avatarFile: File?,
    ): ApiResult<ProfileDto> {
        val displayPart = displayName?.toRequestBody("text/plain".toMediaType())
        val bioPart = bio?.toRequestBody("text/plain".toMediaType())
        val locationPart = location?.toRequestBody("text/plain".toMediaType())
        val websitePart = website?.toRequestBody("text/plain".toMediaType())
        val isPublicPart = isPublic?.toString()?.toRequestBody("text/plain".toMediaType())
        val avatarPart = avatarFile?.let {
            MultipartBody.Part.createFormData("avatar", it.name, it.asRequestBody("image/*".toMediaType()))
        }
        return safeApiCall {
            api.updateProfile(displayPart, bioPart, locationPart, websitePart, isPublicPart, avatarPart)
        }
    }

    suspend fun getPublicProfile(username: String): ApiResult<PublicProfileDto> = safeApiCall { api.getPublicProfile(username) }

    suspend fun follow(userId: String): ApiResult<Any> = safeApiCall { api.follow(userId) }
    suspend fun unfollow(userId: String): ApiResult<Any> = safeApiCall { api.unfollow(userId) }

    suspend fun toggleLike(musicId: String): ApiResult<Map<String, Boolean>> = safeApiCall { api.toggleLike(musicId) }
    suspend fun getLikedMusic(page: Int = 1, limit: Int = 20): ApiResult<List<MusicDto>> = safeApiCallList { api.getLikedMusic(page, limit) }

    suspend fun toggleFavorite(musicId: String): ApiResult<Map<String, Boolean>> = safeApiCall { api.toggleFavorite(musicId) }
    suspend fun getFavoriteMusic(page: Int = 1, limit: Int = 20): ApiResult<List<MusicDto>> = safeApiCallList { api.getFavoriteMusic(page, limit) }

    // Playlists
    suspend fun createPlaylist(name: String, description: String?, isPublic: Boolean): ApiResult<PlaylistDto> =
        safeApiCall { api.createPlaylist(CreatePlaylistRequest(name, description, isPublic)) }

    suspend fun getMyPlaylists(page: Int = 1, limit: Int = 20): ApiResult<List<PlaylistDto>> = safeApiCallList { api.getMyPlaylists(page, limit) }
    suspend fun getPlaylist(id: String): ApiResult<PlaylistDto> = safeApiCall { api.getPlaylist(id) }
    suspend fun updatePlaylist(id: String, request: UpdatePlaylistRequest): ApiResult<PlaylistDto> = safeApiCall { api.updatePlaylist(id, request) }
    suspend fun deletePlaylist(id: String): ApiResult<Any> = safeApiCall { api.deletePlaylist(id) }
    suspend fun addMusicToPlaylist(playlistId: String, musicId: String): ApiResult<Any> = safeApiCall { api.addMusicToPlaylist(playlistId, musicId) }
    suspend fun removeMusicFromPlaylist(playlistId: String, musicId: String): ApiResult<Any> = safeApiCall { api.removeMusicFromPlaylist(playlistId, musicId) }
    suspend fun reorderPlaylist(playlistId: String, musicIds: List<String>): ApiResult<Any> = safeApiCall { api.reorderPlaylist(playlistId, ReorderPlaylistRequest(musicIds)) }

    // Notifications
    suspend fun getNotifications(page: Int = 1, limit: Int = 20): ApiResult<NotificationListDto> = safeApiCall { api.getNotifications(page, limit) }
    suspend fun markNotificationRead(id: String): ApiResult<NotificationDto> = safeApiCall { api.markNotificationRead(id) }
    suspend fun markAllNotificationsRead(): ApiResult<Any> = safeApiCall { api.markAllNotificationsRead() }

    // Categories & Artists
    suspend fun getCategories(): ApiResult<List<CategoryDto>> = safeApiCallList { api.getCategories() }
    suspend fun getPopularArtists(limit: Int = 20): ApiResult<List<SearchUserDto>> = safeApiCallList { api.getPopularArtists(limit) }
}

// =============================================================================
// PREMIUM REPOSITORY
// =============================================================================

class PremiumRepository @Inject constructor(
    private val api: BeatBoxApi,
) {
    suspend fun getPremiumStatus(): ApiResult<PremiumStatusDto> = safeApiCall { api.getPremiumStatus() }
    suspend fun getPremiumFeatures(): ApiResult<List<PremiumFeatureDto>> = safeApiCallList { api.getPremiumFeatures() }
    suspend fun getPremiumUsageHistory(page: Int = 1, limit: Int = 20): ApiResult<List<PremiumUsageDto>> = safeApiCallList { api.getPremiumUsageHistory(page, limit) }

    /**
     * Use a premium feature. This consumes one of the 5 free uses.
     * Returns PREMIUM_REQUIRED error if user has 0 uses and is not subscribed.
     *
     * CRITICAL: Music upload does NOT call this method.
     */
    suspend fun usePremiumFeature(featureKey: String): ApiResult<PremiumUseResult> =
        safeApiCall { api.usePremiumFeature(UsePremiumRequest(featureKey)) }

    suspend fun canUsePremium(): ApiResult<CanUsePremiumResult> = safeApiCall { api.canUsePremium() }
}

// =============================================================================
// STRIPE REPOSITORY
// =============================================================================

class StripeRepository @Inject constructor(
    private val api: BeatBoxApi,
) {
    suspend fun getConfig(): ApiResult<StripeConfigDto> = safeApiCall { api.getStripeConfig() }
    suspend fun createCheckout(plan: String, couponCode: String? = null, successUrl: String? = null, cancelUrl: String? = null): ApiResult<CheckoutResponse> =
        safeApiCall { api.createCheckout(CheckoutRequest(plan, couponCode, successUrl, cancelUrl)) }
    suspend fun createPortal(returnUrl: String? = null): ApiResult<PortalResponse> =
        safeApiCall { api.createPortal(PortalRequest(returnUrl)) }
}

// =============================================================================
// SEARCH REPOSITORY
// =============================================================================

class SearchRepository @Inject constructor(
    private val api: BeatBoxApi,
) {
    suspend fun search(query: String, type: String? = null, page: Int = 1, limit: Int = 20): ApiResult<SearchResultsDto> =
        safeApiCall { api.search(query, type, page, limit) }
    suspend fun saveSearchHistory(query: String): ApiResult<Any> = safeApiCall { api.saveSearchHistory(mapOf("query" to query)) }
    suspend fun getSearchHistory(): ApiResult<List<SearchHistoryDto>> = safeApiCallList { api.getSearchHistory() }
    suspend fun clearSearchHistory(): ApiResult<Any> = safeApiCall { api.clearSearchHistory() }
}

// =============================================================================
// REPORT REPOSITORY
// =============================================================================

class ReportRepository @Inject constructor(
    private val api: BeatBoxApi,
) {
    suspend fun reportMusic(musicId: String, reason: String, description: String?): ApiResult<ReportDto> =
        safeApiCall { api.reportMusic(musicId, ReportRequest(reason, description)) }
    suspend fun reportUser(userId: String, reason: String, description: String?): ApiResult<ReportDto> =
        safeApiCall { api.reportUser(userId, ReportRequest(reason, description)) }
    suspend fun blockUser(userId: String): ApiResult<Any> = safeApiCall { api.blockUser(userId) }
    suspend fun unblockUser(userId: String): ApiResult<Any> = safeApiCall { api.unblockUser(userId) }
    suspend fun getBlockedUsers(): ApiResult<List<SearchUserDto>> = safeApiCallList { api.getBlockedUsers() }
}

// =============================================================================
// ADMIN REPOSITORY
// =============================================================================

class AdminRepository @Inject constructor(
    private val api: BeatBoxApi,
) {
    suspend fun getStats(): ApiResult<AdminStatsDto> = safeApiCall { api.getAdminStats() }
    suspend fun getUsers(page: Int = 1, limit: Int = 20, search: String? = null): ApiResult<List<AdminUserDto>> = safeApiCallList { api.getAdminUsers(page, limit, search) }
    suspend fun getUser(id: String): ApiResult<AdminUserDto> = safeApiCall { api.getAdminUser(id) }
    suspend fun updateUserRole(id: String, role: String): ApiResult<Any> = safeApiCall { api.updateUserRole(id, mapOf("role" to role)) }
    suspend fun banUser(id: String): ApiResult<Any> = safeApiCall { api.banUser(id) }
    suspend fun unbanUser(id: String): ApiResult<Any> = safeApiCall { api.unbanUser(id) }
    suspend fun getMusic(page: Int = 1, limit: Int = 20): ApiResult<List<MusicDto>> = safeApiCallList { api.getAdminMusic(page, limit) }
    suspend fun toggleFeatureMusic(id: String, isFeatured: Boolean): ApiResult<MusicDto> = safeApiCall { api.toggleFeatureMusic(id, mapOf("isFeatured" to isFeatured)) }
    suspend fun removeMusic(id: String): ApiResult<Any> = safeApiCall { api.removeMusic(id) }
    suspend fun restoreMusic(id: String): ApiResult<Any> = safeApiCall { api.restoreMusic(id) }
    suspend fun getCategories(): ApiResult<List<CategoryDto>> = safeApiCallList { api.getAdminCategories() }
    suspend fun createCategory(body: Map<String, Any?>): ApiResult<CategoryDto> = safeApiCall { api.createCategory(body) }
    suspend fun updateCategory(id: String, body: Map<String, Any?>): ApiResult<CategoryDto> = safeApiCall { api.updateCategory(id, body) }
    suspend fun deleteCategory(id: String): ApiResult<Any> = safeApiCall { api.deleteCategory(id) }
    suspend fun getPremiumFeatures(): ApiResult<List<PremiumFeatureDto>> = safeApiCallList { api.getAdminPremiumFeatures() }
    suspend fun createPremiumFeature(body: Map<String, Any?>): ApiResult<PremiumFeatureDto> = safeApiCall { api.createPremiumFeature(body) }
    suspend fun updatePremiumFeature(id: String, body: Map<String, Any?>): ApiResult<PremiumFeatureDto> = safeApiCall { api.updatePremiumFeature(id, body) }
    suspend fun deletePremiumFeature(id: String): ApiResult<Any> = safeApiCall { api.deletePremiumFeature(id) }
    suspend fun getReports(page: Int = 1, limit: Int = 20, status: String? = null): ApiResult<List<ReportDto>> = safeApiCallList { api.getAdminReports(page, limit, status) }
    suspend fun updateReport(id: String, body: Map<String, Any?>): ApiResult<ReportDto> = safeApiCall { api.updateReport(id, body) }
    suspend fun getSubscriptions(page: Int = 1, limit: Int = 20, status: String? = null): ApiResult<List<SubscriptionDto>> = safeApiCallList { api.getAdminSubscriptions(page, limit, status) }
    suspend fun getPayments(page: Int = 1, limit: Int = 20): ApiResult<List<Any>> = safeApiCallList { api.getAdminPayments(page, limit) }
    suspend fun getConfig(): ApiResult<List<AppConfigDto>> = safeApiCallList { api.getAdminConfig() }
    suspend fun updateConfig(key: String, body: Map<String, Any?>): ApiResult<AppConfigDto> = safeApiCall { api.updateConfig(key, body) }
    suspend fun resetPremiumUses(userId: String): ApiResult<Any> = safeApiCall { api.resetPremiumUses(mapOf("userId" to userId)) }
}
