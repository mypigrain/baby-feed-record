package com.example.baby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.baby.data.AppDatabase
import com.example.baby.data.sync.SyncCoordinator
import com.example.baby.ui.navigation.AppNavigation
import com.example.baby.ui.theme.BabyFeedingTheme

class MainActivity : ComponentActivity() {

    private var syncCoordinator: SyncCoordinator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        syncCoordinator = SyncCoordinator(
            dao = AppDatabase.getInstance(this).feedingDao(),
            lifecycleOwner = this,
            scope = lifecycleScope
        ).also { it.start() }

        setContent {
            BabyFeedingTheme {
                AppNavigation()
            }
        }
    }
}
