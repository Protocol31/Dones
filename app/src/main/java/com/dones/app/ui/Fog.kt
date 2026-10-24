package com.dones.app.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.drawscope.scale

/** Niebla violeta (las texturas de la batalla, pasadas a violeta) como fondo fijo de las pantallas. */
private var fogCache: ImageBitmap? = null

fun Modifier.violetFog(): Modifier = composed {
    val context = LocalContext.current
    val fog = remember {
        fogCache ?: runCatching {
            context.assets.open("fondo/niebla-violeta-1.jpg").use { BitmapFactory.decodeStream(it) }.asImageBitmap()
        }.getOrNull()?.also { fogCache = it }
    }
    drawWithCache {
        val glow = Brush.radialGradient(listOf(Ergos.Violet.copy(alpha = .18f), Color.Transparent), center = Offset(size.width / 2, 0f), radius = size.maxDimension * .7f)
        val veil = Brush.verticalGradient(listOf(Ergos.Night.copy(alpha = .72f), Ergos.Night.copy(alpha = .84f), Ergos.Night.copy(alpha = .92f)))
        onDrawBehind {
            drawRect(Ergos.Night)
            if (fog != null) {
                // A todo el ancho, sin estirar; si falta alto, se repite volteada para que no se note el corte.
                val w = size.width
                val h = w * fog.height / fog.width
                drawImage(fog, dstSize = IntSize(w.toInt(), h.toInt()), filterQuality = FilterQuality.Medium)
                if (h < size.height) {
                    scale(1f, -1f, pivot = Offset(w / 2, h)) {
                        drawImage(fog, dstSize = IntSize(w.toInt(), h.toInt()), filterQuality = FilterQuality.Medium)
                    }
                }
            }
            drawRect(veil)
            drawRect(glow)
        }
    }
}
