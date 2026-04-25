package com.example.baby.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

// Baby-friendly warm color palette
val BabyPink = Color(0xFFFFB3B3)
val BabyBlue = Color(0xFFA3D5FF)
val BabyYellow = Color(0xFFFFF3B0)
val BabyGreen = Color(0xFFB3E5B5)
val WarmOrange = Color(0xFFFFB74D)
val SoftPurple = Color(0xFFCE93D8)

val PrimaryLight = Color(0xFF8E6C8A)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFFFD7F9)
val OnPrimaryContainerLight = Color(0xFF382636)
val SecondaryLight = Color(0xFF6F566A)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFF9D8F0)
val OnSecondaryContainerLight = Color(0xFF281425)
val SurfaceLight = Color(0xFFFFF8F4)
val OnSurfaceLight = Color(0xFF1E1B1C)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFF0E3EC),
    background = Color(0xFFFFFBFE),
)

@Composable
fun BabyFeedingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
