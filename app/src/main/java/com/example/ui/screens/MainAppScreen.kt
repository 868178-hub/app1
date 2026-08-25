package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RunViewModel

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MainAppScreen(
    viewModel: RunViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val territories by viewModel.territories.collectAsStateWithLifecycle()
    val sectors by viewModel.sectors.collectAsStateWithLifecycle()
    val socialPosts by viewModel.socialPosts.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val pendingConquest by viewModel.pendingConquest.collectAsStateWithLifecycle()

    val navItems = listOf(
        NavItem("Mapa en Vivo", Icons.Filled.Map, Icons.Outlined.Map, "nav_tab_map"),
        NavItem("Sectores", Icons.Filled.Public, Icons.Outlined.Public, "nav_tab_sectors"),
        NavItem("Muro Social", Icons.Filled.Groups, Icons.Outlined.Groups, "nav_tab_social"),
        NavItem("Privacidad", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_privacy")
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = DarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                navItems.forEachIndexed { index, item ->
                    val selected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00363D),
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> MapRunScreen(
                    currentLocation = currentLocation,
                    currentSession = currentSession,
                    territories = territories,
                    userProfile = userProfile,
                    isSimulating = isSimulating,
                    pendingConquest = pendingConquest,
                    onStartRun = { viewModel.startRun() },
                    onPauseRun = { viewModel.pauseRun() },
                    onResumeRun = { viewModel.resumeRun() },
                    onFinishRun = { viewModel.finishRun() },
                    onToggleSimulation = { viewModel.toggleSimulation() },
                    onForceCloseLoop = { viewModel.forceCloseLoop() },
                    onConfirmConquest = { name, isPub -> viewModel.confirmConquest(name, isPub) },
                    onDismissConquestDialog = { viewModel.dismissConquestDialog() },
                    onRequestGpsRefresh = { viewModel.refreshGpsLocation() }
                )
                1 -> SectorsLeaderboardScreen(
                    sectors = sectors,
                    leaderboard = leaderboard
                )
                2 -> SocialConquestFeedScreen(
                    posts = socialPosts,
                    onLikePost = { viewModel.toggleLikePost(it) }
                )
                3 -> PrivacyProfileScreen(
                    userProfile = userProfile,
                    friends = friends,
                    onToggleStealth = { viewModel.toggleStealthMode(it) },
                    onColorSelected = { viewModel.setSignatureColor(it) },
                    onLoginGoogle = { name, email -> viewModel.loginWithGoogle(name, email) },
                    onLoginEmail = { email, name -> viewModel.loginWithEmail(email, name) },
                    onLogout = { viewModel.logout() },
                    onAddFriendByCode = { viewModel.addFriend(it) }
                )
            }
        }
    }
}
