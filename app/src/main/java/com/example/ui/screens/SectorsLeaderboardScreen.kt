package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaderboardRunner
import com.example.data.model.Sector
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SectorsLeaderboardScreen(
    sectors: List<Sector>,
    leaderboard: List<LeaderboardRunner>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sectores de Ciudad", "Ranking Global", "Mis Amigos")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(top = 16.dp)
            .testTag("sectors_leaderboard_screen")
    ) {
        // Screen Title
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "SECTORES & CONQUISTAS",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Territorios en Disputa",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = NeonCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonCyan,
                    height = 3.dp
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) NeonCyan else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        when (selectedTab) {
            0 -> SectorsListView(sectors = sectors)
            1 -> LeaderboardListView(runners = leaderboard)
            2 -> FriendsLeaderboardView(runners = leaderboard.filter { it.isUser || it.rank <= 4 })
        }
    }
}

@Composable
private fun SectorsListView(sectors: List<Sector>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Zonas activas donde los corredores compiten por dominar el mapa:",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(sectors) { sector ->
            val rulerColor = try {
                Color(android.graphics.Color.parseColor(sector.rulerColorHex))
            } catch (e: Exception) {
                NeonCyan
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, rulerColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sector.district.uppercase(),
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = sector.name,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Sector status pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = rulerColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, rulerColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = sector.status,
                                color = rulerColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dominator Runner info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = rulerColor,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = sector.rulerAvatarInitials,
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("LÍDER ACTUAL", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(sector.rulerName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("ÁREA TOTAL", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(sector.formattedArea, color = NeonLime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dominance Progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Control del Sector:", color = TextSecondary, fontSize = 11.sp)
                        Text("${sector.dominancePercentage}%", color = rulerColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (sector.dominancePercentage / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = rulerColor,
                        trackColor = DarkSurfaceElevated
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Footer metrics (Runners contesting & best pace)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${sector.runnerCount} corredores disputando", color = TextSecondary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Récord: ${sector.bestPace}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun LeaderboardListView(runners: List<LeaderboardRunner>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Clasificación general por territorio total conquistado (m²):",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(runners) { runner ->
            val runnerColor = try {
                Color(android.graphics.Color.parseColor(runner.colorHex))
            } catch (e: Exception) {
                NeonCyan
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (runner.isUser) DarkSurfaceElevated else DarkSurface
                ),
                border = BorderStroke(
                    width = if (runner.isUser) 1.5.dp else 1.dp,
                    color = if (runner.isUser) NeonCyan else DarkBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Medal/Badge
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (runner.rank) {
                            1 -> Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(24.dp))
                            2 -> Icon(imageVector = Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFC0C0C0), modifier = Modifier.size(22.dp))
                            3 -> Icon(imageVector = Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFCD7F32), modifier = Modifier.size(22.dp))
                            else -> Text(
                                text = "#${runner.rank}",
                                color = TextMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Avatar with Signature Color Ring
                    Surface(
                        shape = CircleShape,
                        color = runnerColor,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = runner.avatarInitials,
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Name & Level
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = runner.name,
                                color = if (runner.isUser) NeonCyan else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Nivel ${runner.level} • ${runner.sectorsControlled} sectores",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Total Territory Conquered
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = runner.formattedTerritory,
                            color = NeonLime,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${String.format("%.1f", runner.totalKm)} km",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun FriendsLeaderboardView(runners: List<LeaderboardRunner>) {
    LeaderboardListView(runners = runners)
}
