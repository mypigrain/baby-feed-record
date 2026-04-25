package com.example.baby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.baby.ui.navigation.AppNavigation
import com.example.baby.ui.theme.BabyFeedingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BabyFeedingTheme {
                SetStatusBarColor()
                AppNavigation()
            }
        }
    }
}

@Composable
private fun SetStatusBarColor() {
    val view = LocalView.current
    val color = MaterialTheme.colorScheme.primaryContainer

    SideEffect {
        val window = (view.context as android.app.Activity).window
        window.statusBarColor = color.toArgb()
        window.navigationBarColor = color.toArgb()
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
