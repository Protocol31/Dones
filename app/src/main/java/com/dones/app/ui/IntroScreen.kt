package com.dones.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/*
 * Intro de la app: el logo de la Iglesia Anglicana San Andrés tallado en una losa de piedra.
 * Un líquido violeta (la poción) sube y va rellenando lo tallado hasta llenar todo el logo;
 * entonces aparece "Llamados" en relieve y la app se abre.
 * Solo usa el Canvas de Compose (sin shaders), así funciona igual en todos los teléfonos.
 */

private const val FILL_MS = 4500
private const val HOLD_MS = 1200L

private val FillEasing = CubicBezierEasing(.4f, 0f, .2f, 1f)
private val SlabShape = CutCornerShape(32.dp)

private val CavityTop = Color(0xFF07060A)
private val CavityBottom = Color(0xFF1C1822)
private val RimLight = Color(0xFFFFF0D2)

@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val finish by rememberUpdatedState(onFinished)
    var ended by remember { mutableStateOf(false) }
    val end: () -> Unit = { if (!ended) { ended = true; finish() } }

    val logoPath = remember {
        runCatching {
            PathParser().parsePathString(ChurchLogo.pathData(context, ChurchLogo.LOGO_FILE)).toPath().apply {
                fillType = PathFillType.EvenOdd
            }
        }.getOrNull()
    }

    val slab = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val pulse = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { slab.animateTo(1f, tween(600)) }
            delay(500)
            fill.animateTo(1f, tween(FILL_MS, easing = FillEasing))
            launch {
                pulse.animateTo(1f, tween(320))
                pulse.animateTo(0f, tween(800))
            }
            textAlpha.animateTo(1f, tween(800))
        }
        delay(HOLD_MS)
        end()
    }

    val infinite = rememberInfiniteTransition(label = "pocion")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing)),
        label = "ola",
    )
    val bubbles by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "burbujas",
    )

    Box(
        Modifier
            .fillMaxSize()
            .stoneBackdrop()
            .semantics(mergeDescendants = true) { contentDescription = "Iglesia Anglicana San Andrés. Llamados" }
            .pointerInput(Unit) { detectTapGestures { end() } },
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            // Resplandor violeta que crece al llenarse; la losa va encima.
            Box(
                Modifier
                    .fillMaxWidth(.84f)
                    .aspectRatio(1f)
                    .graphicsLayer { alpha = slab.value }
                    .drawBehind {
                        val g = (0.04f + 0.38f * fill.value + 0.35f * pulse.value).coerceIn(0f, 1f)
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Ergos.Violet.copy(alpha = g), Color.Transparent),
                                center = center,
                                radius = size.width * (0.55f + 0.25f * fill.value),
                            ),
                            radius = size.width * (0.8f + 0.1f * fill.value),
                            center = center,
                        )
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .stone(shape = SlabShape, bevel = 4.dp, elevation = 14.dp),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawSlabCarvings()
                        if (logoPath != null) {
                            drawCarvedLogo(logoPath, fill.value, pulse.value, phase, bubbles)
                        }
                    }
                }
            }

            Column(
                Modifier
                    .padding(top = 28.dp, start = 24.dp, end = 24.dp)
                    .graphicsLayer {
                        alpha = textAlpha.value
                        translationY = (1f - textAlpha.value) * 24.dp.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Llamados",
                    style = TextStyle(
                        fontFamily = Cormorant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        lineHeight = 54.sp,
                        color = Ergos.Ink,
                        shadow = Shadow(Color(0xE6000000), Offset(0f, 3f), 3f),
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "un curso para conocer tus dones espirituales",
                    style = TextStyle(
                        fontFamily = Geist,
                        fontSize = 18.sp,
                        lineHeight = 25.sp,
                        color = Ergos.Ink2,
                        shadow = Shadow(Color(0xCC000000), Offset(0f, 2f), 2f),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    "IGLESIA ANGLICANA SAN ANDRÉS",
                    style = TextStyle(
                        fontFamily = Geist,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        color = Ergos.AccentText,
                        shadow = Shadow(Color(0xCC000000), Offset(0f, 2f), 2f),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }

        // Botón "Saltar" de piedra, grande para tocar.
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .widthIn(min = 120.dp)
                .heightIn(min = 56.dp)
                .stone(shape = StoneShapeSmall, elevation = 4.dp)
                .clickable(role = Role.Button, onClickLabel = "Saltar la presentación", onClick = end)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Saltar",
                style = TextStyle(
                    fontFamily = Geist,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = Ergos.Ink,
                    shadow = Shadow(Color(0xCC000000), Offset(0f, 2f), 2f),
                ),
            )
        }
    }
}

