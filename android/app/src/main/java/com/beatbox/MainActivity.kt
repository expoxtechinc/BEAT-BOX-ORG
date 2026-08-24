package com.beatbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.beatbox.ui.BeatBoxRoot
import com.beatbox.ui.theme.BeatBoxTheme
import com.beatbox.util.ThemePreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreference: ThemePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { false }

        setContent {
            val themeMode by themePreference.themeMode.collectAsState(initial = ThemePreference.ThemeMode.SYSTEM)

            BeatBoxTheme(
                darkTheme = when (themeMode) {
                    ThemePreference.ThemeMode.LIGHT -> false
                    ThemePreference.ThemeMode.DARK -> true
                    ThemePreference.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                }
            ) {
                BeatBoxRoot()
            }
        }
    }
}
