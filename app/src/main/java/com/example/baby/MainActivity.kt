package com.example.baby

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.baby.data.AppDatabase
import com.example.baby.data.BabyProfileManager
import com.example.baby.data.FeedingRecord
import com.example.baby.data.sync.SyncCoordinator
import com.example.baby.ui.home.AmountPicker
import com.example.baby.ui.navigation.AppNavigation
import com.example.baby.ui.theme.BabyFeedingTheme
import com.example.baby.widget.QuickRecordWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainActivity : ComponentActivity() {

    private var syncCoordinator: SyncCoordinator? = null
    private var showQuickRecord by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // Handle quick record from widget
        handleQuickRecord(intent)

        // If profile already exists, start sync immediately
        val profile = BabyProfileManager.getProfile(this)
        if (profile != null) {
            startSync(profile.name, profile.birthDate)
        }

        setContent {
            BabyFeedingTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        onProfileReady = {
                            val p = BabyProfileManager.getProfile(this@MainActivity)
                            if (p != null && syncCoordinator == null) {
                                startSync(p.name, p.birthDate)
                            }
                        }
                    )

                    if (showQuickRecord) {
                        QuickRecordDialog(
                            onDismiss = {
                                showQuickRecord = false
                                // Clear the flag so it doesn't re-trigger on resume
                                intent.removeExtra("quick_record")
                            },
                            onRecorded = {
                                showQuickRecord = false
                                intent.removeExtra("quick_record")
                                // Refresh widget after recording
                                val appWidgetManager = AppWidgetManager.getInstance(this@MainActivity)
                                val ids = ComponentName(this@MainActivity, QuickRecordWidget::class.java).let {
                                    appWidgetManager.getAppWidgetIds(it)
                                }
                                for (id in ids) {
                                    QuickRecordWidget.updateAppWidget(this@MainActivity, appWidgetManager, id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleQuickRecord(intent)
    }

    private fun handleQuickRecord(intent: Intent?) {
        if (intent?.getBooleanExtra("quick_record", false) == true) {
            showQuickRecord = true
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

@Composable
private fun QuickRecordDialog(
    onDismiss: () -> Unit,
    onRecorded: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).feedingDao() }
    val scope = rememberCoroutineScope()

    // Load remembered selections from home_prefs
    val prefs = context.getSharedPreferences("home_prefs", Context.MODE_PRIVATE)
    var selectedAmount by remember {
        mutableStateOf(
            prefs.getInt("last_amount", -1).let { if (it >= 0) it else null }
        )
    }
    var selectedType by remember {
        mutableStateOf(prefs.getString("last_type", "breast") ?: "breast")
    }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // consume clicks within the card
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "快速记录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Feeding type selection
                Text(
                    "喂养类型",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "breast" to "母乳",
                        "formula" to "配方奶",
                        "mixed" to "混合"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = selectedType == value,
                            onClick = { selectedType = value },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Amount picker
                AmountPicker(
                    selectedAmount = selectedAmount,
                    onAmountSelected = { selectedAmount = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Record button
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch(Dispatchers.IO) {
                            val record = FeedingRecord(
                                syncId = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                amountMl = selectedAmount,
                                type = selectedType
                            )
                            dao.insert(record)

                            // Save selection
                            prefs.edit()
                                .putInt("last_amount", selectedAmount ?: -1)
                                .putString("last_type", selectedType)
                                .apply()

                            withContext(Dispatchers.Main) {
                                onRecorded()
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        if (isLoading) "记录中..." else "记录喝奶",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
