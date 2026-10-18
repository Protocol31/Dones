package com.dones.app.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.PathParser
import com.dones.app.R
import com.dones.app.ui.ChurchLogo
import com.dones.core.ClassInfo
import com.dones.core.GiftScore
import com.dones.core.HoutsContent
import com.dones.core.HoutsResult
import com.dones.core.ParticipantState
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** Datos de la iglesia que respalda los documentos. */
object Church {
    const val NAME = "Iglesia Anglicana San Andrés"

    /** RUT de la iglesia: pendiente de que David lo confirme. Vacío = no se imprime. */
    const val RUT = ""

    /** Página donde se verificará el certificado al leer su QR (necesita la base de datos en línea). */
    const val VERIFY_URL = "https://protocol31.github.io/Dones/verificar/?c="

    val footer: String
        get() = listOf(NAME, RUT.takeIf { it.isNotBlank() }?.let { "RUT $it" }, "Llamados").filterNotNull().joinToString("  ·  ")
}

/**
 * PDFs generados en el teléfono con el diseño de Ergos: noche violeta, oro y florituras.
 * Mis dones: A4 vertical (595 × 842 pt). Certificado: A4 horizontal (842 × 595 pt).
 */
object PdfDocuments {

    private val spanishDate = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es"))

    // ------------------------------------------------------------------ Mis dones

