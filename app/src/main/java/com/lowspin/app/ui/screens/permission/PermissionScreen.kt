package com.lowspin.app.ui.screens.permission

import com.lowspin.app.ui.theme.LowspinSurfaceHigh
import com.lowspin.app.ui.theme.LowspinAccentPressed
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lowspin.app.ui.theme.LowspinAccent
import com.lowspin.app.ui.theme.LowspinBackground
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.ui.theme.LowspinTextSecondary

@Composable
fun PermissionScreen(
    permanentlyDenied: Boolean,
    onGrantClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        com.lowspin.app.ui.theme.LowspinAccentPressed.copy(alpha = 0.25f),
                        LowspinBackground,
                        LowspinBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(com.lowspin.app.ui.theme.LowspinSurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = LowspinOnAccent,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Your music.\nYour phone.\nNo internet.",
                style = MaterialTheme.typography.displaySmall,
                color = LowspinTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Lowspin plays the audio files already stored on this device. " +
                    "It needs permission to read your music library. Nothing is uploaded, " +
                    "downloaded or tracked.",
                style = MaterialTheme.typography.bodyMedium,
                color = LowspinTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = if (permanentlyDenied) onOpenSettings else onGrantClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.lowspin.app.ui.theme.LowspinSurfaceHigh,
                    contentColor = LowspinTextPrimary
                )
            ) {
                Text(
                    text = if (permanentlyDenied) "Open app settings" else "Allow access to music",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (permanentlyDenied) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Enable \u201CMusic and audio\u201D in Settings, then come back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LowspinTextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "No account. No ads. No network permission.",
                style = MaterialTheme.typography.labelSmall,
                color = LowspinTextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
