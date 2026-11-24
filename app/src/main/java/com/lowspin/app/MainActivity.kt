package com.lowspin.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lowspin.app.ui.LocalAlbumArtRepository
import com.lowspin.app.ui.LowspinApp
import com.lowspin.app.ui.theme.LowspinTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val container = (application as LowspinApplication).container

        setContent {
            LowspinTheme {
                CompositionLocalProvider(
                    LocalAlbumArtRepository provides container.albumArtRepository
                ) {
                    LowspinApp(container)
                }
            }
        }
    }
}
