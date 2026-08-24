package com.beatbox.data.dto

import com.google.gson.annotations.SerializedName

// =============================================================================
// API Response Wrappers
// =============================================================================

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: PageMeta? = null,
)

data class ApiError(
    val code: String,
    val message: String,
    val details: Any? = null,
)

data class PageMeta(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int? = null,
    val hasMore: Boolean? = null,
)

// =============================================================================
// Auth DTOs
// =============================================================================

data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val displayName: String? = null,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class AuthResponse(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
)

data class RefreshRequest(
    val refreshToken: String,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

data class DeleteAccountRequest(
    val password: String,
)

data class ResetPasswordRequest(
    val token: String,
    val password: String,
)

data class ForgotPasswordRequest(
    val email: String,
)

data class VerifyEmailRequest(
    val token: String,
)

// =============================================================================
// User & Profile DTOs
// =============================================================================

data class UserDto(
    val id: String,
    val email: String,
    val role: String,
    val emailVerified: Boolean,
    val premiumFreeUses: Int,
    val profile: ProfileDto? = null,
    val subscription: SubscriptionDto? = null,
    val stats: UserStatsDto? = null,
    val createdAt: String? = null,
)

data class ProfileDto(
    val id: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val location: String? = null,
    val website: String? = null,
    val isPublic: Boolean = true,
)

data class UserStatsDto(
    val uploads: Int,
    val followers: Int,
    val following: Int,
)

data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val website: String? = null,
    val isPublic: Boolean? = null,
)

data class PublicProfileDto(
    val id: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val isPublic: Boolean,
    val isFollowing: Boolean = false,
    val stats: UserStatsDto,
)

// =============================================================================
// Music DTOs
// =============================================================================

data class MusicDto(
    val id: String,
    val userId: String,
    val title: String,
    val artistName: String,
    val albumName: String? = null,
    val genre: String? = null,
    val description: String? = null,
    val audioUrl: String,
    val artworkUrl: String? = null,
    val duration: Int? = null,
    val fileSize: Int? = null,
    val format: String? = null,
    val isPublished: Boolean = true,
    val isFeatured: Boolean = false,
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val favoriteCount: Int = 0,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val category: CategoryDto? = null,
    val user: MusicUserDto? = null,
    val createdAt: String,
    val updatedAt: String,
)

data class MusicUserDto(
    val id: String,
    val profile: ProfileSummaryDto? = null,
)

data class ProfileSummaryDto(
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
)

data class CategoryDto(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
)

// =============================================================================
// Playlist DTOs
// =============================================================================

data class PlaylistDto(
    val id: String,
    val userId: String,
    val name: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val isPublic: Boolean = true,
    val playCount: Int = 0,
    val items: List<PlaylistItemDto>? = null,
    val user: MusicUserDto? = null,
    val createdAt: String,
    val updatedAt: String,
)

data class PlaylistItemDto(
    val id: String,
    val playlistId: String,
    val musicId: String,
    val sortOrder: Int,
    val addedAt: String,
    val music: MusicDto,
)

data class CreatePlaylistRequest(
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = true,
)

data class UpdatePlaylistRequest(
    val name: String? = null,
    val description: String? = null,
    val isPublic: Boolean? = null,
    val coverUrl: String? = null,
)

data class ReorderPlaylistRequest(
    val musicIds: List<String>,
)

// =============================================================================
// Subscription & Premium DTOs
// =============================================================================

data class SubscriptionDto(
    val id: String,
    val userId: String,
    val status: String, // FREE, ACTIVE, PAST_DUE, CANCELED, EXPIRED
    val plan: String,   // FREE, MONTHLY, YEARLY
    val stripeSubscriptionId: String? = null,
    val currentPeriodStart: String? = null,
    val currentPeriodEnd: String? = null,
    val cancelAtPeriodEnd: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
)