    fun results(
        context: Context,
        dir: File,
        content: HoutsContent,
        participant: ParticipantState,
        result: HoutsResult,
        info: ClassInfo?,
    ): File {
        val file = File(dir, "mis-dones.pdf")
        val doc = PdfDocument()
        val pen = Pen(context)
        val W = 595f
        val H = 842f
        val m = 58f
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), pageNo).create())
            page = p
            pen.canvas = p.canvas
            pen.background(W, H, W / 2f, 0f, W * .9f)
            pen.frame(W, H, 16f, 1.2f, 21f)
            pen.corners(W, H) { pen.cornerMini() }
            pen.text("${Church.footer}  ·  Página $pageNo", W / 2, H - 30, pen.geist, 7f, pen.solid(Ergos.MUTED), Align.CENTER)
            y = 52f
        }

        fun need(h: Float) { if (y + h > H - 58f) newPage() }

        fun section(title: String, minHeight: Float = 40f) {
            need(minHeight + 36f)
            y += 22f
            pen.text(title.uppercase(Locale("es")), W / 2, y, pen.geistSemi, 8.5f, pen.solid(Ergos.ACCENT), Align.CENTER, 3f)
            y += 12f
            pen.ornament(W / 2, y, 90f)
            y += 14f
        }

        newPage()
        // Encabezado: logo, curso, título, nombre
        val logoW = 64f
        pen.logo(W / 2 - logoW / 2, 40f, logoW)
        y = 40f + logoW * ChurchLogo.LOGO_H / ChurchLogo.LOGO_W
        y += 22f
        pen.text("LLAMADOS  ·  ${(info?.courseName ?: "Descubriendo Mi Ministerio").uppercase(Locale("es"))}", W / 2, y, pen.geistSemi, 7.5f, pen.solid(Ergos.ACCENT), Align.CENTER, 2.4f)
        y += 32f
        pen.text("Mis dones espirituales", W / 2, y, pen.corBold, 32f, pen.sheen(y - 26f, y + 4f), Align.CENTER)
        y += 34f
        val nameSize = pen.fitSize(participant.name, pen.fraktur, 24f, W - 2 * m)
        pen.text(participant.name, W / 2, y, pen.fraktur, nameSize, pen.solid(Ergos.INK), Align.CENTER)
        y += 18f
        pen.text(
            listOf(info?.churchName ?: Church.NAME, LocalDate.now().format(spanishDate)).joinToString("  ·  "),
            W / 2, y, pen.corMed, 11.5f, pen.solid(Ergos.MUTED), Align.CENTER,
        )

        // Dones dominantes: tarjetas
        section("Tus dones dominantes", 110f)
        for (s in result.dominant) {
            val textW = W - 2 * m - 92f
            val def = pen.wrap(s.gift.definition, pen.geist, 9.5f, textW)
            val refs = pen.wrap(s.gift.refs.joinToString("  ·  "), pen.geistMed, 8f, textW)
            val h = 44f + def.size * 13.5f + refs.size * 11f + 10f
            need(h + 10f)
            val top = y
            val l = m
            val r = W - m
            pen.card(RectF(l, top, r, top + h), Ergos.SURFACE2, .92f, Ergos.VIOLET, .55f)
            val dx = l + 34f
            val dy = top + 32f
            pen.diamond(dx, dy, 20f, pen.solid(Ergos.LAV, .14f))
            pen.diamond(dx, dy, 20f, pen.sheen(dy - 20f, dy + 20f, 1.1f))
            pen.text("${s.rank}", dx, dy + 7f, pen.corBold, 20f, pen.sheen(dy - 12f, dy + 8f), Align.CENTER)
            val tx = l + 72f
            pen.text(s.gift.name, tx, top + 30f, pen.corBold, 20f, pen.sheen(top + 12f, top + 34f))
            pen.text("${s.percent} %", r - 16f, top + 29f, pen.geistSemi, 11f, pen.solid(Ergos.LAV_HI), Align.RIGHT)
            var yy = top + 48f
            def.forEach { pen.text(it, tx, yy, pen.geist, 9.5f, pen.solid(Ergos.INK2)); yy += 13.5f }
            yy += 3f
            refs.forEach { pen.text(it, tx, yy, pen.geistMed, 8f, pen.solid(Ergos.ACCENT)); yy += 11f }
            y = top + h + 10f
        }

        // Subordinados: casillas de a 3
        if (result.subordinate.isNotEmpty()) {
            section("Tus dones subordinados", 46f)
            val gap = 8f
            val w = (W - 2 * m - gap * 2) / 3f
            result.subordinate.chunked(3).forEach { row ->
                need(46f)
                row.forEachIndexed { i, s ->
                    val x = m + i * (w + gap)
                    pen.card(RectF(x, y, x + w, y + 38f), Ergos.SURFACE, .9f, Ergos.LINE_STRONG, .8f)
                    val ns = pen.fitSize(s.gift.name, pen.corBold, 13.5f, w - 12f)
                    pen.text(s.gift.name, x + w / 2, y + 17f, pen.corBold, ns, pen.solid(Ergos.VIOLET_HI), Align.CENTER)
                    pen.text("${s.percent} %", x + w / 2, y + 30f, pen.geistMed, 8.5f, pen.solid(Ergos.INK2), Align.CENTER)
                }
                y += 46f
            }
        }

        // Curva completa: cada don con 15 casillas (5 preguntas x 3 puntos), agrupado por nivel.
        // La sección va entera en una página si cabe.
        run {
            val others = result.ranking.filter { it !in result.dominant && it !in result.subordinate }
            val groups = listOf(
                Triple("Dominantes", result.dominant, 0),
                Triple("Subordinados", result.subordinate, 1),
                Triple("Tus otros dones", others, 2),
            ).filter { it.second.isNotEmpty() }
            val row = 19f
            section("Tu curva completa", minOf(14f + groups.size * 22f + result.ranking.size * row, H - 160f))
            val c = pen.canvas
            val nx = m + 26f
            val bx = m + 172f
            val pips = 15
            val gap = 3f
            val pw = (W - m - 46f - bx - gap * (pips - 1)) / pips
            pen.text("0", bx, y + 2f, pen.geist, 7f, pen.solid(Ergos.MUTED))
            pen.text("Puntaje de 15", bx + (pw + gap) * pips - gap, y + 2f, pen.geist, 7f, pen.solid(Ergos.MUTED), Align.RIGHT)
            y += 10f
            for ((label, list, tier) in groups) {
                need(26f + row)
                y += 8f
                val caps = label.uppercase(Locale("es"))
                pen.text(caps, m, y + 6f, pen.geistSemi, 7.5f, pen.solid(if (tier < 2) Ergos.ACCENT else Ergos.MUTED), Align.LEFT, 2f)
                c.drawLine(m + pen.measure(caps, pen.geistSemi, 7.5f, 2f) + 10f, y + 3f, W - m, y + 3f, pen.solid(Ergos.LINE, 1f, .6f))
                y += 12f
                if (tier == 0) {
                    c.drawRoundRect(RectF(m - 8f, y - 3f, W - m + 8f, y + list.size * row + 1f), 8f, 8f, pen.solid(Ergos.VIOLET, .10f))
                }
                for (s in list) {
                    need(row)
                    val cy = y + 8f
                    val filled = Math.round(s.fraction.toFloat().coerceIn(0f, 1f) * pips)
                    if (tier < 2) {
                        c.drawCircle(m + 8f, cy, 8f, pen.solid(if (tier == 0) Ergos.VIOLET else Ergos.LINE))
                        pen.text("${s.rank}", m + 8f, cy + 3.2f, pen.geistSemi, 8.5f, pen.solid(Ergos.INK), Align.CENTER)
                    } else {
                        pen.text("${s.rank}", m + 8f, cy + 3.2f, pen.geist, 8.5f, pen.solid(Ergos.MUTED), Align.CENTER)
                    }
                    pen.text(
                        s.gift.name, nx, cy + 3.8f,
                        if (tier == 0) pen.corBold else pen.corSemi, if (tier == 0) 13.5f else 12f,
                        pen.solid(if (tier == 2) Ergos.INK2 else Ergos.INK),
                    )
                    val colors = when (tier) {
                        0 -> intArrayOf(Ergos.LAV_PALE, Ergos.VIOLET)
                        1 -> intArrayOf(Ergos.VIOLET_HI, Ergos.VIOLET_BTN)
                        else -> intArrayOf(Ergos.OTHER_HI, Ergos.OTHER_LO)
                    }
                    val fill = pen.solid(Ergos.VIOLET).apply {
                        shader = LinearGradient(0f, cy - 4.5f, 0f, cy + 4.5f, colors, null, Shader.TileMode.CLAMP)
                    }
                    val empty = pen.solid(Ergos.LINE, .9f, .8f)
                    for (i in 0 until pips) {
                        val x = bx + i * (pw + gap)
                        c.drawRoundRect(RectF(x, cy - 4.5f, x + pw, cy + 4.5f), 2.5f, 2.5f, if (i < filled) fill else empty)
                    }
                    pen.text("${s.raw}", W - m, cy + 3.5f, pen.geistSemi, 10f, pen.solid(if (tier == 2) Ergos.INK2 else Ergos.INK), Align.RIGHT)
                    y += row
                }
                y += 2f
            }
        }

        // Ministerios
        if (result.ministries.isNotEmpty()) {
            section("Dónde podrías servir")
            for (sug in result.ministries) {
                val gifts = pen.wrap("Por tus dones: " + sug.matchingGifts.joinToString(", ") { it.name }, pen.geist, 8.5f, W - 2 * m - 16f)
                need(20f + gifts.size * 12f)
                pen.diamond(m + 5f, y + 7f, 3.5f, pen.sheen(y, y + 10f))
                pen.text(sug.ministry.name, m + 16f, y + 11f, pen.corBold, 14f, pen.solid(Ergos.INK))
                var yy = y + 24f
                gifts.forEach { pen.text(it, m + 16f, yy, pen.geist, 8.5f, pen.solid(Ergos.MUTED)); yy += 12f }
                y = yy + 8f
            }
        }

        // Cierre
        val note = pen.wrap(
            "Este resultado es un punto de partida para la oración y la experimentación. " +
                "Confírmalo con personas que te conocen, con tu pastor y con los líderes de tu iglesia.",
            pen.corMed, 12.5f, W - 2 * m - 40f,
        )
        val boxH = 26f + note.size * 16f
        need(boxH + 30f)
        y += 10f
        pen.card(RectF(m, y, W - m, y + boxH), Ergos.DEEP, .55f, null, 0f)
        pen.canvas.drawRoundRect(RectF(m, y, W - m, y + boxH), 8f, 8f, pen.sheen(y, y + boxH, .7f))
        var yy = y + 22f
        note.forEach { pen.text(it, W / 2, yy, pen.corMed, 12.5f, pen.solid(Ergos.INK), Align.CENTER); yy += 16f }
        y += boxH + 14f
        pen.text("Cuestionario Houts modificado por Wagner · contenido v${content.version}", W / 2, y, pen.geist, 7f, pen.solid(Ergos.MUTED), Align.CENTER)

        page?.let { doc.finishPage(it) }
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    // ------------------------------------------------------------------ Certificado

    fun certificate(context: Context, dir: File, name: String, info: ClassInfo, date: LocalDate, code: String): File {
        val file = File(dir, "certificado.pdf")
        val W = 842f
        val H = 595f
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), 1).create())
        val pen = Pen(context)
        pen.canvas = page.canvas
        val c = pen.canvas

        // Fondo con resplandor y damasco de rombos
        pen.background(W, H, W / 2, H * .42f, W * .62f)
        val lattice = pen.solid(Ergos.VIOLET, .07f, .5f)
        var x = -H
        while (x < W + H) { c.drawLine(x, 0f, x + H, H, lattice); c.drawLine(x, H, x + H, 0f, lattice); x += 26f }
        val dot = pen.solid(Ergos.VIOLET_HI, .10f)
        var dy = 13f
        while (dy < H) {
            var dx = 0f
            while (dx < W) { c.drawCircle(dx + if ((dy / 26f).toInt() % 2 == 1) 13f else 0f, dy, .9f, dot); dx += 26f }
            dy += 26f
        }

        // Marcos lavanda y rombos en el centro de cada lado
        c.drawRect(16f, 16f, W - 16f, H - 16f, pen.sheen(0f, H, 2.2f))
        c.drawRect(22f, 22f, W - 22f, H - 22f, pen.sheen(0f, H, .7f).apply { alpha = 230 })
        c.drawRect(27f, 27f, W - 27f, H - 27f, pen.solid(Ergos.VIOLET_HI, .55f, .6f))
        listOf(W / 2 to 16f, W / 2 to H - 16f, 16f to H / 2, W - 16f to H / 2).forEach { (px, py) ->
            pen.diamond(px, py, 6f, pen.solid(Ergos.NIGHT))
            pen.diamond(px, py, 6f, pen.sheen(py - 6f, py + 6f, 1.2f))
            pen.diamond(px, py, 2.4f, pen.sheen(py - 3f, py + 3f))
        }
        pen.corners(W, H) { pen.cornerFlourish() }

        // Logo de la iglesia
        val logoW = 96f
        pen.logo(W / 2 - logoW / 2, 40f, logoW)
        var y = 40f + logoW * ChurchLogo.LOGO_H / ChurchLogo.LOGO_W

        // Título
        y += 44f
        pen.text("CERTIFICADO", W / 2, y, pen.corBold, 40f, pen.sheen(y - 34f, y + 4f), Align.CENTER, 9f)
        y += 20f
        pen.text("DE PARTICIPACIÓN", W / 2, y, pen.geistSemi, 9.5f, pen.solid(Ergos.ACCENT), Align.CENTER, 4.5f)
        y += 14f
        pen.ornament(W / 2, y, 130f)

        // Nombre en la letra del logo
        y += 30f
        pen.text("Se otorga con alegría y gratitud a", W / 2, y, pen.corMed, 16f, pen.solid(Ergos.INK2), Align.CENTER)
        val ns = pen.fitSize(name, pen.fraktur, 48f, W - 220f)
        y += ns + 10f
        pen.text(name, W / 2 + 1.5f, y + 2f, pen.fraktur, ns, pen.solid(Ergos.VIOLET_LO, .8f), Align.CENTER)
        val namePaint = pen.solid(Ergos.INK).apply {
            shader = LinearGradient(0f, y - ns, 0f, y + 6f, intArrayOf(Ergos.WHITE, Ergos.LAV_PALE, Ergos.VIOLET_HI), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
        }
        pen.text(name, W / 2, y, pen.fraktur, ns, namePaint, Align.CENTER)
        y += 14f
        run {
            val half = max(160f, pen.measure(name, pen.fraktur, ns) / 2 + 20f)
            val p = pen.sheen(y - 6f, y + 6f, .9f)
            val sw = Path().apply {
                moveTo(W / 2 - half, y); cubicTo(W / 2 - half * .5f, y - 5f, W / 2 - 30f, y + 5f, W / 2 - 8f, y)
                moveTo(W / 2 + 8f, y); cubicTo(W / 2 + 30f, y + 5f, W / 2 + half * .5f, y - 5f, W / 2 + half, y)
            }
            c.drawPath(sw, p)
            pen.diamond(W / 2, y, 4f, pen.sheen(y - 4f, y + 4f))
            listOf(-1f, 1f).forEach { s -> c.drawPath(spiral(W / 2 + s * (half + 5f), y - 3f, 4.5f, 1.3f, s, PI.toFloat() * .5f), p) }
        }

        // Texto y felicitaciones
        y += 28f
        pen.text("por haber completado con fidelidad el curso «${info.courseName}»,", W / 2, y, pen.corMed, 15.5f, pen.solid(Ergos.INK), Align.CENTER)
        y += 20f
        pen.text("dando un paso para conocer los dones que Dios le confió para servir a Su iglesia.", W / 2, y, pen.corMed, 15.5f, pen.solid(Ergos.INK), Align.CENTER)
        y += 32f
        pen.text("¡Felicitaciones!", W / 2, y, pen.corBold, 24f, pen.sheen(y - 20f, y + 4f), Align.CENTER)
        y += 20f
        pen.text("«Cada uno según el don que ha recibido, minístrelo a los otros,", W / 2, y, pen.corMed, 12.5f, pen.solid(Ergos.ACCENT), Align.CENTER)
        y += 15f
        pen.text("como buenos administradores de la multiforme gracia de Dios.»  1 Pedro 4:10", W / 2, y, pen.corMed, 12.5f, pen.solid(Ergos.ACCENT), Align.CENTER)

        // Pie: fecha · sello · QR
        val by = H - 128f
        val leftX = 200f
        pen.text("FECHA", leftX, by + 4f, pen.geistSemi, 7.5f, pen.solid(Ergos.LAV), Align.CENTER, 2.5f)
        pen.text(date.format(spanishDate), leftX, by + 22f, pen.corSemi, 14f, pen.solid(Ergos.INK), Align.CENTER)
        c.drawLine(leftX - 60f, by + 30f, leftX + 60f, by + 30f, pen.sheen(by, by + 40f, .6f))
        pen.text(info.churchName, leftX, by + 44f, pen.corMed, 11f, pen.solid(Ergos.MUTED), Align.CENTER)

        pen.seal(W / 2, by + 18f, 38f)

        val qrSize = 62f
        val qx = W - 200f - qrSize / 2
        val qy = by - 16f
        val box = RectF(qx - 3f, qy - 3f, qx + qrSize + 3f, qy + qrSize + 3f)
        c.drawRoundRect(box, 4f, 4f, pen.solid(Ergos.INK))
        c.drawRoundRect(box, 4f, 4f, pen.sheen(qy, qy + qrSize, 1.2f))
        pen.qr(Church.VERIFY_URL + code, qx, qy, qrSize)
        pen.text("VERIFICA ESTE CERTIFICADO", W - 200f, qy + qrSize + 16f, pen.geistSemi, 6.5f, pen.solid(Ergos.LAV), Align.CENTER, 1.6f)
        pen.text(code, W - 200f, qy + qrSize + 28f, pen.geistSemi, 9f, pen.solid(Ergos.INK), Align.CENTER, 1.2f)

        pen.text("${Church.footer}, un curso para conocer tus dones espirituales", W / 2, H - 36f, pen.geist, 7f, pen.solid(Ergos.MUTED), Align.CENTER)

        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}

