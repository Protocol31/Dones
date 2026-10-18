package com.dones.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.dones.app.ui.ChurchLogo
import com.dones.core.ClassInfo
import com.dones.core.GiftScore
import com.dones.core.HoutsContent
import com.dones.core.HoutsResult
import com.dones.core.MinistrySuggestion
import com.dones.core.ParticipantState
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** Textos largos de los PDF (assets/dones-detalle.json y assets/ministerios-detalle.json). */
internal class PdfTexts(context: Context) {
    data class Character(val name: String, val ref: String, val quote: String, val text: String)
    data class GiftText(val life: String, val traits: List<String>, val serve: String, val care: String, val character: Character?)
    data class MinistryText(val what: String, val you: String, val refs: List<String>, val bible: String)

    val gifts: Map<String, GiftText>
    val ministries: Map<String, MinistryText>

    init {
        fun read(file: String, key: String): JSONObject? = runCatching {
            JSONObject(context.assets.open(file).bufferedReader().use { it.readText() }).getJSONObject(key)
        }.getOrNull()

        fun JSONObject.strings(key: String): List<String> =
            optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()

        gifts = buildMap {
            val o = read("dones-detalle.json", "gifts") ?: return@buildMap
            for (code in o.keys()) {
                val g = o.getJSONObject(code)
                val ch = g.optJSONObject("character")?.let {
                    Character(it.optString("name"), it.optString("ref"), it.optString("quote"), it.optString("text"))
                }
                put(code, GiftText(g.optString("life"), g.strings("traits"), g.optString("serve"), g.optString("care"), ch))
            }
        }
        ministries = buildMap {
            val o = read("ministerios-detalle.json", "ministries") ?: return@buildMap
            for (name in o.keys()) {
                val m = o.getJSONObject(name)
                put(name, MinistryText(m.optString("what"), m.optString("you"), m.strings("refs"), m.optString("bible")))
            }
        }
    }
}

/**
 * "Mis dones" en hojas fijas:
 * 1 dominantes · 2 subordinados · 3 curva completa · 4 dónde servir · 5 personaje bíblico.
 * Cada hoja achica un poco la letra si el contenido no cabe (por ejemplo, con empates).
 */
internal object ResultsPdf {

    private const val W = 595f
    private const val H = 842f
    private const val M = 58f
    private const val BOTTOM = H - 58f
    private val spanishDate = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es"))

