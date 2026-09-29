package com.example.rutalogcliente.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = AzulRuta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = RojoRuta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410003),
    tertiary = VerdeEntrega,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB8F0B9),
    onTertiaryContainer = Color(0xFF002106),
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF191C1E),
    surface = Color.White,
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF71787E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF002F64),
    primaryContainer = Color(0xFF15457F),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFFFB3AC),
    onSecondary = Color(0xFF68000B),
    secondaryContainer = Color(0xFF930015),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFF9CD49E),
    onTertiary = Color(0xFF00390F),
    tertiaryContainer = Color(0xFF16521F),
    onTertiaryContainer = Color(0xFFB8F0B9),
    background = Color(0xFF0F1417),
    onBackground = Color(0xFFE0E3E6),
    surface = Color(0xFF171C20),
    onSurface = Color(0xFFE0E3E6),
    surfaceVariant = Color(0xFF41474D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9198)
)

@Composable
fun RutaLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}
