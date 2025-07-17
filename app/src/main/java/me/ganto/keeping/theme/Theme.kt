package me.ganto.keeping.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = AccentOrange,
    tertiary = AccentGreen,
    background = BackgroundDark,
    surface = CardDark,
    onPrimary = AccentGreen,
    onSecondary = PrimaryLight,
    onTertiary = AccentPink,
    onBackground = CardLight,
    onSurface = CardLight
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = AccentOrange,
    tertiary = AccentGreen,
    background = BackgroundLight,
    surface = CardLight,
    onPrimary = CardLight,
    onSecondary = PrimaryDark,
    onTertiary = AccentPink,
    onBackground = PrimaryDark,
    onSurface = PrimaryDark
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

@Composable
fun KeepingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // 启用动态取色，跟随壁纸
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}