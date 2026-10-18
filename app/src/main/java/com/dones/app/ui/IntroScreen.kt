package com.dones.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Duración total del intro. Se puede saltar con un toque. */
private const val INTRO_MILLIS = 4200

/** Avance de una etapa entre [from] y [to] (0 a 1), con aceleración suave. */
private fun stage(t: Float, from: Float, to: Float): Float =
    FastOutSlowInEasing.transform(((t - from) / (to - from)).coerceIn(0f, 1f))

/**
 * Intro "Llamado · por Ergos": en la noche aparece una luz, un ojo se abre,
 * brilla con un resplandor espiritual y, al abrirse del todo, entramos a la app.
 */
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(INTRO_MILLIS, easing = LinearEasing))
        finish()
    }
    val pulse by rememberInfiniteTransition(label = "pulso").animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulso",
    )
    val spin by rememberInfiniteTransition(label = "rayos").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "rayos",
    )

    val t = progress.value
    val glowIn = stage(t, 0.00f, 0.25f)       // aparece la luz
    val open = stage(t, 0.18f, 0.55f)         // el ojo se abre
    val shine = stage(t, 0.45f, 0.70f)        // brilla
    val words = stage(t, 0.52f, 0.72f)        // aparece "Llamado"
    val exit = stage(t, 0.86f, 1.00f)         // se abre hacia la app

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Ergos.Night)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() }
            .semantics { contentDescription = "Llamado, por Ergos. Toca para entrar." },
    ) {
        // Mismas medidas que el dibujo: centro del ojo al 42 % del alto, ancho 78 % (máx. 900 px).
        val eyeWidthDp = min(maxWidth.value * 0.78f, 900f / androidx.compose.ui.platform.LocalDensity.current.density)
        val textTop = maxHeight * 0.42f + (eyeWidthDp * 0.30f).dp + 28.dp
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Al final el ojo "nos deja entrar": crece y se desvanece.
                    val grow = 1f + exit * 5f
                    scaleX = grow
                    scaleY = grow
                    alpha = 1f - exit
                },
        ) {
            val c = Offset(size.width / 2f, size.height * 0.42f)
            val eyeW = min(size.width * 0.78f, 900f)
            drawGlow(c, eyeW, glowIn, shine, pulse)
            drawRays(c, eyeW, shine * (1f - exit), spin)
            drawEye(c, eyeW, open, shine, pulse)
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = textTop)
                .graphicsLayer {
                    alpha = words * (1f - exit)
                    translationY = (1f - words) * 40f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Llamado",
                style = TextStyle(
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 64.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    brush = Brush.verticalGradient(listOf(Ergos.GoldHi, Ergos.Gold, Ergos.VioletHi)),
                ),
            )
            Text(
                "POR ERGOS",
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 6.sp,
                    color = Ergos.Muted,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

/** Luz de fondo: violeta profundo con un centro dorado que respira. */
private fun DrawScope.drawGlow(c: Offset, eyeW: Float, glowIn: Float, shine: Float, pulse: Float) {
    val r = eyeW * (0.9f + 0.25f * shine) * (0.95f + 0.05f * pulse)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Ergos.Violet.copy(alpha = 0.55f * glowIn),
                Ergos.VioletLo.copy(alpha = 0.25f * glowIn),
                Color.Transparent,
            ),
            center = c,
            radius = r,
        ),
        radius = r,
        center = c,
    )
    val core = eyeW * 0.35f * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Ergos.GoldHi.copy(alpha = 0.55f * shine), Color.Transparent),
            center = c,
            radius = core,
        ),
        radius = core,
        center = c,
    )
}

/** Rayos finos de luz que giran despacio alrededor del ojo. */
private fun DrawScope.drawRays(c: Offset, eyeW: Float, strength: Float, spin: Float) {
    if (strength <= 0f) return
    rotate(spin, pivot = c) {
        val count = 24
        for (i in 0 until count) {
            val a = Math.toRadians(i * 360.0 / count)
            val inner = eyeW * 0.42f
            val outer = eyeW * (if (i % 2 == 0) 0.95f else 0.75f)
            drawLine(
                brush = Brush.linearGradient(
                    listOf(Ergos.GoldHi.copy(alpha = 0.5f * strength), Color.Transparent),
                    start = Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                    end = Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                ),
                start = Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                end = Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** El ojo: párpados en forma de almendra que se separan, iris violeta y dorado, pupila y un destello. */
private fun DrawScope.drawEye(c: Offset, eyeW: Float, open: Float, shine: Float, pulse: Float) {
    val half = eyeW / 2f
    val lid = eyeW * 0.30f * open // separación de los párpados
    val left = Offset(c.x - half, c.y)
    val right = Offset(c.x + half, c.y)

    val eye = Path().apply {
        moveTo(left.x, left.y)
        cubicTo(c.x - half * 0.45f, c.y - lid, c.x + half * 0.45f, c.y - lid, right.x, right.y)
        cubicTo(c.x + half * 0.45f, c.y + lid, c.x - half * 0.45f, c.y + lid, left.x, left.y)
        close()
    }

    // Dentro del ojo.
    if (open > 0.02f) {
        clipPath(eye) {
            drawRect(Ergos.Night2)
            val irisR = eyeW * 0.17f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Ergos.GoldHi, Ergos.Gold, Ergos.Violet, Ergos.VioletDeep),
                    center = c,
                    radius = irisR,
                ),
                radius = irisR,
                center = c,
            )
            drawCircle(Ergos.Night, radius = irisR * 0.38f * (1.05f - 0.1f * shine), center = c)
            // Destello de luz en la pupila.
            drawCircle(
                Color.White.copy(alpha = 0.9f * open),
                radius = irisR * 0.12f,
                center = Offset(c.x + irisR * 0.28f, c.y - irisR * 0.28f),
            )
        }
    }

    // Contorno que brilla: varias pasadas cada vez más finas = resplandor.
    val glowAlpha = 0.25f + 0.35f * shine * pulse.coerceAtMost(1f)
    listOf(18f to 0.12f, 9f to 0.25f, 4f to 0.6f, 2f to 1f).forEach { (w, a) ->
        drawPath(
            eye,
            brush = Brush.horizontalGradient(
                listOf(Ergos.VioletHi, Ergos.GoldHi, Ergos.VioletHi),
                startX = left.x,
                endX = right.x,
            ),
            alpha = (a * (if (w > 4f) glowAlpha else 1f)).coerceIn(0f, 1f),
            style = Stroke(width = w, cap = StrokeCap.Round),
        )
    }
}