// ------------------------------------------------------------------ Herramientas de dibujo

private object Ergos {
    const val NIGHT = 0xFF0D0919.toInt()
    const val NIGHT2 = 0xFF1A1033.toInt()
    const val SURFACE = 0xFF171226.toInt()
    const val SURFACE2 = 0xFF211A3A.toInt()
    const val LINE = 0xFF3A2F5A.toInt()
    const val LINE_STRONG = 0xFF6B58A3.toInt()
    const val DEEP = 0xFF2A1458.toInt()
    const val VIOLET = 0xFF9450FF.toInt()
    const val VIOLET_HI = 0xFFB58BFF.toInt()
    const val ACCENT = 0xFFC4A2FF.toInt()
    const val LAV = 0xFFC4A2FF.toInt()
    const val LAV_HI = 0xFFEFE6FF.toInt()
    const val LAV_LO = 0xFF8C52F6.toInt()
    const val LAV_PALE = 0xFFE6D8FF.toInt()
    const val VIOLET_LO = 0xFF6A2FE0.toInt()
    const val VIOLET_BTN = 0xFF7134EC.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val OTHER_HI = 0xFF8E7BC4.toInt()
    const val OTHER_LO = 0xFF5B4A8E.toInt()
    const val INK = 0xFFF6F6FB.toInt()
    const val INK2 = 0xFFDCDBE8.toInt()
    const val MUTED = 0xFFB3AFCC.toInt()

