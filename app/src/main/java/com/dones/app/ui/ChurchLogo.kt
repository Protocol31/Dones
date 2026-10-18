package com.dones.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Logo y emblema de la Iglesia Anglicana San Andrés (tomados de la app Ergos).
 * Sus trazos son demasiado largos para un recurso de Android, así que viven en
 * assets/logo/ y se leen una sola vez.
 */
object ChurchLogo {
    /** Logo completo (viewBox 0 0 303 295). */
    const val LOGO_FILE = "logo/logo.txt"
    const val LOGO_W = 303f
    const val LOGO_H = 295f

    /** Emblema solo (viewBox 40 6 223 182). */
    const val EMBLEM_FILE = "logo/emblema.txt"

    private val cache = mutableMapOf<String, String>()

    fun pathData(context: Context, file: String): String = synchronized(cache) {
        cache.getOrPut(file) { context.assets.open(file).bufferedReader().use { it.readText() } }
    }

    private fun vector(context: Context, file: String, x: Float, y: Float, w: Float, h: Float): ImageVector =
        ImageVector.Builder("iglesia", w.dp, h.dp, w, h)
            .addGroup(translationX = -x, translationY = -y)
            .addPath(
                PathParser().parsePathString(pathData(context, file)).toNodes(),
                pathFillType = PathFillType.EvenOdd,
                fill = SolidColor(Color.White),
            )
            .clearGroup()
            .build()

    @Composable
    fun logo(): ImageVector {
        val context = LocalContext.current
        return remember { vector(context, LOGO_FILE, 0f, 0f, LOGO_W, LOGO_H) }
    }

    @Composable
    fun emblem(): ImageVector {
        val context = LocalContext.current
        return remember { vector(context, EMBLEM_FILE, 40f, 6f, 223f, 182f) }
    }
}
