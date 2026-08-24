package com.beatbox.ui.screens.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatbox.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val highlight: String? = null,
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to BeatBox",
            description = "Discover, upload, and share music with the world. Join a community of creators and listeners.",
            icon = Icons.Default.GraphicEq,
            gradient = listOf(GradientStart, BrandPrimaryDark),
        ),
        OnboardingPage(
            title = "Upload Your Music",
            description = "Upload your music for free. No subscription required. Ever. Upload 1 song or 100 songs — it's always free.",
            icon = Icons.Default.CloudUpload,
            gradient = listOf(BrandPrimary, BrandAccent),
            highlight = "FREE FOREVER",
        ),
        OnboardingPage(
            title = "Discover Music",
            description = "Find new music from creators around the world. Explore trending tracks, new releases, and curated recommendations.",
            icon = Icons.Default.Explore,
            gradient = listOf(BrandAccent, BrandPrimary),
        ),
        OnboardingPage(
            title = "Premium Features",
            description = "Get 5 free premium uses for advanced features. Subscribe to BeatBox Premium anytime for unlimited access.",
            icon = Icons.Default.Star,
            gradient = listOf(BrandGold, BrandPrimary),
            highlight = "5 FREE USES",
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = {
                viewModel.completeOnboarding()
                onSkip()
            }) {
                Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) BrandPrimary
                            else MaterialTheme.colorScheme.outline
                        ),
                )
            }
        }

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Back")
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        viewModel.completeOnboarding()
                        onComplete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            ) {
                Text(
                    if (pagerState.currentPage == pages.size - 1) "Get Started" else "Continue",
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(page.gradient)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color.White,
            )
        }

        Spacer(Modifier.height(32.dp))

        // Highlight badge
        if (page.highlight != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BrandGold.copy(alpha = 0.2f),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Text(
                    text = page.highlight,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandGold,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
