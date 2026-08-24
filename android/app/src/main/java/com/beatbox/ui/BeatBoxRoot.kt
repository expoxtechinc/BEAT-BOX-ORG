package com.beatbox.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.beatbox.R
import com.beatbox.ui.navigation.Screen
import com.beatbox.ui.screens.auth.*
import com.beatbox.ui.screens.discover.DiscoverScreen
import com.beatbox.ui.screens.home.HomeScreen
import com.beatbox.ui.screens.library.LibraryScreen
import com.beatbox.ui.screens.onboarding.OnboardingScreen
import com.beatbox.ui.screens.player.MiniPlayer
import com.beatbox.ui.screens.search.SearchScreen
import com.beatbox.ui.screens.splash.SplashDestination
import com.beatbox.ui.screens.splash.SplashScreen
import com.beatbox.ui.screens.upload.UploadScreen
import com.beatbox.ui.theme.BeatBoxTheme

data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, R.string.nav_home, Icons.Default.Home, Icons.Filled.Home),
    BottomNavItem(Screen.Discover, R.string.nav_discover, Icons.Default.Explore, Icons.Filled.Explore),
    BottomNavItem(Screen.Upload, R.string.nav_upload, Icons.Default.CloudUpload, Icons.Filled.CloudUpload),
    BottomNavItem(Screen.Search, R.string.nav_search, Icons.Default.Search, Icons.Filled.Search),
    BottomNavItem(Screen.Library, R.string.nav_library, Icons.Default.LibraryMusic, Icons.Filled.LibraryMusic),
)

@Composable
fun BeatBoxRoot() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Check if we should show bottom bar
    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.icon,
                                    contentDescription = stringResource(item.labelRes),
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(item.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            // Splash
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigate = { destination ->
                        when (destination) {
                            SplashDestination.ONBOARDING -> navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                            SplashDestination.LOGIN -> navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                            SplashDestination.HOME -> navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                            SplashDestination.LOADING -> {}
                        }
                    }
                )
            }

            // Onboarding
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }

            // Auth
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onRegisterClick = { navController.navigate(Screen.Register.route) },
                    onForgotPasswordClick = { navController.navigate(Screen.ForgotPassword.route) },
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onLoginClick = { navController.popBackStack() },
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                    onResetTokenReceived = { token ->
                        navController.navigate("reset_password") {
                            // Pass token via nav argument or saved state handle
                        }
                    },
                )
            }

            // Main screens
            composable(Screen.Home.route) {
                HomeScreen(
                    onMusicClick = { musicId -> navController.navigate(Screen.MusicDetail.createRoute(musicId)) },
                    onSeeAll = { /* Navigate to full list */ },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                )
            }

            composable(Screen.Discover.route) {
                DiscoverScreen(
                    onMusicClick = { musicId -> navController.navigate(Screen.MusicDetail.createRoute(musicId)) },
                )
            }

            composable(Screen.Upload.route) {
                UploadScreen(
                    onUploadSuccess = { navController.navigate(Screen.MyUploads.route) },
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onMusicClick = { musicId -> navController.navigate(Screen.MusicDetail.createRoute(musicId)) },
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    onMusicClick = { musicId -> navController.navigate(Screen.MusicDetail.createRoute(musicId)) },
                    onPlaylistClick = { playlistId -> navController.navigate(Screen.PlaylistDetail.createRoute(playlistId)) },
                )
            }
        }
    }
}