/** Marco cincelado dentro de la losa y cuatro rombos tallados en el centro de los lados. */
private fun DrawScope.drawSlabCarvings() {
    val inset = size.width * 0.055f
    val line = 2.dp.toPx()
    val rect = Size(size.width - 2 * inset, size.height - 2 * inset)
    // Línea hundida: oscura arriba-izquierda, clara abajo-derecha.
    translate(1.5.dp.toPx(), 1.5.dp.toPx()) {
        drawRect(RimLight.copy(alpha = .35f), Offset(inset, inset), rect, style = Stroke(line))
    }
    drawRect(Color.Black.copy(alpha = .75f), Offset(inset, inset), rect, style = Stroke(line))

    val r = 7.dp.toPx()
    val mid = Offset(size.width / 2f, size.height / 2f)
    val spots = listOf(
        Offset(mid.x, inset), Offset(mid.x, size.height - inset),
        Offset(inset, mid.y), Offset(size.width - inset, mid.y),
    )
    for (c in spots) {
        val d = Path().apply {
            moveTo(c.x, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r, c.y); close()
        }
        translate(1.2.dp.toPx(), 1.2.dp.toPx()) { drawPath(d, RimLight.copy(alpha = .4f)) }
        drawPath(d, Color(0xFF0C0B0E))
    }
}

/** Logo tallado (hundido en la piedra) y la poción violeta que lo va rellenando. */
private fun DrawScope.drawCarvedLogo(path: Path, fill: Float, pulse: Float, phase: Float, bubbles: Float) {
    val lw = ChurchLogo.LOGO_W
    val lh = ChurchLogo.LOGO_H
    val pad = size.width * 0.14f
    val s = min((size.width - 2 * pad) / lw, (size.height - 2 * pad) / lh)
    val ox = (size.width - lw * s) / 2f
    val oy = (size.height - lh * s) / 2f
    val d = 1.6.dp.toPx() / s

    withTransform({
        translate(ox, oy)
        scale(s, s, pivot = Offset.Zero)
    }) {
        // Bordes de la talla: claro abajo-derecha, oscuro arriba-izquierda.
        translate(d, d) { drawPath(path, RimLight.copy(alpha = .55f)) }
        translate(-d, -d) { drawPath(path, Color.Black.copy(alpha = .8f)) }

        // Cavidad oscura.
        drawPath(path, Brush.verticalGradient(listOf(CavityTop, CavityBottom), startY = 0f, endY = lh))
        clipPath(path) {
            drawPath(path, Color.Black.copy(alpha = .65f), style = Stroke(width = 6f))
        }

        // Poción.
        if (fill > 0.001f) {
            val top = lh + 6f - fill * (lh + 24f)
            val amp = 3.2f * (1f - fill * fill * fill)
            val wave = Path()
            val crest = Path()
            wave.moveTo(-10f, lh + 10f)
            var x = -10f
            var first = true
            while (x <= lw + 10f) {
                val y = top + amp * sin(x * 0.055f + phase) + amp * 0.5f * sin(x * 0.13f - phase * 1.3f)
                wave.lineTo(x, y)
                if (first) { crest.moveTo(x, y); first = false } else crest.lineTo(x, y)
                x += 4f
            }
            wave.lineTo(lw + 10f, lh + 10f)
            wave.close()

            clipPath(path) {
                drawPath(wave, Brush.verticalGradient(listOf(Ergos.VioletHi, Ergos.Violet, Ergos.VioletLo), startY = top, endY = lh))
                // Luz que se mueve por dentro del líquido.
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0x66E9D8FF), Color.Transparent),
                        center = Offset(lw * 0.5f, top + 50f),
                        radius = 150f,
                    ),
                    radius = 150f,
                    center = Offset(lw * 0.5f, top + 50f),
                )
                val sheenX = bubbles * (lw + 200f) - 100f
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = .22f), Color.White.copy(alpha = 0f)),
                        start = Offset(sheenX - 50f, 0f),
                        end = Offset(sheenX + 50f, lh),
                    ),
                    topLeft = Offset(-10f, -30f),
                    size = Size(lw + 20f, lh + 60f),
                )
                // Burbujas que suben.
                for (i in 0 until 9) {
                    val xx = 22f + ((i * 37) % 100) / 100f * (lw - 44f)
                    val speed = 0.6f + (i % 4) * 0.25f
                    val yf = (bubbles * speed + i * 0.113f) % 1f
                    val by = lh - yf * (lh - top)
                    if (by > top + 5f) {
                        drawCircle(
                            Color.White.copy(alpha = .38f),
                            radius = 1.4f + (i % 3) * 0.9f,
                            center = Offset(xx + 3f * sin(by * 0.1f + i), by),
                        )
                    }
                }
                // Borde brillante de la superficie.
                drawPath(crest, Color.White.copy(alpha = .6f), style = Stroke(width = 1.8f, cap = StrokeCap.Round))
                // Destello al llenarse.
                if (pulse > 0.001f) {
                    drawRect(
                        Color(0xFFE4D2FF).copy(alpha = .38f * pulse),
                        topLeft = Offset(-10f, -30f),
                        size = Size(lw + 20f, lh + 60f),
                    )
                }
            }
        }

        // Borde fino por encima para que lo tallado se vea nítido.
        drawPath(path, Color.Black.copy(alpha = .5f), style = Stroke(width = 1.2f))
    }
}
