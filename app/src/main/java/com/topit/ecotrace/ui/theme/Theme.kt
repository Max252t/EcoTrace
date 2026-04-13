package com.topit.ecotrace.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary                = EcoPrimary,
    onPrimary              = EcoOnPrimary,
    primaryContainer       = EcoPrimaryContainer,
    onPrimaryContainer     = EcoOnPrimaryContainer,
    secondary              = EcoSecondary,
    onSecondary            = EcoOnSecondary,
    secondaryContainer     = EcoSecondaryContainer,
    onSecondaryContainer   = EcoOnSecondaryContainer,
    tertiary               = EcoTertiary,
    onTertiary             = EcoOnTertiary,
    tertiaryContainer      = EcoTertiaryContainer,
    onTertiaryContainer    = EcoOnTertiaryContainer,
    background             = EcoBackground,
    onBackground           = EcoOnBackground,
    surface                = EcoSurface,
    onSurface              = EcoOnSurface,
    surfaceVariant         = EcoSurfaceVariant,
    onSurfaceVariant       = EcoOnSurfaceVariant,
    outline                = EcoOutline,
    outlineVariant         = EcoOutlineVariant,
    error                  = EcoError,
    onError                = EcoOnError,
    errorContainer         = EcoErrorContainer,
    onErrorContainer       = EcoOnErrorContainer,
)

private val DarkColors = darkColorScheme(
    primary                = EcoPrimaryDark,
    onPrimary              = EcoOnPrimaryDark,
    primaryContainer       = EcoPrimaryContainerDark,
    onPrimaryContainer     = EcoOnPrimaryContainerDark,
    secondary              = EcoSecondaryDark,
    onSecondary            = EcoOnSecondaryDark,
    secondaryContainer     = EcoSecondaryContainerDark,
    onSecondaryContainer   = EcoOnSecondaryContainerDark,
    tertiary               = EcoTertiaryDark,
    onTertiary             = EcoOnTertiaryDark,
    tertiaryContainer      = EcoTertiaryContainerDark,
    onTertiaryContainer    = EcoOnTertiaryContainerDark,
    background             = EcoBackgroundDark,
    onBackground           = EcoOnBackgroundDark,
    surface                = EcoSurfaceDark,
    onSurface              = EcoOnSurfaceDark,
    surfaceVariant         = EcoSurfaceVariantDark,
    onSurfaceVariant       = EcoOnSurfaceVariantDark,
    outline                = EcoOutlineDark,
    outlineVariant         = EcoOutlineVariantDark,
)

@Composable
fun EcoTraceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Fixed branded palette — UI UX Pro Max: Hyperlocal Services.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EcoTypography,
        content = content,
    )
}
