package com.beatbox.ui.navigation

/**
 * All navigation routes for BeatBox.
 */
sealed class Screen(val route: String) {
    // Splash & Onboarding
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")

    // Auth
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object ResetPassword : Screen("reset_password")

    // Main (bottom nav)
    data object Home : Screen("home")
    data object Discover : Screen("discover")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Upload : Screen("upload")

    // Music detail
    data object MusicDetail : Screen("music/{musicId}") {
        fun createRoute(musicId: String) = "music/$musicId"
    }

    // Player
    data object FullPlayer : Screen("full_player")

    // Profile
    data object MyProfile : Screen("my_profile")
    data object UserProfile : Screen("profile/{username}") {
        fun createRoute(username: String) = "profile/$username"
    }
    data object EditProfile : Screen("edit_profile")

    // Playlist
    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    data object CreatePlaylist : Screen("create_playlist")

    // Music management
    data object EditMusic : Screen("edit_music/{musicId}") {
        fun createRoute(musicId: String) = "edit_music/$musicId"
    }
    data object MyUploads : Screen("my_uploads")

    // Premium & Subscription
    data object Premium : Screen("premium")
    data object Subscription : Screen("subscription")
    data object ManageSubscription : Screen("manage_subscription")

    // Settings
    data object Settings : Screen("settings")
    data object Notifications : Screen("notifications")
    data object About : Screen("about")

    // Admin
    data object AdminDashboard : Screen("admin_dashboard")
    data object AdminUsers : Screen("admin_users")
    data object AdminMusic : Screen("admin_music")
    data object AdminReports : Screen("admin_reports")
    data object AdminCategories : Screen("admin_categories")
    data object AdminSubscriptions : Screen("admin_subscriptions")
}
