package com.dones.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dones.app.R

/**
 * Piedra tallada: grises cálidos con pátina verdosa; el violeta es la poción que llena lo tallado.
 * (Los nombres vienen de Ergos para no tocar cada pantalla.)
 */
object Ergos {
    val Night = Color(0xFF0E0D10)
    val Night2 = Color(0xFF17151A)
    val Surface = Color(0xFF2A2724)
    val Surface2 = Color(0xFF38342F)
    val Line = Color(0xFF4A453E)
    val LineStrong = Color(0xFF7A7266)
    val Ink = Color(0xFFF3EDE0)
    val Ink2 = Color(0xFFDDD5C5)
    val Muted = Color(0xFFB8AF9F)
    val Violet = Color(0xFF9450FF)
    val VioletHi = Color(0xFFB58BFF)
    val VioletLo = Color(0xFF6A2FE0)
    val VioletDeep = Color(0xFF2A1458)
    val AccentText = Color(0xFFD2BEFF)
    val Gold = Color(0xFFD8C79F)
    val GoldHi = Color(0xFFEADFC0)
    val Positive = Color(0xFF6FDCA8)
    val Negative = Color(0xFFFF8A8A)
    val ButtonTop = Color(0xFF8C52F6)
    val ButtonBottom = Color(0xFF7134EC)
}

val Cormorant = FontFamily(
    Font(R.font.cormorant_medium, FontWeight.Medium),
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_bold, FontWeight.Bold),
)

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

private val ErgosColors = darkColorScheme(
    primary = Ergos.ButtonBottom,
    onPrimary = Color.White,
    primaryContainer = Ergos.VioletDeep,
    onPrimaryContainer = Ergos.Ink,
    secondary = Ergos.Gold,
    onSecondary = Color(0xFF2A1C0C),
    tertiary = Ergos.AccentText,
    background = Ergos.Night,
    onBackground = Ergos.Ink,
    surface = Ergos.Surface,
    onSurface = Ergos.Ink,
    surfaceVariant = Ergos.Surface2,
    onSurfaceVariant = Ergos.Muted,
    surfaceContainer = Ergos.Surface,
    surfaceContainerHigh = Ergos.Surface2,
    surfaceContainerHighest = Ergos.Surface2,
    outline = Ergos.LineStrong,
    outlineVariant = Ergos.Line,
    error = Ergos.Negative,
    onError = Color(0xFF2A0A0A),
)

/**
 * Letra grande para personas de 40 a 65 años (los sp respetan el tamaño de letra del sistema).
 * Títulos en Cormorant Garamond (un poco más grandes porque es una letra fina), texto en Geist.
 */
private val Relief = Shadow(Color(0xCC000000), Offset(0f, 2f), 2f)

private val ErgosTypography = Typography(
    displaySmall = TextStyle(shadow = Relief, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 46.sp),
    headlineMedium = TextStyle(shadow = Relief, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineSmall = TextStyle(shadow = Relief, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 31.sp, lineHeight = 38.sp),
    titleLarge = TextStyle(shadow = Relief, fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    titleMedium = TextStyle(shadow = Relief, fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 30.sp),
    bodyLarge = TextStyle(shadow = Relief, fontFamily = Geist, fontSize = 21.sp, lineHeight = 31.sp),
    bodyMedium = TextStyle(shadow = Relief, fontFamily = Geist, fontSize = 20.sp, lineHeight = 28.sp),
    labelLarge = TextStyle(shadow = Relief, fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 28.sp),
    labelMedium = TextStyle(shadow = Relief, fontFamily = Geist, fontSize = 18.sp, lineHeight = 25.sp),
    labelSmall = TextStyle(shadow = Relief, fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 1.5.sp),
)

private val ErgosShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
)

@Composable
fun DonesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ErgosColors,
        typography = ErgosTypography,
        shapes = ErgosShapes,
        content = content,
    )
}
