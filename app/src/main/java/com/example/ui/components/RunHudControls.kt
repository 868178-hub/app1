package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RunSession
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RunHudControls(
    session: RunSession,
    isSimulating: Boolean,
    onStartRun: () -> Unit,
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onFinishRun: () -> Unit,
    onToggleSimulation: () -> Unit,
    onForceCloseLoop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("run_hud_controls"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, DarkBorder),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Metrics Dashboard Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric 1: Distance
                MetricItem(
                    label = "DISTANCIA",
                    value = session.formattedDistance,
                    icon = Icons.Default.DirectionsRun,
                    tint = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                // Metric 2: Pace
                MetricItem(
                    label = "RITMO",
                    value = "${session.formattedPace}/km",
                    icon = Icons.Default.Speed,
                    tint = NeonAmber,
                    modifier = Modifier.weight(1f)
                )

                // Metric 3: Time
                MetricItem(
                    label = "TIEMPO",
                    value = session.formattedDuration,
                    icon = Icons.Default.Timer,
                    tint = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Metric 4: Conquered Territory Area
                MetricItem(
                    label = "TERRITORIO",
                    value = session.formattedTotalArea,
                    icon = Icons.Default.Public,
                    tint = NeonLime,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            if (!session.isRunning) {
                // Start Run Main CTA Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onStartRun,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_start_run"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF00363D)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SALIR A CORRER",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Quick Simulator Button for instant testing
                    OutlinedButton(
                        onClick = onToggleSimulation,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("btn_toggle_simulation"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isSimulating) NeonCoral else NeonAmber),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isSimulating) NeonCoral else NeonAmber
                        )
                    ) {
                        Icon(
                            imageVector = if (isSimulating) Icons.Default.Stop else Icons.Default.ElectricBolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSimulating) "Parar Test" else "Simular Bucle",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                // In-run Controls (Pause/Resume, Finish, Force Loop)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause / Resume
                    if (session.isPaused) {
                        Button(
                            onClick = onResumeRun,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_resume_run"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reanudar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPauseRun,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_pause_run"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, NeonAmber),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonAmber)
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pausar", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Force Close Loop button (helpful for testing instant polygon closure)
                    FilledTonalButton(
                        onClick = onForceCloseLoop,
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("btn_force_loop"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = NeonCyan
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cerrar Bucle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Finish Run
                    Button(
                        onClick = onFinishRun,
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("btn_finish_run"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terminar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
