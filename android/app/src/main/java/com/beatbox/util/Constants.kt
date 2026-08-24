package com.beatbox.util

object Constants {
    // API configuration
    const val API_PREFIX = "api/v1/"

    // Preferences
    const val PREFS_NAME = "beatbox_prefs"
    const val ENCRYPTED_PREFS_NAME = "beatbox_encrypted_prefs"

    // Keys
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    const val KEY_THEME_MODE = "theme_mode"

    // Upload limits
    const val MAX_AUDIO_FILE_SIZE_MB = 100L
    const val MAX_IMAGE_FILE_SIZE_MB = 10L

    // Pagination
    const val DEFAULT_PAGE_SIZE = 20

    // Premium
    const val PREMIUM_FREE_USES = 5

    // Notification channels
    const val CHANNEL_PLAYBACK = "playback"
    const val CHANNEL_NOTIFICATIONS = "notifications"
}
