package com.dones.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** Esquinas cortadas (chaflán) como las placas talladas de la referencia. */
val StoneShape = CutCornerShape(10.dp)
val StoneShapeSmall = CutCornerShape(6.dp)

/** Texturas de piedra (assets/ui/): baldosas de 512 px que se repiten sin costuras. */
object StoneAssets {
    private val cache = HashMap<String, ImageBitmap>()

    fun bitmap(context: Context, file: String): ImageBitmap = synchronized(cache) {
        cache.getOrPut(file) {
            context.assets.open("ui/$file").use { BitmapFactory.decodeStream(it) }.asImageBitmap()
        }
    }
}

/** Pincel con la textura de piedra. [dark] = la baldosa oscura que se usa de fondo. */
@Composable
fun rememberStoneBrush(dark: Boolean = false): Brush {
    val context = LocalContext.current
    return remember(dark) {
        ShaderBrush(
            ImageShader(
                StoneAssets.bitmap(context, if (dark) "piedra-oscura.jpg" else "piedra.jpg"),
                TileMode.Repeated,
                TileMode.Repeated,
            ),
        )
    }
}

/** Poción: el líquido violeta que rellena lo tallado. */
val PotionBrush: Brush = Brush.verticalGradient(listOf(Ergos.VioletHi, Ergos.Violet, Ergos.VioletLo))

/**
 * Losa de piedra: textura, borde biselado (luz arriba a la izquierda) y sombra.
 * [inset] = hundida en la piedra (para ranuras y botones apretados).
 * [tint] se pinta encima para oscurecer o dar color a la losa.
 */
@Composable
fun Modifier.stone(
    shape: Shape = StoneShape,
    dark: Boolean = false,
    tint: Color = Color.Transparent,
    bevel: Dp = 3.dp,
    inset: Boolean = false,
    elevation: Dp = 6.dp,
): Modifier {
    val brush = rememberStoneBrush(dark)
    return this
        .then(
            if (elevation > 0.dp && !inset) Modifier.shadow(elevation, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            else Modifier,
        )
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline, brush)
            if (tint != Color.Transparent) drawOutline(outline, tint)
            val w = bevel.toPx()
            val light = Color(0xFFFFF4DC)
            val top = if (inset) Color.Black.copy(alpha = .75f) else light.copy(alpha = .5f)
            val bottom = if (inset) light.copy(alpha = .35f) else Color.Black.copy(alpha = .8f)
            if (inset) {
                drawOutline(outline, Brush.verticalGradient(listOf(Color.Black.copy(alpha = .55f), Color.Transparent)), style = Stroke(w * 5))
            }
            drawOutline(outline, Brush.verticalGradient(listOf(top, Color.Transparent, bottom)), style = Stroke(w * 2))
            drawOutline(outline, Color.Black.copy(alpha = .6f), style = Stroke(2.dp.toPx()))
        }
}

/** Fondo de pantalla: piedra oscura con viñeta y un resplandor violeta leve arriba. */
@Composable
fun Modifier.stoneBackdrop(): Modifier {
    val brush = rememberStoneBrush(dark = true)
    return this.drawBehind {
        drawRect(brush)
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = .6f)),
                center = center,
                radius = max(size.width, size.height) * .85f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(Ergos.Violet.copy(alpha = .14f), Color.Transparent),
                center = Offset(0f, 0f),
                radius = 1100f,
            ),
        )
    }
}
