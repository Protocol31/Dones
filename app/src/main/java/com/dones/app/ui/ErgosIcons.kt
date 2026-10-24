package com.dones.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Íconos de la app Ergos (trazo fino redondeado con un pequeño rombo de acento).
 * Los primeros vienen tal cual del HTML de Ergos; los demás siguen el mismo estilo.
 */
object ErgosIcons {
    private class Part(val d: String, val filled: Boolean = false, val width: Float = 1.6f, val alpha: Float = 1f)

    private fun line(d: String, width: Float = 1.6f, alpha: Float = 1f) = Part(d, width = width, alpha = alpha)
    private fun solid(d: String, alpha: Float = 1f) = Part(d, filled = true, alpha = alpha)
    private fun diamond(x: Float, y: Float, r: Float = 1.3f) = solid("M$x ${y - r}L${x + r} ${y}L$x ${y + r}L${x - r} ${y}Z")
    private fun circle(cx: Float, cy: Float, r: Float, width: Float = 1.6f) =
        line("M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z", width)
    private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) = line(
        "M${x + rx} ${y}H${x + w - rx}a$rx $rx 0 0 1 $rx ${rx}V${y + h - rx}a$rx $rx 0 0 1 ${-rx} ${rx}" +
            "H${x + rx}a$rx $rx 0 0 1 ${-rx} ${-rx}V${y + rx}a$rx $rx 0 0 1 $rx ${-rx}Z",
    )

    private fun icon(name: String, vararg parts: Part, size: Float = 24f, color: Color = Color.Black): ImageVector {
        val b = ImageVector.Builder(name, size.dp, size.dp, size, size)
        parts.forEach { p ->
            val nodes = PathParser().parsePathString(p.d).toNodes()
            if (p.filled) {
                b.addPath(nodes, fill = SolidColor(color), fillAlpha = p.alpha)
            } else {
                b.addPath(
                    nodes,
                    stroke = SolidColor(color),
                    strokeAlpha = p.alpha,
                    strokeLineWidth = p.width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        return b.build()
    }

    // --- Del HTML de Ergos ---
    val Church = icon(
        "iglesia",
        line("M12 2.2v3.2M10.6 3.6h2.8"), line("M3.5 11.5 12 6.2l8.5 5.3"),
        line("M3.5 11.5c-1 .4-1.2 1.6-.3 1.9M20.5 11.5c1 .4 1.2 1.6.3 1.9"),
        line("M5.5 11v9.5h13V11"), line("M10 20.5v-4a2 2 0 0 1 4 0v4"), diamond(12f, 11.6f, 1.1f),
    )
    val People = icon(
        "personas",
        circle(8.5f, 9f, 2.6f), line("M3.6 20c.3-3.3 2.3-5.3 4.9-5.3s4.6 2 4.9 5.3"),
        circle(16.2f, 10f, 2.2f), line("M14.6 14.9c.5-.2 1-.3 1.6-.3 2.3 0 4 1.8 4.3 5.4"),
        diamond(12.4f, 3.6f, 1.4f), line("M9.6 3.6c-.9 0-1.5.6-1.5 1.3M15.2 3.6c.9 0 1.5.6 1.5 1.3"),
    )
    val Book = icon(
        "libro",
        line("M12 6.5C10 5 7 4.6 3 5v13c4-.4 7 0 9 1.5 2-1.5 5-1.9 9-1.5V5c-4-.4-7 0-9 1.5Z"),
        line("M12 6.5v13"), line("M16 5.2v5.3"), diamond(16f, 11.8f),
        line("M5.5 9c1.6-.1 3 .2 4 .7M5.5 12c1.6-.1 3 .2 4 .7", 1.2f),
    )
    val Scroll = icon(
        "certificado",
        line("M6 3.5h10.5a2 2 0 0 1 2 2V18"),
        line("M6 3.5a2 2 0 0 0-2 2c0 1.1.9 1.7 2 1.7V19a1.8 1.8 0 0 0 1.8 1.8H19a1.8 1.8 0 0 1-1.8-1.8"),
        line("M9 8h6M9 11h6", 1.3f),
        line("M8.6 16.2c1-1.6 2-1.8 2.1-.8.1 1.1-.9 1.9.1 1.6 1-.3 1.6-1.6 2.6-1.4.7.2.4 1 1.6.9"),
    )
    val Envelope = icon(
        "mensaje",
        rect(3f, 5.5f, 18f, 13f, 1.5f), line("m3.6 6.4 8.4 6.6 8.4-6.6"),
        line("m3.6 17.6 6-5.1M20.4 17.6l-6-5.1", 1.2f), solid("M12 11.2l1.9 1.9L12 15l-1.9-1.9Z"),
    )
    val Seal = icon(
        "sello",
        circle(12f, 12f, 7.2f), circle(12f, 12f, 5f, 1f), line("M12 8.6 15.4 12 12 15.4 8.6 12Z"),
        line("M12 10.2v3.6M10.2 12h3.6", 1.2f), line("M3 9.5c-1.2.8-1.2 4.2 0 5M21 9.5c1.2.8 1.2 4.2 0 5"),
    )
    val Bars = icon(
        "lista",
        line("M4 6h16M4 12h16M4 18h16"), line("M9 3.6 11.4 6 9 8.4 6.6 6Z"),
        line("M15.5 9.6 17.9 12l-2.4 2.4-2.4-2.4Z"), line("M7.5 15.6 9.9 18l-2.4 2.4L5.1 18Z"),
    )
    val Heart = icon(
        "corazon",
        line("M12 20.5S3.5 15.5 3.5 9.6C3.5 7 5.3 5.2 7.6 5.2c1.9 0 3.4 1.2 4.4 3 1-1.8 2.5-3 4.4-3 2.3 0 4.1 1.8 4.1 4.4 0 5.9-8.5 10.9-8.5 10.9Z"),
        line("M7.6 8.6c-1 0-1.6.7-1.6 1.5", 1.2f), diamond(12f, 12.8f, 1.4f),
    )
    val Sparkle = icon(
        "don",
        line("M12 3c.6 4.6 2.4 6.4 7 7-4.6.6-6.4 2.4-7 7-.6-4.6-2.4-6.4-7-7 4.6-.6 6.4-2.4 7-7Z"),
        diamond(19f, 18.5f, 1.4f), diamond(5f, 4.5f, 1.1f),
    )
    val Key = icon(
        "llave",
        line("M8 4.5 11.5 8 8 11.5 4.5 8Z"), diamond(8f, 8f, 1.1f), line("M10.6 10.6 19.5 19.5M15.5 15.5l2-2M18 18l2-2"),
    )
    val Document = icon(
        "documento",
        rect(4.5f, 3.5f, 15f, 17f, 1.5f), line("M8 9h8M8 12.5h8M8 16h5"), diamond(12f, 3.5f, 1.4f),
    )
    val Calendar = icon(
        "calendario",
        rect(3.5f, 4.5f, 17f, 15f, 1.5f), line("M3.5 9h17M3.5 14h17M9.5 9v10.5", 1.3f), diamond(6.5f, 6.8f, 1f),
    )
    val Eye = icon(
        "ver",
        line("M2.5 12S6 5.8 12 5.8 21.5 12 21.5 12 18 18.2 12 18.2 2.5 12 2.5 12Z"),
        line("M12 9 15 12 12 15 9 12Z"), diamond(12f, 12f, 1f),
    )
    val Send = icon(
        "enviar",
        line("M6 3.5h8l4.5 4.5v12.5H6Z"), line("M14 3.5V8h4.5"), line("M12.2 17.5v-6M9.8 13.8l2.4-2.4 2.4 2.4"),
    )
    val Cross = icon(
        "cruz",
        line("M12 4v16M7.5 9h9"), circle(12f, 9f, 4.2f, 1.1f), line("M9 20h6"),
    )

    // --- En el mismo estilo, hechos para esta app ---
    val ChevronRight = icon("siguiente", line("M9 5.5 15.5 12 9 18.5", 2f))
    val ChevronLeft = icon("anterior", line("M15 5.5 8.5 12 15 18.5", 2f))
    val ArrowRight = icon("avanzar", line("M4.5 12h15M14 6.5l5.5 5.5-5.5 5.5", 1.8f))
    val Check = icon("correcto", line("M4.5 12.5 9.5 17.5 19.5 6.5", 2.2f))
    val Close = icon("incorrecto", line("M6.5 6.5l11 11M17.5 6.5l-11 11", 2.2f))
    val Plus = icon("agregar", line("M12 5v14M5 12h14", 2f))
    val Person = icon(
        "persona",
        circle(12f, 8.2f, 3.6f), line("M4.5 20.5c.5-4 3.4-6.4 7.5-6.4s7 2.4 7.5 6.4"), diamond(12f, 2.3f, 1.1f),
    )
    val Rings = icon("estado civil", circle(9f, 14f, 5f), circle(15f, 14f, 5f), diamond(12f, 4.5f, 1.4f))
    val Chat = icon(
        "whatsapp",
        line("M4.2 20 5.5 16A8.2 8.2 0 1 1 8.4 18.8Z"), line("M9 10.5h6M9 13.5h4", 1.3f),
    )
    val Shield = icon(
        "cuidado",
        line("M12 3 19.5 6v5.5c0 4.6-3.2 8.2-7.5 9.5-4.3-1.3-7.5-4.9-7.5-9.5V6Z"), line("M8.8 12l2.3 2.3 4.2-4.6", 1.8f),
    )
    val Pencil = icon(
        "escribir",
        line("M4 20l1-4.5L15.5 5a2 2 0 0 1 2.8 0l.7.7a2 2 0 0 1 0 2.8L8.5 19Z"), line("M14 6.5l3.5 3.5"),
    )
    val Trash = icon(
        "borrar",
        line("M4.5 6.5h15"), line("M9.5 6.5V4.5h5v2"), line("M6.5 6.5l1 13.5h9l1-13.5"), line("M10.5 10v7M13.5 10v7", 1.3f),
    )
    val Bookmark = icon("donde quedé", line("M7 3.5h10v17l-5-4-5 4Z"), diamond(12f, 9f, 1.4f))
    val Qr = icon(
        "código qr",
        rect(3.5f, 3.5f, 7f, 7f, 1f), rect(13.5f, 3.5f, 7f, 7f, 1f), rect(3.5f, 13.5f, 7f, 7f, 1f),
        solid("M6 6h2v2H6ZM16 6h2v2h-2ZM6 16h2v2H6Z"),
        line("M13.5 13.5h3v3M20.5 13.5v0M13.5 20.5h3M19.5 17.5v3h-1", 1.6f),
    )
    val Restart = icon("desde el principio", line("M5 12a7 7 0 1 0 2-4.9"), line("M4.5 4v4h4", 1.8f))
    val Choose = icon(
        "elegir",
        rect(3.5f, 4f, 17f, 7f, 1.5f), rect(3.5f, 13f, 17f, 7f, 1.5f), line("M7 7.5l1.5 1.5 3-3", 1.6f), line("M14 16.5h3.5", 1.3f),
    )

    // --- Adornos (colores incluidos) ---
    /** Esquina ornamental de las tarjetas de Ergos (40×40). */
    val OrnamentCorner: ImageVector = ImageVector.Builder("esquina", 40.dp, 40.dp, 40f, 40f).apply {
        val violet = SolidColor(Ergos.Violet)
        addPath(PathParser().parsePathString("M2 38V12C2 6 6 2 12 2h26").toNodes(), stroke = violet, strokeAlpha = .55f, strokeLineWidth = 1.1f, strokeLineCap = StrokeCap.Round)
        addPath(PathParser().parsePathString("M8 24c0-8 8-14 16-14").toNodes(), stroke = violet, strokeAlpha = .4f, strokeLineWidth = 1.1f, strokeLineCap = StrokeCap.Round)
        addPath(PathParser().parsePathString("M8 4l4 4-4 4-4-4z").toNodes(), fill = violet, fillAlpha = .8f)
    }.build()
}
