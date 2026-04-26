package com.example.baby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.baby.data.AppDatabase
import com.example.baby.data.BabyProfileManager
import com.example.baby.data.sync.SyncCoordinator
import com.example.baby.ui.navigation.AppNavigation
import com.example.baby.ui.theme.BabyFeedingTheme

class MainActivity : ComponentActivity() {

    private var syncCoordinator: SyncCoordinator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // If profile already exists, start sync immediately
        val profile = BabyProfileManager.getProfile(this)
        if (profile != null) {
            startSync(profile.name, profile.birthDate)
        }

        setContent {
            BabyFeedingTheme {
                AppNavigation(
                    onProfileReady = {
                        val p = BabyProfileManager.getProfile(this@MainActivity)
                        if (p != null && syncCoordinator == null) {
                            startSync(p.name, p.birthDate)
                        }
                    }
                )
            }
        }
    }

    private fun startSync(babyName: String, babyBirthDate: String) {
        syncCoordinator = SyncCoordinator(
            dao = AppDatabase.getInstance(this).feedingDao(),
            lifecycleOwner = this,
            scope = lifecycleScope,
            babyName = babyName,
            babyBirthDate = babyBirthDate
        ).also { it.start() }
    }
}
