package com.beatbox.data.api

import com.beatbox.data.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface BeatBoxApi {

    // =========================================================================
    // AUTH
    // =========================================================================

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequest): Response<ApiResponse<Any>>

    @POST("auth/logout-all")
    suspend fun logoutAll(): Response<ApiResponse<Any>>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<UserDto>>

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<ApiResponse<Any>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse<Any>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiResponse<Any>>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Any>>

    @DELETE("auth/account")
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<ApiResponse<Any>>

    // =========================================================================
    // MUSIC - Upload is FREE (does NOT consume premium uses)
    // =========================================================================

    @Multipart
    @POST("music/upload")
    suspend fun uploadMusic(
        @Part audio: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("artistName") artistName: RequestBody,
        @Part("albumName") albumName: RequestBody?,
        @Part("genre") genre: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part("categoryId") categoryId: RequestBody?,
        @Part artwork: MultipartBody.Part?,
    ): Response<ApiResponse<MusicDto>>

    @Multipart
    @PUT("music/{id}")
    suspend fun updateMusic(
        @Path("id") id: String,
        @Part("title") title: RequestBody?,
        @Part("artistName") artistName: RequestBody?,
        @Part("albumName") albumName: RequestBody?,
        @Part("genre") genre: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part("categoryId") categoryId: RequestBody?,
        @Part("isPublished") isPublished: RequestBody?,
        @Part artwork: MultipartBody.Part?,
    ): Response<ApiResponse<MusicDto>>

    @DELETE("music/{id}")
    suspend fun deleteMusic(@Path("id") id: String): Response<ApiResponse<Any>>

    @GET("music/{id}")
    suspend fun getMusic(@Path("id") id: String): Response<ApiResponse<MusicDto>>

    @GET("music/my/uploads")
    suspend fun getMyUploads(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<List<MusicDto>>>

    @GET("music/user/{userId}")
    suspend fun getUserMusic(
        @Path("userId") userId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<List<MusicDto>>>

    @POST("music/{id}/play")
    suspend fun recordPlay(@Path("id") id: String): Response<ApiResponse<Any>>

    @PATCH("music/{id}/publish")
    suspend fun togglePublish(
        @Path("id") id: String,
        @Body body: Map<String, Boolean>,
    ): Response<ApiResponse<MusicDto>>

    @GET("music")
    suspend fun searchMusic(
        @Query("q") query: String = "",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("genre") genre: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("sort") sort: String? = null,
    ): Response<ApiResponse<List<MusicDto>>>

    @GET("music/discover/trending")
    suspend fun getTrending(@Query("limit") limit: Int = 20): Response<ApiResponse<List<MusicDto>>>

    @GET("music/discover/new")
    suspend fun getNewReleases(@Query("limit") limit: Int = 20): Response<ApiResponse<List<MusicDto>>>

    @GET("music/discover/featured")
    suspend fun getFeatured(@Query("limit") limit: Int = 20): Response<ApiResponse<List<MusicDto>>>

    @GET("music/discover/recommended")
    suspend fun getRecommended(@Query("limit") limit: Int = 20): Response<ApiResponse<List<MusicDto>>>

    @GET("music/discover/recent")
    suspend fun getRecentlyPlayed(@Query("limit") limit: Int = 20): Response<ApiResponse<List<MusicDto>>>

    // =========================================================================
    // SOCIAL - Profiles
    // =========================================================================

    @GET("social/me")
    suspend fun getMyProfile(): Response<ApiResponse<Any>>

    @Multipart
    @PUT("social/me")
    suspend fun updateProfile(
        @Part("displayName") displayName: RequestBody?,
        @Part("bio") bio: RequestBody?,
        @Part("location") location: RequestBody?,
        @Part("website") website: RequestBody?,
        @Part("isPublic") isPublic: RequestBody?,
        @Part avatar: MultipartBody.Part?,
    ): Response<ApiResponse<ProfileDto>>

    @GET("social/u/{username}")
    suspend fun getPublicProfile(@Path("username") username: String): Response<ApiResponse<PublicProfileDto>>

    // Follows
    @POST("social/follow/{userId}")
    suspend fun follow(@Path("userId") userId: String): Response<ApiResponse<Any>>

    @DELETE("social/follow/{userId}")
    suspend fun unfollow(@Path("userId") userId: String): Response<ApiResponse<Any>>

    @GET("social/{userId}/followers")
    suspend fun getFollowers(
        @Path("userId") userId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<List<SearchUserDto>>>

    @GET("social/{userId}/following")
    suspend fun getFollowing(
        @Path("userId") userId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<List<SearchUserDto>>>

    // Likes
    @POST("social/like/{musicId}")
    suspend fun toggleLike(@Path("musicId") musicId: String): Response<ApiResponse<Map<String, Boolean>>>

    @GET("social/likes/music")
    suspend fun getLikedMusic(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<List<MusicDto>>>

    // Favorites
    @POST("social/favorite/{musicId}")
    suspend fun toggleFavorite(@Path("musicId") musicId: String): Response<ApiResponse<Map<String, Boolean>>>

    @GET("social/favorites/music")
    suspend fun getFavoriteMusic(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<List<MusicDto>>>

    // Playlists
    @POST("social/playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistRequest): Response<ApiResponse<PlaylistDto>>

    @GET("social/playlists/my")
    suspend fun getMyPlaylists(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<List<PlaylistDto>>>

    @GET("social/playlists/{id}")
    suspend fun getPlaylist(@Path("id") id: String): Response<ApiResponse<PlaylistDto>>

    @PUT("social/playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") id: String,
        @Body request: UpdatePlaylistRequest,
    ): Response<ApiResponse<PlaylistDto>>

    @DELETE("social/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: String): Response<ApiResponse<Any>>

    @POST("social/playlists/{id}/music/{musicId}")
    suspend fun addMusicToPlaylist(
        @Path("id") playlistId: String,
        @Path("musicId") musicId: String,
    ): Response<ApiResponse<Any>>

    @DELETE("social/playlists/{id}/music/{musicId}")
    suspend fun removeMusicFromPlaylist(
        @Path("id") playlistId: String,
        @Path("musicId") musicId: String,
    ): Response<ApiResponse<Any>>

    @PUT("social/playlists/{id}/reorder")
    suspend fun reorderPlaylist(
        @Path("id") playlistId: String,
        @Body request: ReorderPlaylistRequest,
    ): Response<ApiResponse<Any>>

    // Notifications
    @GET("social/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<NotificationListDto>>

    @PATCH("social/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<ApiResponse<NotificationDto>>

    @POST("social/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<Any>>

    // Categories & Artists
    @GET("social/categories")
    suspend fun getCategories(): Response<ApiResponse<List<CategoryDto>>>

    @GET("social/artists/popular")
    suspend fun getPopularArtists(@Query("limit") limit: Int = 20): Response<ApiResponse<List<SearchUserDto>>>

    // =========================================================================
    // PREMIUM
    // =========================================================================

    @GET("premium/status")
    suspend fun getPremiumStatus(): Response<ApiResponse<PremiumStatusDto>>

    @GET("premium/features")
    suspend fun getPremiumFeatures(): Response<ApiResponse<List<PremiumFeatureDto>>>

    @GET("premium/usage-history")
    suspend fun getPremiumUsageHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<List<PremiumUsageDto>>>

    @POST("premium/use")
    suspend fun usePremiumFeature(@Body request: UsePremiumRequest): Response<ApiResponse<PremiumUseResult>>

    @GET("premium/can-use")
    suspend fun canUsePremium(): Response<ApiResponse<CanUsePremiumResult>>

    // =========================================================================
    // STRIPE
    // =========================================================================

    @GET("stripe/config")
    suspend fun getStripeConfig(): Response<ApiResponse<StripeConfigDto>>

    @POST("stripe/checkout")
    suspend fun createCheckout(@Body request: CheckoutRequest): Response<ApiResponse<CheckoutResponse>>

    @POST("stripe/portal")
    suspend fun createPortal(@Body request: PortalRequest): Response<ApiResponse<PortalResponse>>

    // =========================================================================
    // SEARCH
    // =========================================================================

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<SearchResultsDto>>

    @POST("search/history")
    suspend fun saveSearchHistory(@Body body: Map<String, String>): Response<ApiResponse<Any>>

    @GET("search/history")
    suspend fun getSearchHistory(): Response<ApiResponse<List<SearchHistoryDto>>>

    @DELETE("search/history")
    suspend fun clearSearchHistory(): Response<ApiResponse<Any>>

    // =========================================================================
    // REPORTS
    // =========================================================================

    @POST("reports/music/{musicId}")
    suspend fun reportMusic(
        @Path("musicId") musicId: String,
        @Body request: ReportRequest,
    ): Response<ApiResponse<ReportDto>>

    @POST("reports/user/{userId}")
    suspend fun reportUser(
        @Path("userId") userId: String,
        @Body request: ReportRequest,
    ): Response<ApiResponse<ReportDto>>

    @POST("reports/block/{userId}")
    suspend fun blockUser(@Path("userId") userId: String): Response<ApiResponse<Any>>

    @DELETE("reports/block/{userId}")
    suspend fun unblockUser(@Path("userId") userId: String): Response<ApiResponse<Any>>

    @GET("reports/blocked")
    suspend fun getBlockedUsers(): Response<ApiResponse<List<SearchUserDto>>>

    // =========================================================================
    // ADMIN
    // =========================================================================

    @GET("admin/stats")
    suspend fun getAdminStats(): Response<ApiResponse<AdminStatsDto>>

    @GET("admin/users")
    suspend fun getAdminUsers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
    ): Response<ApiResponse<List<AdminUserDto>>>

    @GET("admin/users/{id}")
    suspend fun getAdminUser(@Path("id") id: String): Response<ApiResponse<AdminUserDto>>

    @PATCH("admin/users/{id}/role")
    suspend fun updateUserRole(
        @Path("id") id: String,
        @Body body: Map<String, String>,
    ): Response<ApiResponse<Any>>

    @PATCH("admin/users/{id}/ban")
    suspend fun banUser(@Path("id") id: String): Response<ApiResponse<Any>>

    @PATCH("admin/users/{id}/unban")
    suspend fun unbanUser(@Path("id") id: String): Response<ApiResponse<Any>>

    @GET("admin/music")
    suspend fun getAdminMusic(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<List<MusicDto>>>

    @PATCH("admin/music/{id}/feature")
    suspend fun toggleFeatureMusic(
        @Path("id") id: String,
        @Body body: Map<String, Boolean>,
    ): Response<ApiResponse<MusicDto>>

    @DELETE("admin/music/{id}")
    suspend fun removeMusic(@Path("id") id: String): Response<ApiResponse<Any>>

    @PATCH("admin/music/{id}/restore")
    suspend fun restoreMusic(@Path("id") id: String): Response<ApiResponse<Any>>

    @GET("admin/categories")
    suspend fun getAdminCategories(): Response<ApiResponse<List<CategoryDto>>>

    @POST("admin/categories")
    suspend fun createCategory(@Body body: Map<String, Any?>): Response<ApiResponse<CategoryDto>>

    @PUT("admin/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body body: Map<String, Any?>,
    ): Response<ApiResponse<CategoryDto>>

    @DELETE("admin/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<ApiResponse<Any>>

    @GET("admin/premium-features")
    suspend fun getAdminPremiumFeatures(): Response<ApiResponse<List<PremiumFeatureDto>>>

    @POST("admin/premium-features")
    suspend fun createPremiumFeature(@Body body: Map<String, Any?>): Response<ApiResponse<PremiumFeatureDto>>

    @PUT("admin/premium-features/{id}")
    suspend fun updatePremiumFeature(
        @Path("id") id: String,
        @Body body: Map<String, Any?>,
    ): Response<ApiResponse<PremiumFeatureDto>>

    @DELETE("admin/premium-features/{id}")
    suspend fun deletePremiumFeature(@Path("id") id: String): Response<ApiResponse<Any>>

    @GET("admin/reports")
    suspend fun getAdminReports(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null,
    ): Response<ApiResponse<List<ReportDto>>>

    @PATCH("admin/reports/{id}")
    suspend fun updateReport(
        @Path("id") id: String,
        @Body body: Map<String, Any?>,
    ): Response<ApiResponse<ReportDto>>

    @GET("admin/subscriptions")
    suspend fun getAdminSubscriptions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null,
    ): Response<ApiResponse<List<SubscriptionDto>>>

    @GET("admin/payments")
    suspend fun getAdminPayments(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ApiResponse<List<Any>>>

    @GET("admin/config")
    suspend fun getAdminConfig(): Response<ApiResponse<List<AppConfigDto>>>

    @PUT("admin/config/{key}")
    suspend fun updateConfig(
        @Path("key") key: String,
        @Body body: Map<String, Any?>,
    ): Response<ApiResponse<AppConfigDto>>

    @POST("premium/reset")
    suspend fun resetPremiumUses(@Body body: Map<String, String>): Response<ApiResponse<Any>>
}
