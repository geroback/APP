package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = TossBlue,
    onPrimary = Color.White,
    secondary = TossLightGrey,
    onSecondary = TossDarkGrey,
    background = TossDarkGrey,
    surface = Color(0xFF2C2C2E),
    onBackground = TossWhite,
    onSurface = TossWhite,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TossBlue,
    onPrimary = Color.White,
    secondary = TossDarkGrey,
    onSecondary = Color.White,
    background = TossLightGrey,
    surface = TossWhite,
    onBackground = TossDarkGrey,
    onSurface = TossDarkGrey,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set dynamicColor to false by default to respect custom Toss styling
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
