package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.GeoPoint
import com.example.data.model.RunSession
import com.example.data.model.RunnerProfile
import com.example.data.model.Territory
import com.example.ui.components.ConquestCelebrationDialog
import com.example.ui.components.RunHudControls
import com.example.ui.components.TerritoryMapView
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MapRunScreen(
    currentLocation: GeoPoint,
    currentSession: RunSession,
    territories: List<Territory>,
    userProfile: RunnerProfile,
    isSimulating: Boolean,
    pendingConquest: Territory?,
    onStartRun: () -> Unit,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onFinishRun: () -> Unit,
    onToggleSimulation: () -> Unit,
    onForceCloseLoop: () -> Unit,
    onConfirmConquest: (String, Boolean) -> Unit,
    onDismissConquestDialog: () -> Unit,
    onRequestGpsRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            onRequestGpsRefresh()
        }
    }

    // Auto-request GPS permission on screen launch if not granted yet
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            onRequestGpsRefresh()
        }
    }

    var showPrivacyBanner by remember { mutableStateOf(false) }

    val userColor = remember(userProfile.signatureColorHex) {
        try {
            Color(android.graphics.Color.parseColor(userProfile.signatureColorHex))
        } catch (e: Exception) {
            NeonCyan
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("map_run_screen")) {
        // Fullscreen Interactive Canvas Map
        TerritoryMapView(
            currentLocation = currentLocation,
            activeTrail = currentSession.trailPoints,
            territories = territories,
            userSignatureColor = userColor,
            isStealthActive = userProfile.stealthModeEnabled,
            onRecenterClicked = onRequestGpsRefresh,
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Header (Privacy Shield & GPS Status)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp, end = 80.dp)
        ) {
            // Stealth Privacy Quick Chip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, if (userProfile.stealthModeEnabled) NeonLime.copy(alpha = 0.8f) else DarkBorder),
                modifier = Modifier
                    .clickable { showPrivacyBanner = !showPrivacyBanner }
                    .testTag("privacy_status_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (userProfile.stealthModeEnabled) Icons.Default.Security else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (userProfile.stealthModeEnabled) NeonLime else NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (userProfile.stealthModeEnabled) "MODO SIGILO ACTIVO" else "MODO PÚBLICO",
                            color = if (userProfile.stealthModeEnabled) NeonLime else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (userProfile.stealthModeEnabled) "Ubicación en vivo privada. Se publica al cerrar lazo." else "Ubicación visible",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (showPrivacyBanner) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expandable Privacy Details Card
            AnimatedVisibility(
                visible = showPrivacyBanner,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.96f)),
                    border = BorderStroke(1.dp, NeonLime.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = NeonLime, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Garantía de Seguridad & Privacidad", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Nadie puede rastrearte mientras corres. Tu estela y posición permanecen cifradas y privadas en tu dispositivo.\n" +
                                    "• Únicamente cuando completas y cierras un lazo/círculo se notifica la nueva zona conquistada en el mapa social.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Permission reminder if not granted
            if (!hasLocationPermission) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF332000),
                    border = BorderStroke(1.dp, Color(0xFFFF9F1C)),
                    modifier = Modifier.clickable {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFFF9F1C), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Activar GPS Real (Pulsa aquí)", color = Color(0xFFFFDDB3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bottom Running HUD & Action Controls
        RunHudControls(
            session = currentSession,
            isSimulating = isSimulating,
            onStartRun = {
                if (!hasLocationPermission) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                onStartRun()
            },
            onPauseRun = onPauseRun,
            onResumeRun = onResumeRun,
            onFinishRun = onFinishRun,
            onToggleSimulation = onToggleSimulation,
            onForceCloseLoop = onForceCloseLoop,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Victory Dialog when a loop/circle is closed
        pendingConquest?.let { conquest ->
            ConquestCelebrationDialog(
                territory = conquest,
                onPublish = { name, isPublic ->
                    onConfirmConquest(name, isPublic)
                },
                onDismiss = onDismissConquestDialog
            )
        }
    }
}
