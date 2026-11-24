package com.lowspin.app.ui.screens.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.di.AppContainer
import com.lowspin.app.media.EqualizerPresets
import com.lowspin.app.media.PlaybackService
import com.lowspin.app.ui.settings.LocalAppearance
import com.lowspin.app.ui.settings.LowspinAccentShades
import com.lowspin.app.ui.theme.LowspinBackground
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinSurfaceHigh
import com.lowspin.app.ui.theme.LowspinTextPrimary

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appearance = LocalAppearance.current
    val equalizer = container.equalizerSettings
    val eqEnabled by equalizer.enabled.collectAsStateWithLifecycle()
    val eqPreset by equalizer.preset.collectAsStateWithLifecycle()
    var refresh by remember { mutableStateOf(0) }

    val accentIndex by appearance.accentIndex.collectAsStateWithLifecycle()
    val textScale by appearance.textScale.collectAsStateWithLifecycle()
    val compact by appearance.compact.collectAsStateWithLifecycle()
    val showArt by appearance.showArt.collectAsStateWithLifecycle()
    val reduceMotion by appearance.reduceMotion.collectAsStateWithLifecycle()
    val highContrast by appearance.highContrast.collectAsStateWithLifecycle()
    val largeTargets by appearance.largeTargets.collectAsStateWithLifecycle()

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    val notificationsEnabled = remember(refresh) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val channelState = remember(refresh) { notificationChannelState(context) }
    val batteryOptimised = remember(refresh) {
        val manager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        manager?.isIgnoringBatteryOptimizations(context.packageName) == false
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = LowspinTextPrimary
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = LowspinTextPrimary
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingCard {
                Text(
                    text = "Accent",
                    style = MaterialTheme.typography.titleSmall,
                    color = LowspinTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    LowspinAccentShades.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(color)
                                .clickable { appearance.setAccent(index) }
                                .padding(if (accentIndex == index) 4.dp else 0.dp)
                                .then(
                                    if (accentIndex == index) {
                                        Modifier.background(
                                            LowspinTextPrimary,
                                            RoundedCornerShape(18.dp)
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (accentIndex == index) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                ChoiceRow(
                    title = "Text size",
                    options = listOf("Small" to 0.9f, "Default" to 1f, "Large" to 1.15f),
                    selected = textScale,
                    onSelect = appearance::setTextScale
                )
                ToggleRow("Compact lists", compact, appearance::setCompact)
                ToggleRow("Show album art", showArt, appearance::setShowArt)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Sound",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SettingCard {
                ToggleRow("Equalizer", eqEnabled, equalizer::setEnabled)
                if (eqEnabled) {
                    Spacer(Modifier.height(4.dp))
                    Row {
                        EqualizerPresets.forEachIndexed { index, label ->
                            val active = eqPreset == index
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) com.lowspin.app.ui.theme.LowspinOnAccent
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else LowspinSurfaceHigh
                                    )
                                    .clickable { equalizer.setPreset(index) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                    Text(
                        text = "Preset support depends on your device's audio hardware.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Accessibility",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SettingCard {
                ToggleRow("Reduce motion", reduceMotion, appearance::setReduceMotion)
                ToggleRow("High contrast text", highContrast, appearance::setHighContrast)
                ToggleRow("Larger touch targets", largeTargets, appearance::setLargeTargets)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                color = LowspinTextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            DiagnosticCard(
                title = "Notifications enabled",
                value = if (notificationsEnabled) "Yes" else "No — playback controls won't appear"
            )
            DiagnosticCard(
                title = "Media channel",
                value = channelState
            )
            DiagnosticCard(
                title = "Battery optimisation",
                value = if (batteryOptimised) "On — may hide the notification" else "Off"
            )

            Spacer(Modifier.height(12.dp))

            if (!notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActionButton("Grant notification permission") {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                Spacer(Modifier.height(8.dp))
            }

            ActionButton("Open notification settings") {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
            Spacer(Modifier.height(8.dp))

            if (batteryOptimised) {
                ActionButton("Disable battery optimisation") {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = "On OxygenOS, One UI, MIUI and similar skins, also enable the " +
                    "\u201CMedia playback\u201D notification category and allow background " +
                    "activity for Lowspin. The media notification is then shown by the system.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LowspinSurfaceHigh)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = LowspinTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = com.lowspin.app.ui.theme.LowspinOnAccent,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = LowspinSurfaceHigh
            )
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    options: List<Pair<String, Float>>,
    selected: Float,
    onSelect: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = LowspinTextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Row {
            options.forEach { (label, value) ->
                val active = selected == value
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) com.lowspin.app.ui.theme.LowspinOnAccent
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else LowspinSurfaceHigh
                        )
                        .clickable { onSelect(value) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticCard(title: String, value: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LowspinSurfaceHigh)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = LowspinTextPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LowspinSurfaceHigh,
            contentColor = LowspinTextPrimary
        )
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

private fun notificationChannelState(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return "System managed"
    val channel = context.getSystemService(NotificationManager::class.java)
        ?.getNotificationChannel(PlaybackService.NOTIFICATION_CHANNEL_ID)
        ?: return "Not created yet — play a song once"
    return when {
        channel.importance == NotificationManager.IMPORTANCE_NONE -> "Disabled — enable it in notification settings"
        channel.importance >= NotificationManager.IMPORTANCE_LOW -> "Enabled"
        else -> "Low importance — controls may be hidden"
    }
}