    fun build(
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
        val texts = PdfTexts(context)
        val name = participant.name
        val totalPages = if (result.subordinate.isEmpty()) 4 else 5
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun newPage(glowY: Float = 0f) {
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), pageNo).create())
            page = p
            pen.canvas = p.canvas
            pen.background(W, H, W / 2f, glowY, W * .9f)
            pen.frame(W, H, 16f, 1.2f, 21f)
            pen.corners(W, H) { pen.cornerMini() }
            pen.text("${Church.footer}  ·  Página $pageNo de $totalPages", W / 2, H - 30, pen.geist, 7f, pen.solid(Ergos.MUTED), Align.CENTER)
            y = 52f
        }

        /** Encabezado de las hojas 2 en adelante. */
        fun pageHead(title: String, sub: String?) {
            pen.text("Mis dones espirituales  ·  $name".uppercase(Locale("es")), W / 2, 46f, pen.geistSemi, 6.8f, pen.solid(Ergos.MUTED), Align.CENTER, 1.6f)
            y = 84f
            pen.text(title, W / 2, y, pen.corBold, 28f, pen.sheen(y - 22f, y + 4f), Align.CENTER)
            y += 14f
            pen.ornament(W / 2, y, 90f)
            y += 20f
            if (sub != null) {
                pen.wrap(sub, pen.corMed, 12.5f, W - 2 * M - 40f).forEach {
                    pen.text(it, W / 2, y, pen.corMed, 12.5f, pen.solid(Ergos.INK2), Align.CENTER)
                    y += 15.5f
                }
                y += 6f
            }
        }

        // ---------------------------------------------------------------- Hoja 1: dominantes
        newPage()
        val logoW = 46f
        pen.logo(W / 2 - logoW / 2, 36f, logoW)
        y = 36f + logoW * ChurchLogo.LOGO_H / ChurchLogo.LOGO_W + 16f
        pen.text(
            "LLAMADOS  ·  ${(info?.courseName ?: "Descubriendo Mi Ministerio").uppercase(Locale("es"))}",
            W / 2, y, pen.geistSemi, 6.8f, pen.solid(Ergos.ACCENT), Align.CENTER, 2.2f,
        )
        y += 28f
        pen.text("Mis dones espirituales", W / 2, y, pen.corBold, 28f, pen.sheen(y - 22f, y + 4f), Align.CENTER)
        y += 27f
        pen.text(name, W / 2, y, pen.fraktur, fit(pen, name, pen.fraktur, 21f, W - 2 * M), pen.solid(Ergos.INK), Align.CENTER)
        y += 16f
        pen.text(
            listOf(info?.churchName ?: Church.NAME, LocalDate.now().format(spanishDate)).joinToString("  ·  "),
            W / 2, y, pen.corMed, 10.5f, pen.solid(Ergos.MUTED), Align.CENTER,
        )
        y += 22f
        pen.text("TUS DONES DOMINANTES", W / 2, y, pen.geistSemi, 8.5f, pen.solid(Ergos.ACCENT), Align.CENTER, 3f)
        y += 11f
        pen.ornament(W / 2, y, 90f)
        y += 14f
        y = giftCards(pen, texts, result.dominant, y, tier = 0)

        // ---------------------------------------------------------------- Hoja 2: subordinados
        if (result.subordinate.isNotEmpty()) {
            newPage()
            pageHead(
                "Tus dones subordinados",
                "Acompañan y fortalecen a tus dones dominantes. Son herramientas que Dios también puso en tus manos.",
            )
            y = giftCards(pen, texts, result.subordinate, y, tier = 1)
        }

        // ---------------------------------------------------------------- Hoja 3: curva completa
        newPage()
        pageHead("Tu curva completa", null)
        curve(pen, result, y)

        // ---------------------------------------------------------------- Hoja 4: dónde servir
        newPage()
        pageHead(
            "Dónde podrías servir",
            "Estos ministerios de la iglesia encajan con tus dones. Conversa con tu pastor para dar el primer paso.",
        )
        ministries(pen, texts, result.ministries, y)

        // ---------------------------------------------------------------- Hoja 5: personaje bíblico
        newPage(glowY = 250f)
        character(context, pen, texts, result)

        page?.let { doc.finishPage(it) }
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun fit(pen: Pen, s: String, face: Typeface, size: Float, width: Float): Float {
        var v = size
        while (v > 6f && pen.measure(s, face, v) > width) v -= .5f
        return v
    }

    private fun label(pen: Pen, s: String, x: Float, y: Float, color: Int = Ergos.ACCENT) =
        pen.text(s.uppercase(Locale("es")), x, y, pen.geistSemi, 6.8f, pen.solid(color), Align.LEFT, 1.6f)

    private fun rrect(pen: Pen, l: Float, t: Float, r: Float, b: Float, rad: Float, paint: Paint) =
        pen.canvas.drawRoundRect(RectF(l, t, r, b), rad, rad, paint)

    private fun vertical(paint: Paint, y0: Float, y1: Float, colors: IntArray) = paint.apply {
        shader = LinearGradient(0f, y0, 0f, y1, colors, null, Shader.TileMode.CLAMP)
    }

    // ------------------------------------------------------------------ Tarjetas de dones

    /** Alto de la tarjeta con escala de letra k; si draw, además la dibuja en top. */
    private fun giftCard(pen: Pen, texts: PdfTexts, s: GiftScore, top: Float, k: Float, draw: Boolean, tier: Int): Float {
        val l = M
        val r = W - M
        val tx = l + 66f
        val tw = r - 16f - tx
        val d = texts.gifts[s.gift.code]
        val def = pen.wrap(s.gift.definition, pen.corMed, 11.5f * k, tw)
        val life = d?.let { pen.wrap(it.life, pen.geist, 9.3f * k, tw) } ?: emptyList()
        val colW = (tw - 14f) / 2f
        val serve = d?.let { pen.wrap(it.serve, pen.geist, 8.6f * k, colW) } ?: emptyList()
        val care = d?.let { pen.wrap(it.care, pen.geist, 8.6f * k, colW) } ?: emptyList()
        val refs = pen.wrap(s.gift.refs.joinToString("  ·  "), pen.geistMed, 7.8f * k, tw)
        val lh = 12.6f * k
        val lh2 = 11.4f * k
        val hasDetail = d != null
        val h = 34f * k + def.size * 14f * k + 6f * k +
            (if (hasDetail) 9f * k + life.size * lh + 8f * k + 18f * k + 10f * k + 9f * k + max(serve.size, care.size) * lh2 + 6f * k else 0f) +
            refs.size * 10.5f * k + 12f * k
        if (!draw) return h

        val c = pen.canvas
        rrect(pen, l, top, r, top + h, 10f, pen.solid(if (tier == 0) Ergos.SURFACE2 else Ergos.SURFACE, .92f))
        rrect(pen, l, top, r, top + h, 10f, pen.solid(if (tier == 0) Ergos.VIOLET else Ergos.LINE_STRONG, .6f, .8f))
        rrect(pen, l, top, l + 54f, top + h, 10f, vertical(pen.solid(Ergos.VIOLET), top, top + h, intArrayOf(Ergos.withAlpha(Ergos.VIOLET_HI, .35f), Ergos.withAlpha(Ergos.VIOLET, .05f))))
        val dx = l + 27f
        val dy = top + 30f * k
        pen.diamond(dx, dy, 17f, pen.solid(Ergos.NIGHT, .5f))
        pen.diamond(dx, dy, 17f, pen.sheen(dy - 17f, dy + 17f, 1.1f))
        pen.text("${s.rank}", dx, dy + 6f, pen.corBold, 18f, pen.sheen(dy - 10f, dy + 7f), Align.CENTER)

        var yy = top + 26f * k
        pen.text(s.gift.name, tx, yy + 2f * k, pen.corBold, 21f * k, pen.sheen(yy - 16f * k, yy + 4f * k))
        pen.text("${s.percent} %", r - 16f, yy, pen.geistSemi, 11f * k, pen.solid(Ergos.LAV_HI), Align.RIGHT)
        yy += 8f * k
        def.forEach { yy += 14f * k; pen.text(it, tx, yy - 3f * k, pen.corMed, 11.5f * k, pen.solid(Ergos.INK2)) }
        yy += 6f * k
        if (d != null) {
            label(pen, "Así se ve en tu vida", tx, yy + 4f * k)
            yy += 9f * k
            life.forEach { yy += lh; pen.text(it, tx, yy - 3f * k, pen.geist, 9.3f * k, pen.solid(Ergos.INK)) }
            yy += 8f * k
            var px = tx
            for (t in d.traits) {
                val w = pen.measure(t, pen.geistMed, 8.2f * k) + 16f
                rrect(pen, px, yy, px + w, yy + 16f * k, 8f * k, pen.solid(Ergos.VIOLET, .2f))
                rrect(pen, px, yy, px + w, yy + 16f * k, 8f * k, pen.solid(Ergos.VIOLET_HI, .45f, .6f))
                pen.text(t, px + 8f, yy + 11f * k, pen.geistMed, 8.2f * k, pen.solid(Ergos.INK))
                px += w + 6f
            }
            yy += 28f * k
            label(pen, "Cómo puedes servir", tx, yy)
            label(pen, "Cuida tu don", tx + colW + 14f, yy)
            yy += 2f * k
            var y1 = yy
            var y2 = yy
            serve.forEach { y1 += lh2; pen.text(it, tx, y1, pen.geist, 8.6f * k, pen.solid(Ergos.INK2)) }
            care.forEach { y2 += lh2; pen.text(it, tx + colW + 14f, y2, pen.geist, 8.6f * k, pen.solid(Ergos.INK2)) }
            c.drawLine(tx + colW + 7f, yy - 6f, tx + colW + 7f, max(y1, y2) + 2f, pen.solid(Ergos.LINE, 1f, .6f))
            yy = max(y1, y2) + 6f * k
        }
        yy += 9f * k
        refs.forEach { pen.text(it, tx, yy, pen.geistMed, 7.8f * k, pen.solid(Ergos.ACCENT)); yy += 10.5f * k }
        return h
    }

    private fun giftCards(pen: Pen, texts: PdfTexts, list: List<GiftScore>, startY: Float, tier: Int): Float {
        val gap = 10f
        val avail = BOTTOM - startY
        var k = 1f
        while (k > .62f && list.sumOf { giftCard(pen, texts, it, 0f, k, false, tier).toDouble() + gap } - gap > avail) k -= .02f
        var y = startY
        for (s in list) y += giftCard(pen, texts, s, y, k, true, tier) + gap
        return y
    }

    // ------------------------------------------------------------------ Curva completa

    private fun curve(pen: Pen, result: HoutsResult, startY: Float) {
        val c = pen.canvas
        var y = startY
        val others = result.ranking.filter { it !in result.dominant && it !in result.subordinate }
        val groups = listOf(
            Triple("Dominantes", result.dominant, 0),
            Triple("Subordinados", result.subordinate, 1),
            Triple("Tus otros dones", others, 2),
        ).filter { it.second.isNotEmpty() }

        val expl = "Cada casilla es un punto: cada don se midió con 5 frases y cada una vale hasta 3 puntos, así que el máximo es 15. " +
            "Tus dones dominantes son los que Dios ha puesto con más fuerza en ti; los subordinados los acompañan. " +
            "Los dones con pocas casillas no son una falla: allí otros miembros del cuerpo te completan, porque " +
            "«ni el ojo puede decir a la mano: No te necesito» (1 Co 12:21)."
        val lines = pen.wrap(expl, pen.geist, 9f, W - 2 * M - 36f)
        val boxH = 30f + lines.size * 12.5f
        val boxTop = BOTTOM - boxH

        // Alto de fila según el espacio que queda sobre la explicación
        val fixed = 6f + groups.size * 19f
        val row = ((boxTop - 14f - y - fixed) / result.ranking.size).coerceIn(12f, 17.5f)
        val nx = M + 26f
        val bx = M + 172f
        val pips = 15
        val gap = 3f
        val pw = (W - M - 46f - bx - gap * (pips - 1)) / pips
        pen.text("0", bx, y, pen.geist, 7f, pen.solid(Ergos.MUTED))
        pen.text("Puntaje de 15", bx + (pw + gap) * pips - gap, y, pen.geist, 7f, pen.solid(Ergos.MUTED), Align.RIGHT)
        y += 6f
        for ((title, list, tier) in groups) {
            y += 8f
            val caps = title.uppercase(Locale("es"))
            pen.text(caps, M, y + 6f, pen.geistSemi, 7.5f, pen.solid(if (tier < 2) Ergos.ACCENT else Ergos.MUTED), Align.LEFT, 2f)
            c.drawLine(M + pen.measure(caps, pen.geistSemi, 7.5f, 2f) + 10f, y + 3f, W - M, y + 3f, pen.solid(Ergos.LINE, 1f, .6f))
            y += 11f
            if (tier == 0) rrect(pen, M - 8f, y - 2f, W - M + 8f, y + list.size * row, 8f, pen.solid(Ergos.VIOLET, .10f))
            for (s in list) {
                val cy = y + row / 2f - .75f
                val filled = (s.fraction.toFloat().coerceIn(0f, 1f) * pips).roundToInt()
                if (tier < 2) {
                    c.drawCircle(M + 8f, cy, 7.5f, pen.solid(if (tier == 0) Ergos.VIOLET else Ergos.LINE))
                    pen.text("${s.rank}", M + 8f, cy + 3f, pen.geistSemi, 8f, pen.solid(Ergos.INK), Align.CENTER)
                } else {
                    pen.text("${s.rank}", M + 8f, cy + 3f, pen.geist, 8f, pen.solid(Ergos.MUTED), Align.CENTER)
                }
                pen.text(
                    s.gift.name, nx, cy + 3.8f,
                    if (tier == 0) pen.corBold else pen.corSemi, if (tier == 0) 13f else 11.5f,
                    pen.solid(if (tier == 2) Ergos.INK2 else Ergos.INK),
                )
                val colors = when (tier) {
                    0 -> intArrayOf(Ergos.LAV_PALE, Ergos.VIOLET)
                    1 -> intArrayOf(Ergos.VIOLET_HI, Ergos.VIOLET_BTN)
                    else -> intArrayOf(Ergos.OTHER_HI, Ergos.OTHER_LO)
                }
                val fill = vertical(pen.solid(Ergos.VIOLET), cy - 4.5f, cy + 4.5f, colors)
                val empty = pen.solid(Ergos.LINE, .9f, .8f)
                for (i in 0 until pips) {
                    val x = bx + i * (pw + gap)
                    rrect(pen, x, cy - 4.2f, x + pw, cy + 4.2f, 2.5f, if (i < filled) fill else empty)
                }
                pen.text("${s.raw}", W - M, cy + 3.5f, pen.geistSemi, 9.5f, pen.solid(if (tier == 2) Ergos.INK2 else Ergos.INK), Align.RIGHT)
                y += row
            }
        }

        // Explicación al pie de la hoja
        rrect(pen, M, boxTop, W - M, BOTTOM, 10f, pen.solid(Ergos.DEEP, .55f))
        rrect(pen, M, boxTop, W - M, BOTTOM, 10f, pen.sheen(boxTop, BOTTOM, .7f))
        label(pen, "Cómo leer tu curva", M + 18f, boxTop + 18f)
        var yy = boxTop + 32f
        lines.forEach { pen.text(it, M + 18f, yy, pen.geist, 9f, pen.solid(Ergos.INK)); yy += 12.5f }
    }

    // ------------------------------------------------------------------ Ministerios

    private fun ministryCard(pen: Pen, texts: PdfTexts, x: MinistrySuggestion, top: Float, k: Float, draw: Boolean): Float {
        val l = M
        val r = W - M
        val tx = l + 22f
        val tw = r - l - 44f
        val d = texts.ministries[x.ministry.name]
        val what = d?.let { pen.wrap(it.what, pen.geist, 9.3f * k, tw) } ?: emptyList()
        val you = d?.let { pen.wrap(it.you, pen.corMed, 12f * k, tw) } ?: emptyList()
        val bib = d?.let { pen.wrap(it.bible, pen.geist, 8.8f * k, tw - 14f) } ?: emptyList()
        val refs = d?.refs?.joinToString("  ·  ").orEmpty()
        val nameS = fit(pen, x.ministry.name, pen.corBold, 19f * k, tw)
        val h = 30f * k + 20f * k + what.size * 12.6f * k + 6f * k + you.size * 14.5f * k + 10f * k +
            (if (bib.isNotEmpty()) 12f * k + bib.size * 11.8f * k else 0f) + (if (refs.isNotEmpty()) 14f * k else 0f) + 12f * k
        if (!draw) return h

        rrect(pen, l, top, r, top + h, 10f, pen.solid(Ergos.SURFACE2, .92f))
        rrect(pen, l, top, r, top + h, 10f, pen.solid(Ergos.VIOLET, .55f, .8f))
        var yy = top + 26f * k
        pen.text(x.ministry.name, tx, yy, pen.corBold, nameS, pen.sheen(yy - 16f, yy + 4f))
        yy += 8f * k
        var px = tx
        label(pen, "Por tus dones", px, yy + 10f * k, Ergos.MUTED)
        px += pen.measure("POR TUS DONES", pen.geistSemi, 6.8f, 1.6f) + 8f
        for (g in x.matchingGifts) {
            val w = pen.measure(g.name, pen.geistMed, 8f * k) + 14f
            rrect(pen, px, yy, px + w, yy + 15f * k, 7.5f * k, pen.solid(Ergos.VIOLET, .22f))
            pen.text(g.name, px + 7f, yy + 10.5f * k, pen.geistMed, 8f * k, pen.solid(Ergos.INK))
            px += w + 5f
        }
        yy += 20f * k
        what.forEach { yy += 12.6f * k; pen.text(it, tx, yy - 3f, pen.geist, 9.3f * k, pen.solid(Ergos.INK)) }
        yy += 6f * k
        you.forEach { yy += 14.5f * k; pen.text(it, tx, yy - 3f, pen.corMed, 12f * k, pen.solid(Ergos.ACCENT)) }
        yy += 10f * k
        if (bib.isNotEmpty()) {
            val bt = yy
            label(pen, "En la Biblia", tx, yy + 4f * k)
            yy += 8f * k
            bib.forEach { yy += 11.8f * k; pen.text(it, tx + 14f, yy - 2f, pen.geist, 8.8f * k, pen.solid(Ergos.INK2)) }
            pen.canvas.drawLine(tx + 4f, bt + 10f * k, tx + 4f, yy + 1f, pen.sheen(bt, yy, 1.2f))
        }
        if (refs.isNotEmpty()) {
            yy += 14f * k
            pen.text(refs, tx, yy, pen.geistMed, 8f * k, pen.solid(Ergos.ACCENT))
        }
        return h
    }

    private fun ministries(pen: Pen, texts: PdfTexts, list: List<MinistrySuggestion>, startY: Float) {
        var y = startY
        if (list.isEmpty()) {
            val msg = pen.wrap(
                "Tus dones abren caminos que no siempre están en una lista. Conversa con tu pastor: juntos pueden encontrar " +
                    "el lugar donde tus dones sirvan mejor a la iglesia.",
                pen.corMed, 15f, W - 2 * M - 40f,
            )
            msg.forEach { pen.text(it, W / 2, y + 30f, pen.corMed, 15f, pen.solid(Ergos.INK), Align.CENTER); y += 20f }
            return
        }
        val gap = 10f
        val avail = BOTTOM - y
        var k = 1f
        while (k > .62f && list.sumOf { ministryCard(pen, texts, it, 0f, k, false).toDouble() + gap } - gap > avail) k -= .02f
        for (x in list) y += ministryCard(pen, texts, x, y, k, true) + gap
    }

    // ------------------------------------------------------------------ Personaje bíblico

    /** Imagen en assets/personajes/ cuyo nombre empieza con el código del don (p. ej. "O-moises.png"). */
    private fun characterImage(context: Context, code: String): Bitmap? = runCatching {
        val name = context.assets.list("personajes")?.firstOrNull {
            (it.startsWith("$code-") || it.startsWith("$code.")) &&
                it.substringAfterLast('.').lowercase() in setOf("png", "jpg", "jpeg", "webp")
        } ?: return null
        val full = context.assets.open("personajes/$name").use { BitmapFactory.decodeStream(it) } ?: return null
        // Se reduce para que el PDF no pese demasiado.
        val side = 640
        val s = side.toFloat() / minOf(full.width, full.height)
        if (s >= 1f) full else Bitmap.createScaledBitmap(full, (full.width * s).roundToInt(), (full.height * s).roundToInt(), true)
    }.getOrNull()

    private fun character(context: Context, pen: Pen, texts: PdfTexts, result: HoutsResult) {
        val c = pen.canvas
        val top = result.dominant.firstOrNull() ?: result.ranking.firstOrNull() ?: return
        val ch = texts.gifts[top.gift.code]?.character ?: return
        val cx = W / 2
        val cy = 236f
        val r = 112f

        // Halo, rayos y rombos alrededor del círculo
        c.drawCircle(cx, cy, r * 2.1f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, r * 2.1f,
                intArrayOf(Ergos.withAlpha(Ergos.VIOLET, .45f), Ergos.withAlpha(Ergos.DEEP, .25f), Ergos.withAlpha(Ergos.NIGHT, 0f)),
                floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP,
            )
        })
        for (i in 0 until 48) {
            val a = (i / 48f * 2 * PI).toFloat()
            val r0 = r + 16f
            val r1 = r + if (i % 2 == 1) 26f else 40f
            c.drawLine(cx + r0 * cos(a), cy + r0 * sin(a), cx + r1 * cos(a), cy + r1 * sin(a), pen.solid(Ergos.LAV, if (i % 2 == 1) .35f else .7f, .7f))
        }
        for (i in 0 until 8) {
            val a = (i / 8f * 2 * PI + PI / 8).toFloat()
            pen.diamond(cx + (r + 48f) * cos(a), cy + (r + 48f) * sin(a), 3.5f, pen.sheen(cy - r, cy + r))
        }
        c.drawCircle(cx, cy, r + 10f, pen.sheen(cy - r, cy + r, .7f))
        c.drawCircle(cx, cy, r + 3f, pen.sheen(cy - r, cy + r, 2.2f))

        // Retrato recortado en círculo; sin imagen, la inicial del personaje.
        c.save()
        c.clipPath(Path().apply { addCircle(cx, cy, r, Path.Direction.CW) })
        val img = characterImage(context, top.gift.code)
        if (img != null) {
            val s = max(2 * r / img.width, 2 * r / img.height)
            val iw = img.width * s
            val ih = img.height * s
            c.drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(cx - iw / 2, cy - ih / 2, cx + iw / 2, cy + ih / 2), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(cx, cy - 30f, r * 1.3f, intArrayOf(0xFF3B1E7A.toInt(), Ergos.NIGHT2), null, Shader.TileMode.CLAMP)
            })
            pen.text(ch.name.take(1), cx, cy + 40f, pen.corBold, 120f, pen.sheen(cy - 60f, cy + 40f), Align.CENTER)
        }
        c.restore()

        var y = cy + r + 62f
        pen.text("ERES COMO", W / 2, y, pen.geistSemi, 9f, pen.solid(Ergos.ACCENT), Align.CENTER, 4f)
        y += 46f
        pen.text(ch.name, W / 2, y, pen.corBold, fit(pen, ch.name, pen.corBold, 46f, W - 2 * M), pen.sheen(y - 36f, y + 6f), Align.CENTER)
        y += 14f
        pen.ornament(W / 2, y, 110f)
        y += 26f
        pen.wrap("«${ch.quote}»", pen.corMed, 13f, W - 2 * M - 60f).forEach {
            pen.text(it, W / 2, y, pen.corMed, 13f, pen.solid(Ergos.ACCENT), Align.CENTER)
            y += 16f
        }
        pen.text(ch.ref, W / 2, y + 1f, pen.geistSemi, 8f, pen.solid(Ergos.MUTED), Align.CENTER, 1.5f)
        y += 26f

        val others = result.dominant.drop(1).map { it.gift.name }
        val extra = when (others.size) {
            0 -> ""
            1 -> " Además, Dios puso en ti el don de ${others[0]}: juntos forman tu manera única de servir."
            else -> " Además, Dios puso en ti los dones de ${others.dropLast(1).joinToString(", ")} y ${others.last()}: juntos forman tu manera única de servir."
        }
        val note = pen.wrap(
            "Este resultado es un punto de partida para la oración y la experimentación. " +
                "Confírmalo con personas que te conocen, con tu pastor y con los líderes de tu iglesia.",
            pen.geist, 7.8f, W - 2 * M - 40f,
        )
        val noteTop = BOTTOM - note.size * 10.5f - 4f
        // El texto principal se achica si no cabe sobre las píldoras y la nota.
        var size = 14.5f
        var body = pen.wrap(ch.text + extra, pen.corMed, size, W - 2 * M - 30f)
        while (size > 11f && y + body.size * size * 1.31f + 10f + 22f > noteTop - 16f) {
            size -= .5f
            body = pen.wrap(ch.text + extra, pen.corMed, size, W - 2 * M - 30f)
        }
        body.forEach { pen.text(it, W / 2, y, pen.corMed, size, pen.solid(Ergos.INK), Align.CENTER); y += size * 1.31f }
        y += 10f

        // Píldoras con los dones dominantes
        val items = result.dominant.map { it.gift.name }
        val ws = items.map { pen.measure(it, pen.geistSemi, 9f) + 22f }
        var px = W / 2 - (ws.sum() + 8f * (items.size - 1)) / 2
        items.forEachIndexed { i, t ->
            rrect(pen, px, y, px + ws[i], y + 22f, 11f, pen.solid(Ergos.VIOLET, .28f))
            rrect(pen, px, y, px + ws[i], y + 22f, 11f, pen.sheen(y, y + 22f, .8f))
            pen.text(t, px + ws[i] / 2, y + 14.5f, pen.geistSemi, 9f, pen.solid(Ergos.INK), Align.CENTER)
            px += ws[i] + 8f
        }

        var yy = noteTop
        note.forEach { pen.text(it, W / 2, yy, pen.geist, 7.8f, pen.solid(Ergos.MUTED), Align.CENTER); yy += 10.5f }
        pen.text("Cuestionario Houts modificado por Wagner", W / 2, yy + 4f, pen.geist, 6.8f, pen.solid(Ergos.MUTED, .8f), Align.CENTER)
    }
}