    fun withAlpha(color: Int, a: Float): Int = (color and 0x00FFFFFF) or ((a * 255).roundToInt().coerceIn(0, 255) shl 24)
}

private enum class Align { LEFT, CENTER, RIGHT }

/** Espiral que se cierra (las volutas de las florituras). */
private fun spiral(cx: Float, cy: Float, r0: Float, turns: Float, dir: Float, start: Float): Path {
    val p = Path()
    val n = (60 * turns).toInt()
    for (i in 0..n) {
        val t = i.toFloat() / n
        val a = start + dir * t * turns * 2f * PI.toFloat()
        val r = r0 * (1f - .82f * t)
        val x = cx + r * cos(a)
        val y = cy + r * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    return p
}

private class Pen(private val context: Context) {
    lateinit var canvas: Canvas

    private fun font(id: Int): Typeface = ResourcesCompat.getFont(context, id) ?: Typeface.SERIF
    val corBold = font(R.font.cormorant_bold)
    val corSemi = font(R.font.cormorant_semibold)
    val corMed = font(R.font.cormorant_medium)
    val geist = font(R.font.geist_regular)
    val geistMed = font(R.font.geist_medium)
    val geistSemi = font(R.font.geist_semibold)
    val fraktur = font(R.font.unifraktur_maguntia)

    private val logoPath: Path by lazy {
        PathParser.createPathFromPathData(ChurchLogo.pathData(context, ChurchLogo.LOGO_FILE)).apply { fillType = Path.FillType.EVEN_ODD }
    }
    private val emblemPath: Path by lazy {
        PathParser.createPathFromPathData(ChurchLogo.pathData(context, ChurchLogo.EMBLEM_FILE)).apply { fillType = Path.FillType.EVEN_ODD }
    }

    fun solid(color: Int, alpha: Float = 1f, stroke: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Ergos.withAlpha(color, alpha * ((color ushr 24) / 255f))
        if (stroke > 0f) {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    /** Oro con degradado vertical entre y0 e y1. */
    fun sheen(y0: Float, y1: Float, stroke: Float = 0f) = solid(Ergos.LAV, 1f, stroke).apply {
        shader = LinearGradient(0f, y0, 0f, y1, intArrayOf(Ergos.LAV_HI, Ergos.LAV, Ergos.LAV_LO), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
    }

    fun measure(text: String, face: Typeface, size: Float, spacing: Float = 0f): Float {
        val p = Paint().apply { typeface = face; textSize = size }
        return p.measureText(text) + spacing * max(0, text.length - 1)
    }

    /** Achica la letra hasta que el texto quepa en [width]. */
    fun fitSize(text: String, face: Typeface, size: Float, width: Float): Float {
        var s = size
        while (s > 8f && measure(text, face, s) > width) s -= 1f
        return s
    }

    fun text(s: String, x: Float, y: Float, face: Typeface, size: Float, paint: Paint, align: Align = Align.LEFT, spacing: Float = 0f) {
        paint.typeface = face
        paint.textSize = size
        val w = measure(s, face, size, spacing)
        var x0 = when (align) { Align.LEFT -> x; Align.CENTER -> x - w / 2; Align.RIGHT -> x - w }
        if (spacing == 0f) { canvas.drawText(s, x0, y, paint); return }
        for (ch in s) {
            val t = ch.toString()
            canvas.drawText(t, x0, y, paint)
            x0 += paint.measureText(t) + spacing
        }
    }

    fun wrap(text: String, face: Typeface, size: Float, width: Float): List<String> {
        val lines = mutableListOf<String>()
        for (paragraph in text.split("\n")) {
            var current = ""
            for (word in paragraph.split(" ")) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (measure(candidate, face, size) <= width || current.isEmpty()) current = candidate
                else { lines += current; current = word }
            }
            lines += current
        }
        return lines
    }

    fun diamond(x: Float, y: Float, r: Float, paint: Paint) {
        canvas.drawPath(Path().apply { moveTo(x, y - r); lineTo(x + r, y); lineTo(x, y + r); lineTo(x - r, y); close() }, paint)
    }

    fun card(rect: RectF, fill: Int, fillAlpha: Float, border: Int?, borderAlpha: Float) {
        canvas.drawRoundRect(rect, 8f, 8f, solid(fill, fillAlpha))
        if (border != null) canvas.drawRoundRect(rect, 8f, 8f, solid(border, borderAlpha, .8f))
    }

    fun background(w: Float, h: Float, gx: Float, gy: Float, radius: Float) {
        canvas.drawRect(0f, 0f, w, h, solid(Ergos.NIGHT))
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                gx, gy, radius,
                intArrayOf(Ergos.withAlpha(Ergos.DEEP, .95f), Ergos.withAlpha(Ergos.NIGHT2, .6f), Ergos.NIGHT),
                floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, h, glow)
    }

    fun frame(w: Float, h: Float, outer: Float, outerWidth: Float, inner: Float) {
        canvas.drawRect(outer, outer, w - outer, h - outer, sheen(0f, h, outerWidth))
        canvas.drawRect(inner, inner, w - inner, h - inner, solid(Ergos.VIOLET_HI, .45f, .5f))
    }

    /** Dibuja [art] en las 4 esquinas y su reflejo sobre la diagonal (para cubrir ambos bordes). */
    fun corners(w: Float, h: Float, art: () -> Unit) {
        val diagonal = Matrix().apply { setValues(floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)) }
        listOf(floatArrayOf(1f, 1f, 0f, 0f), floatArrayOf(-1f, 1f, w, 0f), floatArrayOf(1f, -1f, 0f, h), floatArrayOf(-1f, -1f, w, h)).forEach { (sx, sy, tx, ty) ->
            for (flip in listOf(false, true)) {
                canvas.save()
                canvas.translate(tx, ty)
                canvas.scale(sx, sy)
                if (flip) canvas.concat(diagonal)
                art()
                canvas.restore()
            }
        }
    }

    private fun leaf(x: Float, y: Float, len: Float, ang: Float, wid: Float): Path {
        val ca = cos(ang)
        val sa = sin(ang)
        fun px(u: Float, v: Float) = x + u * ca - v * sa
        fun py(u: Float, v: Float) = y + u * sa + v * ca
        return Path().apply {
            moveTo(px(0f, 0f), py(0f, 0f))
            cubicTo(px(len * .3f, -wid), py(len * .3f, -wid), px(len * .75f, -wid * .8f), py(len * .75f, -wid * .8f), px(len, 0f), py(len, 0f))
            cubicTo(px(len * .75f, wid * .5f), py(len * .75f, wid * .5f), px(len * .3f, wid * .7f), py(len * .3f, wid * .7f), px(0f, 0f), py(0f, 0f))
            close()
        }
    }

    /** Floritura renacentista de esquina: brazos en S, volutas, hojas de acanto y perlas. */
    fun cornerFlourish() {
        val g = sheen(30f, 140f, 1.3f)
        val gf = sheen(30f, 140f)
        val thin = sheen(30f, 140f, .7f)
        canvas.drawPath(Path().apply { moveTo(36f, 36f); cubicTo(60f, 30f, 90f, 58f, 118f, 46f); cubicTo(134f, 39f, 140f, 52f, 132f, 58f) }, g)
        canvas.drawPath(spiral(126f, 50f, 9f, 1.6f, 1f, PI.toFloat() * .35f), g)
        canvas.drawPath(Path().apply { moveTo(44f, 44f); cubicTo(70f, 50f, 76f, 78f, 98f, 76f); cubicTo(108f, 75f, 110f, 66f, 103f, 64f) }, thin)
        canvas.drawPath(spiral(104f, 69f, 5.5f, 1.4f, -1f, PI.toFloat() * 1.2f), thin)
        listOf(
            floatArrayOf(58f, 40f, 16f, -.9f, 4.5f), floatArrayOf(80f, 47f, 14f, -1.2f, 4f), floatArrayOf(96f, 51f, 12f, -1.5f, 3.5f),
            floatArrayOf(66f, 52f, 12f, .55f, 3.5f), floatArrayOf(86f, 62f, 10f, .9f, 3f),
        ).forEach { (lx, ly, len, ang, wid) -> canvas.drawPath(leaf(lx, ly, len, ang, wid), gf) }
        canvas.drawCircle(150f, 42f, 1.8f, gf)
        canvas.drawCircle(160f, 42f, 1.3f, gf)
        canvas.drawCircle(168f, 42f, 1f, gf)
        canvas.drawCircle(42f, 42f, 9f, thin)
        diamond(42f, 42f, 6.5f, gf)
        diamond(42f, 42f, 2.4f, solid(Ergos.NIGHT))
    }

    /** Versión pequeña para las páginas de "Mis dones". */
    fun cornerMini() {
        canvas.drawPath(Path().apply { moveTo(30f, 30f); cubicTo(48f, 26f, 66f, 44f, 86f, 36f) }, sheen(20f, 90f, 1f))
        canvas.drawPath(spiral(90f, 40f, 6f, 1.5f, 1f, PI.toFloat() * 1.1f), sheen(20f, 90f, 1f))
        diamond(30f, 30f, 4.5f, sheen(20f, 40f))
        canvas.drawCircle(30f, 30f, 7f, sheen(20f, 40f, .6f))
    }

    /** Separador de Ergos con volutas: línea, perlas, rombo. */
    fun ornament(cx: Float, cy: Float, half: Float) {
        val p = sheen(cy - 5f, cy + 5f, .8f)
        canvas.drawLine(cx - half, cy, cx - 22f, cy, p)
        canvas.drawLine(cx + 22f, cy, cx + half, cy, p)
        canvas.drawCircle(cx - 14f, cy, 1.8f, sheen(cy - 2f, cy + 2f))
        canvas.drawCircle(cx + 14f, cy, 1.8f, sheen(cy - 2f, cy + 2f))
        diamond(cx, cy, 5f, sheen(cy - 5f, cy + 5f))
        listOf(-1f, 1f).forEach { s -> canvas.drawPath(spiral(cx + s * (half + 5f), cy - 3.2f, 4f, 1.2f, s, PI.toFloat() * .5f), p) }
    }

    /** Logo de la iglesia en oro, con su ancho [width]. */
    fun logo(x: Float, y: Float, width: Float) {
        val s = width / ChurchLogo.LOGO_W
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(s, s)
        canvas.drawPath(logoPath, sheen(0f, ChurchLogo.LOGO_H))
        canvas.restore()
    }

    /** Sello circular con el emblema y el texto alrededor. */
    fun seal(cx: Float, cy: Float, r: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy - 10f, r * 1.2f, intArrayOf(Ergos.DEEP, Ergos.NIGHT), null, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, r, fill)
        canvas.drawCircle(cx, cy, r, sheen(cy - r, cy + r, 1.6f))
        canvas.drawCircle(cx, cy, r - 4f, sheen(cy - r, cy + r, .6f))
        canvas.drawCircle(cx, cy, r - 15f, sheen(cy - r, cy + r, .6f))
        // Texto en el anillo, centrado arriba
        val ring = "LLAMADOS  ·  SAN ANDRÉS  ·  DONES ESPIRITUALES  ·"
        val rr = r - 12.5f
        val tp = sheen(cy - r, cy + r).apply { typeface = geistSemi; textSize = 6.2f; letterSpacing = .08f }
        val circle = Path().apply { addCircle(cx, cy, rr, Path.Direction.CW) }
        val length = (2 * PI * rr).toFloat()
        val tw = tp.measureText(ring)
        // El círculo empieza a la derecha (0°); arriba es 3/4 del recorrido.
        val start = (length * .75f - tw / 2 + length) % length
        canvas.drawTextOnPath(ring, circle, start, 0f, tp)
        // Emblema
        val es = 34f / 223f
        canvas.save()
        canvas.translate(cx - 17f, cy - 182f * es / 2f)
        canvas.scale(es, es)
        canvas.translate(-40f, -6f)
        canvas.drawPath(emblemPath, sheen(0f, 200f))
        canvas.restore()
    }

    /** Código QR en noche sobre fondo claro (así lo lee cualquier cámara). */
    fun qr(content: String, x: Float, y: Float, size: Float) {
        val matrix = QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
        )
        val n = matrix.width
        val cell = size / (n + 2)
        val p = solid(Ergos.NIGHT)
        for (r in 0 until n) for (k in 0 until n) {
            if (matrix.get(k, r)) canvas.drawRect(x + (k + 1) * cell, y + (r + 1) * cell, x + (k + 2) * cell + .05f, y + (r + 2) * cell + .05f, p)
        }
    }
}