data class PremiumStatusDto(
    val premiumFreeUses: Int,
    val isSubscribed: Boolean,
    val subscription: SubscriptionDto? = null,
    val canUsePremium: Boolean,
    val canUploadMusic: Boolean, // Always true - upload is free
)

data class PremiumFeatureDto(
    val id: String,
    val key: String,
    val name: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val isActive: Boolean,
    val sortOrder: Int,
)

data class UsePremiumRequest(
    val featureKey: String,
)

data class PremiumUseResult(
    val consumed: Boolean,
    val remainingUses: Int,
    val isSubscribed: Boolean,
    val requiresSubscription: Boolean,
)

data class CanUsePremiumResult(
    val canUse: Boolean,
    val reason: String? = null,
    val remainingUses: Int,
    val isSubscribed: Boolean,
)

data class PremiumUsageDto(
    val id: String,
    val userId: String,
    val featureKey: String,
    val usedAt: String,
)

// =============================================================================
// Stripe DTOs
// =============================================================================

data class StripeConfigDto(
    val enabled: Boolean,
    val publishableKey: String?,
    val plans: StripePlansDto,
)

data class StripePlansDto(
    val monthly: StripePlanDto,
    val yearly: StripePlanDto,
)

data class StripePlanDto(
    val priceId: String?,
    val name: String,
    val interval: String,
)

data class CheckoutRequest(
    val plan: String, // MONTHLY or YEARLY
    val couponCode: String? = null,
    val successUrl: String? = null,
    val cancelUrl: String? = null,
)

data class CheckoutResponse(
    val sessionId: String,
    val url: String,
)

data class PortalRequest(
    val returnUrl: String? = null,
)

data class PortalResponse(
    val url: String,
)

// =============================================================================
// Notification DTOs
// =============================================================================

data class NotificationDto(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val data: String? = null,
    val isRead: Boolean,
    val createdAt: String,
)

data class NotificationListDto(
    val notifications: List<NotificationDto>,
    val total: Int,
    val unreadCount: Int,
)

// =============================================================================
// Search DTOs
// =============================================================================

data class SearchResultsDto(
    val music: List<MusicDto> = emptyList(),
    val users: List<SearchUserDto> = emptyList(),
    val playlists: List<PlaylistDto> = emptyList(),
)

data class SearchUserDto(
    val id: String,
    val profile: ProfileSummaryDto? = null,
    val _count: SearchUserCount? = null,
)

data class SearchUserCount(
    val followers: Int,
    val music: Int,
)

data class SearchHistoryDto(
    val id: String,
    val query: String,
    val createdAt: String,
)

// =============================================================================
// Report DTOs
// =============================================================================

data class ReportRequest(
    val reason: String,
    val description: String? = null,
)

data class ReportDto(
    val id: String,
    val reporterId: String,
    val reportedUserId: String?,
    val reportedMusicId: String?,
    val reason: String,
    val description: String?,
    val status: String,
    val createdAt: String,
)

// =============================================================================
// Admin DTOs
// =============================================================================

data class AdminStatsDto(
    val totalUsers: Int,
    val totalMusic: Int,
    val totalSubscribers: Int,
    val totalRevenue: Int,
    val totalPlays: Int,
    val pendingReports: Int,
    val newUsersToday: Int,
    val newMusicToday: Int,
)

data class AdminUserDto(
    val id: String,
    val email: String,
    val role: String,
    val emailVerified: Boolean,
    val premiumFreeUses: Int,
    val isDeleted: Boolean,
    val createdAt: String,
    val profile: ProfileDto? = null,
    val subscription: SubscriptionDto? = null,
    val _count: AdminUserCount? = null,
)

data class AdminUserCount(
    val music: Int,
    val followers: Int,
    val following: Int,
    val likes: Int,
    val favorites: Int,
)

data class AppConfigDto(
    val id: String,
    val key: String,
    val value: String,
    val description: String?,
    val isPublic: Boolean,
)
