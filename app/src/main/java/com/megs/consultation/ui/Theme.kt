package com.megs.consultation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF1B8A5A)
val GreenDark = Color(0xFF0F5C3B)
val GreenLight = Color(0xFFE2F3E9)
val Bg = Color(0xFFF2F7F3)
val Muted = Color(0xFF6B7A72)
val Red = Color(0xFFD64545)
val RedSoft = Color(0xFFFCE1E1)
val Amber = Color(0xFFA9671A)
val AmberSoft = Color(0xFFFFF1D6)
val Slate = Color(0xFF3D4A5C)
val SlateSoft = Color(0xFFEDF1F6)

private val scheme = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = GreenDark,
    background = Bg,
    surface = Color.White,
    surfaceVariant = Color(0xFFEFF4F0),
    error = Red
)

@Composable
fun MegsTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, content = content)
