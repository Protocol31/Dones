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
import kotlin.math.max
import kotlin.math.roundToInt

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
 * 1 dominantes · 2 subordinados · 3 curva completa · 4 dónde servir · 5 personaje bíblico · 6 la batalla.
 * Cada hoja achica un poco la letra si el contenido no cabe (por ejemplo, con empates).
 */
internal object ResultsPdf {

    private const val W = 595f
    private const val H = 842f
    private const val M = 58f
    private const val BOTTOM = H - 70f
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
        val battleText = BattleText.load(context)
        val totalPages = (if (result.subordinate.isEmpty()) 4 else 5) + (if (battleText != null) 1 else 0)
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        /** Fondo con textura y marco dorado; [frame] = false cuando la hoja lo dibuja al final (hoja 5). */
        fun newPage(red: Boolean = false, frame: Boolean = true) {
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), pageNo).create())
            page = p
            pen.canvas = p.canvas
            pen.paper(W, H)
            if (red) redBackground(pen)
            if (frame) pen.goldFrame(W, H)
            pen.text(
                "${Church.footer}  ·  Página $pageNo de $totalPages", W / 2, H - 50f, pen.geist, 7f,
                pen.solid(if (red) Red.MUTED else Ergos.MUTED), Align.CENTER,
            )
            y = 52f
        }

        fun runningHead(color: Int = Ergos.MUTED) =
            pen.text("Mis dones espirituales  ·  $name".uppercase(Locale("es")), W / 2, 60f, pen.geistSemi, 6.8f, pen.solid(color), Align.CENTER, 1.6f)

        /** Encabezado de las hojas 2 en adelante. */
        fun pageHead(title: String, sub: String?) {
            runningHead()
            y = 98f
            pen.frakturText(title, W / 2, y, pen.fitFraktur(title, 30f, W - 2 * M), pen.gold(y - 24f, y + 6f), Align.CENTER)
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
        pen.logo(W / 2 - logoW / 2, 54f, logoW)
        y = 54f + logoW * ChurchLogo.LOGO_H / ChurchLogo.LOGO_W + 16f
        pen.text(
            "LLAMADOS  ·  ${(info?.courseName ?: "Descubriendo Mi Ministerio").uppercase(Locale("es"))}",
            W / 2, y, pen.geistSemi, 6.8f, pen.solid(Ergos.ACCENT), Align.CENTER, 2.2f,
        )
        y += 28f
        pen.frakturText("Mis dones espirituales", W / 2, y, 30f, pen.gold(y - 24f, y + 6f), Align.CENTER)
        y += 27f
        pen.frakturText(name, W / 2, y, pen.fitFraktur(name, 21f, W - 2 * M), pen.solid(Ergos.INK), Align.CENTER)
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
        newPage(frame = false)
        character(context, pen, texts, result, content)
        pen.goldFrame(W, H)

        // ---------------------------------------------------------------- Hoja 6: la batalla por tus dones
        if (battleText != null) {
            newPage(red = true, frame = false)
            battle(context, pen, battleText, result)
            pen.goldFrame(W, H)
        }

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

    /** Primera imagen en assets/[folder]/ cuyo nombre empieza con [prefix] (p. ej. "B-pedro.jpg"). */
    private fun assetImage(context: Context, folder: String, prefix: String, maxSide: Int): Bitmap? = runCatching {
        val name = context.assets.list(folder)?.firstOrNull {
            (it.startsWith("$prefix-") || it.startsWith("$prefix.")) &&
                it.substringAfterLast('.').lowercase() in setOf("png", "jpg", "jpeg", "webp")
        } ?: return null
        val full = context.assets.open("$folder/$name").use { BitmapFactory.decodeStream(it) } ?: return null
        // Se reduce para que el PDF no pese demasiado.
        val s = maxSide.toFloat() / max(full.width, full.height)
        if (s >= 1f) full else Bitmap.createScaledBitmap(full, (full.width * s).roundToInt(), (full.height * s).roundToInt(), true)
    }.getOrNull()

    /** Dibuja [img] llenando el rectángulo y recortando lo que sobra (centrado). */
    private fun drawCover(pen: Pen, img: Bitmap, dst: RectF) {
        val s = max(dst.width() / img.width, dst.height() / img.height)
        val iw = dst.width() / s
        val ih = dst.height() / s
        val sx = (img.width - iw) / 2f
        val sy = (img.height - ih) / 2f
        val src = Rect(sx.roundToInt(), sy.roundToInt(), (sx + iw).roundToInt(), (sy + ih).roundToInt())
        pen.canvas.drawBitmap(img, src, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    // ------------------------------------------------------------------ Hoja 5: personaje bíblico

    /** Tema de cada personaje (assets/personajes-tema.json): color, tres palabras y una frase. */
    private class Theme(val accent: Int, val words: List<String>, val motto: String)

    private fun theme(context: Context, code: String): Theme = runCatching {
        val o = JSONObject(context.assets.open("personajes-tema.json").bufferedReader().use { it.readText() })
            .getJSONObject("characters").getJSONObject(code)
        val w = o.getJSONArray("words")
        Theme(android.graphics.Color.parseColor(o.getString("accent")), (0 until w.length()).map { w.getString(it) }, o.optString("motto"))
    }.getOrElse { Theme(Ergos.ACCENT, emptyList(), "") }

    /** Mezcla el color con la noche (para fondos de píldoras y del ícono). */
    private fun deep(color: Int, k: Float = .22f): Int {
        fun ch(c: Int, n: Int) = (n + (c - n) * k).roundToInt()
        return (0xFF shl 24) or (ch(color shr 16 and 255, 13) shl 16) or (ch(color shr 8 and 255, 9) shl 8) or ch(color and 255, 25)
    }

    private fun star(pen: Pen, x: Float, y: Float, r: Float, color: Int) {
        pen.canvas.drawPath(Path().apply {
            moveTo(x, y - r); quadTo(x, y, x + r * .5f, y); quadTo(x, y, x, y + r); quadTo(x, y, x - r * .5f, y); quadTo(x, y, x, y - r); close()
        }, pen.solid(color))
    }

    /** Ícono del personaje dentro de un sol de rayos (assets/iconos/<código>.png teñido con el color del tema). */
    private fun emblem(pen: Pen, code: String, th: Theme, cx: Float, cy: Float, r: Float) {
        val c = pen.canvas
        for (i in 0 until 36) {
            val a = (i / 36.0 * 2 * Math.PI).toFloat()
            val r0 = r * 1.05f
            val r1 = r * if (i % 2 == 1) 1.25f else 1.45f
            c.drawLine(cx + r0 * kotlin.math.cos(a), cy + r0 * kotlin.math.sin(a), cx + r1 * kotlin.math.cos(a), cy + r1 * kotlin.math.sin(a), pen.solid(th.accent, if (i % 2 == 1) .45f else .85f, .8f))
        }
        c.drawCircle(cx, cy, r * .92f, pen.solid(deep(th.accent, .3f), .8f))
        c.drawCircle(cx, cy, r * .92f, pen.solid(th.accent, .7f, .7f))
        pen.asset("iconos/$code.png")?.let { icon ->
            val s = r * .78f
            c.drawBitmap(icon, Rect(0, 0, icon.width, icon.height), RectF(cx - s, cy - s, cx + s, cy + s), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = android.graphics.PorterDuffColorFilter(th.accent, android.graphics.PorterDuff.Mode.SRC_IN)
            })
        }
        star(pen, cx - r * 1.9f, cy - r * .9f, 5f, th.accent)
        star(pen, cx + r * 1.9f, cy - r * .9f, 5f, th.accent)
    }

    /** Píldoras con borde del color del tema, centradas en x. */
    private fun chips(pen: Pen, words: List<String>, x: Float, y: Float, color: Int) {
        if (words.isEmpty()) return
        val ws = words.map { pen.measure(it, pen.geistSemi, 7f, 1.6f) + 18f }
        var px = x - (ws.sum() + 6f * (ws.size - 1)) / 2
        words.forEachIndexed { i, w ->
            rrect(pen, px, y, px + ws[i], y + 17f, 8.5f, pen.solid(color, .9f, .7f))
            pen.text(w, px + ws[i] / 2, y + 11.6f, pen.geistSemi, 7f, pen.solid(color), Align.CENTER, 1.6f)
            px += ws[i] + 6f
        }
    }

    /** Dónde va el texto sobre la escultura: "left", "right" o "bottom" (assets/esculturas-texto.json). */
    private fun textSide(context: Context, code: String): String = runCatching {
        JSONObject(context.assets.open("esculturas-texto.json").bufferedReader().use { it.readText() }).optString(code, "bottom")
    }.getOrDefault("bottom")

    /** Hoja 5: la escultura ocupa toda la hoja y el texto va en el lado vacío (o abajo si la figura ocupa el centro). */
    private fun character(context: Context, pen: Pen, texts: PdfTexts, result: HoutsResult, content: HoutsContent) {
        val c = pen.canvas
        val top = result.dominant.firstOrNull() ?: result.ranking.firstOrNull() ?: return
        val code = top.gift.code
        val ch = texts.gifts[code]?.character ?: return
        val th = theme(context, code)
        val number = (content.gifts.indexOfFirst { it.code == code } + 1).coerceAtLeast(1)
        val es = Locale("es")
        val side = textSide(context, code)
        val center = side == "bottom"

        // Escultura a sangre (la ilustración es 3:4 y la hoja un poco más angosta: se pierde un poco a los lados)
        val img = assetImage(context, "esculturas", code, 1800)
        if (img != null) {
            val s = max(W / img.width, H / img.height)
            val iw = W / s
            val ih = H / s
            val sx = ((img.width - iw) / 2f).coerceAtLeast(0f)
            val src = Rect(sx.roundToInt(), 0, (sx + iw).roundToInt().coerceAtMost(img.width), ih.roundToInt().coerceAtMost(img.height))
            c.drawBitmap(img, src, RectF(0f, 0f, W, H), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        // Sombra suave del lado del texto, para que se lea
        if (center) {
            val y0 = H * .40f
            val y1 = H * .64f
            c.drawRect(0f, y0, W, H, Paint().apply {
                shader = LinearGradient(0f, y0, 0f, y1, intArrayOf(Ergos.withAlpha(Ergos.NIGHT, 0f), Ergos.withAlpha(Ergos.NIGHT, .93f)), null, Shader.TileMode.CLAMP)
            })
            c.drawRect(0f, y1, W, H, pen.solid(Ergos.NIGHT, .93f))
        } else {
            val xa = if (side == "left") 0f else W
            val xb = if (side == "left") W * .8f else W * .2f
            c.drawRect(0f, 0f, W, H, Paint().apply {
                shader = LinearGradient(xa, 0f, xb, 0f, intArrayOf(Ergos.withAlpha(Ergos.NIGHT, .86f), Ergos.withAlpha(Ergos.NIGHT, .55f), Ergos.withAlpha(Ergos.NIGHT, 0f)), floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP)
            })
        }
        c.drawRect(0f, 0f, W, 130f, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, 130f, Ergos.withAlpha(Ergos.NIGHT, .75f), Ergos.withAlpha(Ergos.NIGHT, 0f), Shader.TileMode.CLAMP)
        })

        // Rótulos tipo ficha
        pen.shadowed("N.º ${"%02d".format(number)}  ·  TU PERSONAJE BÍBLICO", M, 80f, pen.geistSemi, 8.4f, Ergos.CREAM, Align.LEFT, 2.2f)
        pen.shadowed("${ch.ref.uppercase(es)}  ·  RVR1960", W - M, 80f, pen.geistSemi, 8.4f, Ergos.CREAM, Align.RIGHT, 2.2f)
        c.drawLine(M, 88f, W - M, 88f, pen.solid(Ergos.CREAM, .5f, .5f))

        // Bloque de texto
        val bw = if (center) W - 2 * M else 224f
        val bx = if (side == "right") W - M - bw else M
        val ax = if (center) W / 2 else bx
        val al = if (center) Align.CENTER else Align.LEFT
        val startY = if (center) H * .45f else 112f
        val others = result.dominant.drop(1).map { it.gift.name }
        val extra = when (others.size) {
            0 -> ""
            1 -> " Además, Dios puso en ti el don de ${others[0]}: juntos forman tu manera única de servir."
            else -> " Además, Dios puso en ti los dones de ${others.dropLast(1).joinToString(", ")} y ${others.last()}: juntos forman tu manera única de servir."
        }
        val note = pen.wrap(
            "Este resultado es un punto de partida para la oración y la experimentación. " +
                "Confírmalo con personas que te conocen, con tu pastor y con los líderes de tu iglesia.",
            pen.geist, 8.6f, if (center) W - 2 * M - 60f else bw,
        )
        val noteTop = BOTTOM - note.size * 11.4f + 4f
        val items = result.dominant.map { it.gift.name }
        val words = th.words.take(3).map { it.uppercase(es) }

        // Dibuja (o solo mide) todo el bloque con la letra a escala k; devuelve dónde termina
        fun lay(k: Float, draw: Boolean): Float {
            var y = startY
            // "Eres como" y el nombre
            val eresSz = 21f * k
            y += eresSz
            if (draw) pen.shadowed("Eres como", ax, y, pen.italic, eresSz, Ergos.CREAM, al)
            val nameSize = pen.fitFraktur(ch.name, 76f * k, bw)
            y += nameSize * .86f + 2f
            if (draw) {
                pen.frakturText(ch.name, ax + 2.5f, y + 2.5f, nameSize, pen.solid(0xFF000000.toInt(), .6f), al)
                pen.frakturText(ch.name, ax, y, nameSize, pen.gold(y - nameSize * .8f, y + 6f), al)
            }
            // Tres palabras en píldoras unidas por una línea
            y += 26f
            if (words.isNotEmpty()) {
                var fs = 9f
                val link = 12f
                fun total() = words.sumOf { (pen.measure(it, pen.geistSemi, fs, 1.4f) + 16f).toDouble() }.toFloat() + link * (words.size - 1)
                while (total() > bw && fs > 5.6f) fs -= .3f
                if (draw) {
                    var px = if (center) W / 2 - total() / 2 else ax
                    words.forEachIndexed { i, t ->
                        val w = pen.measure(t, pen.geistSemi, fs, 1.4f) + 16f
                        rrect(pen, px, y - 11f, px + w, y + 5f, 8f, pen.solid(Ergos.NIGHT, .55f))
                        rrect(pen, px, y - 11f, px + w, y + 5f, 8f, pen.gold(y - 11f, y + 5f, .9f))
                        pen.text(t, px + w / 2, y, pen.geistSemi, fs, pen.solid(th.accent), Align.CENTER, 1.4f)
                        px += w
                        if (i < words.size - 1) {
                            c.drawLine(px, y - 3f, px + link, y - 3f, pen.gold(y - 4f, y - 2f, .8f))
                            px += link
                        }
                    }
                }
            }
            // El versículo, tal cual, con su cita
            val qs = 15.5f * k
            val quote = pen.wrap("«${ch.quote}»", pen.corMed, qs, if (center) bw - 40f else bw)
            y += 32f
            quote.forEach { if (draw) pen.shadowed(it, ax, y, pen.corMed, qs, Ergos.INK, al); y += qs * 1.22f }
            y += 2f
            if (draw) pen.shadowed("—  ${ch.ref}  ·  RVR1960", ax, y, pen.geistSemi, 8.8f, Ergos.ACCENT, al, 1.6f)
            // Su historia
            y += 26f
            if (draw) pen.shadowed("SU HISTORIA, TU LLAMADO", ax, y, pen.geistSemi, 8.8f, th.accent, al, 2f)
            val bs = 12.6f * k
            val body = pen.wrap(ch.text + extra, pen.corMed, bs, bw)
            y += 15f
            body.forEach { if (draw) pen.shadowed(it, bx, y, pen.corMed, bs, Ergos.INK); y += bs * 1.26f }
            // Emblema y lema
            y += 10f
            val motto = if (th.motto.isEmpty()) emptyList() else pen.wrap("«${th.motto}»", pen.italic, 13f * k, bw - 70f)
            val rowH = max(56f, 22f + motto.size * 15f * k)
            if (draw) {
                emblem(pen, code, th, bx + 24f, y + 26f, 19f)
                if (motto.isNotEmpty()) {
                    pen.shadowed("SU LEMA", bx + 68f, y + 14f, pen.geistSemi, 8.2f, th.accent, Align.LEFT, 2f)
                    var my = y + 32f
                    motto.forEach { pen.shadowed(it, bx + 68f, my, pen.italic, 13f * k, Ergos.CREAM); my += 15f * k }
                }
            }
            y += rowH + 8f
            // Dones dominantes
            if (draw) pen.shadowed("TUS DONES DOMINANTES", ax, y + 4f, pen.geistSemi, 8.6f, Ergos.CREAM, al, 2f)
            y += 14f
            val ps = 10.5f * k
            val pw = items.map { pen.measure(it, pen.geistSemi, ps) + 22f }
            var row = mutableListOf<Int>()
            var rowW = 0f
            fun flush() {
                if (draw && row.isNotEmpty()) {
                    val tot = row.sumOf { pw[it].toDouble() }.toFloat() + 8f * (row.size - 1)
                    var px = if (center) W / 2 - tot / 2 else bx
                    for (i in row) {
                        rrect(pen, px, y, px + pw[i], y + 22f, 11f, pen.solid(deep(th.accent), .9f))
                        rrect(pen, px, y, px + pw[i], y + 22f, 11f, pen.gold(y, y + 22f, .8f))
                        pen.text(items[i], px + pw[i] / 2, y + 14.5f, pen.geistSemi, ps, pen.solid(Ergos.INK), Align.CENTER)
                        px += pw[i] + 8f
                    }
                }
                row = mutableListOf()
                rowW = 0f
            }
            items.indices.forEach { i ->
                if (row.isNotEmpty() && rowW + 8f + pw[i] > bw) { flush(); y += 28f }
                rowW += pw[i] + if (row.isEmpty()) 0f else 8f
                row.add(i)
            }
            flush()
            return y + 22f
        }
        var k = 1f
        while (lay(k, false) > noteTop - 8f && k > .7f) k -= .03f
        lay(k, true)

        var ny2 = noteTop
        note.forEach { pen.shadowed(it, ax, ny2, pen.geist, 8.6f, Ergos.MUTED, al); ny2 += 11.4f }
    }

    // ------------------------------------------------------------------ Hoja 6: la batalla por tus dones

    /** Tonos de la hoja de advertencia: toda roja; el morado queda solo en los bordes. */
    private object Red {
        const val BG0 = 0xFF8E1426.toInt()
        const val BG1 = 0xFF5A0B1F.toInt()
        const val BG2 = 0xFF2A0618.toInt()
        const val CARD = 0xFF3A0716.toInt()
        const val ROSE = 0xFFFFB8C2.toInt()
        const val ROSE_HI = 0xFFFFE3E7.toInt()
        const val ALARM = 0xFFFF6B76.toInt()
        const val MUTED = 0xFFE7A9B3.toInt()
    }

    private fun redBackground(pen: Pen) {
        // Rojo encima del papel con textura, dejando ver un poco la textura.
        pen.canvas.drawRect(0f, 0f, W, H, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                W / 2, 230f, W * 1.05f,
                intArrayOf(Ergos.withAlpha(Red.BG0, .86f), Ergos.withAlpha(Red.BG1, .88f), Ergos.withAlpha(Red.BG2, .9f)),
                floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP,
            )
        })
    }

    private fun rose(y0: Float, y1: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, y0, 0f, y1, intArrayOf(Ergos.WHITE, Red.ROSE_HI, Red.ROSE), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
    }

    /** Imagen de este don en assets/batalla/: <código>-…-<tipo>.jpg; si no, una sin sufijo; si no, null. */
    private fun battleArt(context: Context, code: String, kind: String): Bitmap? = runCatching {
        val all = (context.assets.list("batalla") ?: emptyArray())
            .filter { (it.startsWith("$code-") || it.startsWith("$code.")) && it.substringAfterLast('.').lowercase() in setOf("png", "jpg", "jpeg", "webp") }
        fun stem(n: String) = n.substringBeforeLast('.')
        val name = all.firstOrNull { stem(it).endsWith("-$kind") }
            ?: all.firstOrNull { !stem(it).endsWith("-pdf") && !stem(it).endsWith("-app") }
            ?: return null
        val full = context.assets.open("batalla/$name").use { BitmapFactory.decodeStream(it) } ?: return null
        val s = 2400f / max(full.width, full.height)
        if (s >= 1f) full else Bitmap.createScaledBitmap(full, (full.width * s).roundToInt(), (full.height * s).roundToInt(), true)
    }.getOrNull()

    private enum class Glyph { Swords, Lie, Sword, Shield }

    /** Íconos de batalla dibujados con trazos: espadas cruzadas, mentira, espada de la verdad y escudo. */
    private fun glyph(pen: Pen, g: Glyph, cx: Float, cy: Float, r: Float, color: Int) {
        val c = pen.canvas
        val line = pen.solid(color, 1f, max(1.2f, r * .24f))
        when (g) {
            Glyph.Swords -> for (s in listOf(-1f, 1f)) {
                c.drawLine(cx - s * r, cy + r, cx + s * r, cy - r, line)
                val gx = cx - s * r * .4f
                val gy = cy + r * .4f
                c.drawLine(gx - r * .4f, gy - r * .4f * s, gx + r * .4f, gy + r * .4f * s, line)
            }
            Glyph.Lie -> {
                c.drawCircle(cx, cy, r * .9f, line)
                c.drawLine(cx - r * .42f, cy - r * .42f, cx + r * .42f, cy + r * .42f, line)
                c.drawLine(cx + r * .42f, cy - r * .42f, cx - r * .42f, cy + r * .42f, line)
            }
            Glyph.Sword -> {
                c.drawLine(cx, cy - r, cx, cy + r * .35f, line)
                c.drawLine(cx - r * .5f, cy + r * .35f, cx + r * .5f, cy + r * .35f, line)
                c.drawLine(cx, cy + r * .35f, cx, cy + r, line)
            }
            Glyph.Shield -> c.drawPath(Path().apply {
                moveTo(cx - r * .8f, cy - r); lineTo(cx + r * .8f, cy - r); lineTo(cx + r * .8f, cy)
                quadTo(cx + r * .8f, cy + r * .7f, cx, cy + r); quadTo(cx - r * .8f, cy + r * .7f, cx - r * .8f, cy); close()
            }, line)
        }
    }

    /** Ícono del personaje de ese don (assets/iconos/<código>.png) teñido con [color]. */
    private fun giftIcon(pen: Pen, code: String, cx: Float, cy: Float, s: Float, color: Int) {
        val icon = pen.asset("iconos/$code.png") ?: return
        pen.canvas.drawBitmap(icon, Rect(0, 0, icon.width, icon.height), RectF(cx - s, cy - s, cx + s, cy + s), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
        })
    }

    /** Texto con un halo oscuro alrededor (trazo ancho y suave), para leerse sobre la ilustración sin taparla. */
    private fun halo(pen: Pen, s: String, x: Float, y: Float, face: Typeface, size: Float, color: Int, align: Align = Align.CENTER, spacing: Float = 0f) {
        val dark = 0xFF0C0204.toInt()
        val outer = pen.solid(dark, .35f, size * .42f)
        val inner = pen.solid(dark, .6f, size * .2f)
        pen.text(s, x, y, face, size, outer, align, spacing)
        pen.text(s, x, y, face, size, inner, align, spacing)
        pen.text(s, x, y, face, size, pen.solid(color), align, spacing)
    }

    private fun haloFraktur(pen: Pen, s: String, x: Float, y: Float, size: Float, color: Int) {
        val dark = 0xFF0C0204.toInt()
        pen.frakturText(s, x, y, size, pen.solid(dark, .35f, size * .3f), Align.CENTER)
        pen.frakturText(s, x, y, size, pen.solid(dark, .6f, size * .14f), Align.CENTER)
        pen.frakturText(s, x, y, size, pen.solid(color), Align.CENTER)
    }

    /** Hoja 6: la ilustración pelada a sangre (como la hoja 5) y solo la tipografía encima, con halo. */
    private fun battle(context: Context, pen: Pen, b: BattleText, result: HoutsResult) {
        val c = pen.canvas
        val es = Locale("es")
        val doms = (result.dominant.ifEmpty { result.ranking.take(1) }).take(3).filter { b.gifts.containsKey(it.gift.code) }
        val main = doms.firstOrNull() ?: return
        val others = doms.drop(1)
        val g = b.gifts.getValue(main.gift.code)

        c.drawRect(0f, 0f, W, H, pen.solid(Red.BG2))
        val art = battleArt(context, main.gift.code, "pdf") ?: assetImage(context, "batalla", "default", 1800)
        if (art != null) drawCover(pen, art, RectF(0f, 0f, W, H))

        // ---- Arriba, como en la hoja 5 ----
        halo(pen, "N.º 06  ·  ADVERTENCIA", M, 80f, pen.geistSemi, 7f, Red.ROSE_HI, Align.LEFT, 2.2f)
        halo(pen, "1 P 5:8-9  ·  RVR1960", W - M, 80f, pen.geistSemi, 7f, Red.ROSE_HI, Align.RIGHT, 2.2f)
        c.drawLine(M, 88f, W - M, 88f, pen.solid(Red.ROSE, .55f, .5f))
        halo(pen, "Prepárate para", W / 2, 118f, pen.italic, 21f, Red.ROSE_HI)
        haloFraktur(pen, "La batalla", W / 2, 166f, 54f, Red.ROSE_HI)
        halo(pen, "POR TUS DONES", W / 2, 186f, pen.geistSemi, 8.5f, Red.ALARM, Align.CENTER, 3.5f)

        // ---- Abajo, sobre la niebla: el don principal grande y los otros en dos columnas ----
        val tw = W - 2 * M
        fun ornament(y: Float, half: Float) {
            c.drawLine(W / 2 - half, y, W / 2 - 12f, y, pen.solid(Red.ROSE, .9f, .8f))
            c.drawLine(W / 2 + 12f, y, W / 2 + half, y, pen.solid(Red.ROSE, .9f, .8f))
            pen.diamond(W / 2, y, 4.5f, pen.solid(Red.ALARM))
        }

        fun lay(k: Float, draw: Boolean): Float {
            var y = H * .565f
            if (draw) ornament(y, tw / 2.6f)
            y += 34f * k
            val ns = pen.fitFraktur(main.gift.name, 34f * k, tw)
            if (draw) haloFraktur(pen, main.gift.name, W / 2, y, ns, Red.ROSE_HI)
            y += 20f * k
            if (draw) halo(pen, "LA MENTIRA", W / 2, y, pen.geistSemi, 7f * k, Red.ALARM, Align.CENTER, 1.8f)
            y += 20f * k
            pen.wrap("«${g.lie}»", pen.italic, 19f * k, tw - 40f).forEach { if (draw) halo(pen, it, W / 2, y, pen.italic, 19f * k, Ergos.INK); y += 22f * k }
            y += 4f * k
            if (draw) halo(pen, "LA VERDAD", W / 2, y, pen.geistSemi, 7f * k, Red.ROSE_HI, Align.CENTER, 1.8f)
            y += 17f * k
            pen.wrap(g.truth, pen.corMed, 13.5f * k, tw - 20f).forEach { if (draw) halo(pen, it, W / 2, y, pen.corMed, 13.5f * k, Ergos.INK); y += 16f * k }
            if (draw) halo(pen, "${g.ref.uppercase(es)}  ·  RVR1960", W / 2, y + 1f, pen.geistSemi, 6.6f * k, Red.ROSE, Align.CENTER, 1.5f)
            y += 18f * k
            if (draw) halo(pen, "CÓMO RESISTIR", W / 2, y, pen.geistSemi, 7f * k, Red.ROSE_HI, Align.CENTER, 1.8f)
            y += 15f * k
            pen.wrap(g.defense, pen.geist, 9.6f * k, tw - 60f).forEach { if (draw) halo(pen, it, W / 2, y, pen.geist, 9.6f * k, Ergos.INK2); y += 12.5f * k }
            y += 12f * k
            if (others.isNotEmpty()) {
                if (draw) ornament(y, tw / 2)
                y += 22f * k
                val gap = 24f
                val cw = if (others.size == 1) tw * .7f else (tw - gap) / 2
                var bottom = y
                others.forEachIndexed { i, s ->
                    val og = b.gifts.getValue(s.gift.code)
                    val x0 = if (others.size == 1) W / 2 - cw / 2 else M + i * (cw + gap)
                    val cx = x0 + cw / 2
                    var yy = y
                    val fs = pen.fitFraktur(s.gift.name, 18f * k, cw)
                    if (draw) haloFraktur(pen, s.gift.name, cx, yy, fs, Red.ROSE_HI)
                    yy += 15f * k
                    pen.wrap("«${og.lie}»", pen.italic, 12.5f * k, cw).forEach { if (draw) halo(pen, it, cx, yy, pen.italic, 12.5f * k, Ergos.INK); yy += 14.5f * k }
                    yy += 2f
                    pen.wrap(og.defense, pen.geist, 8.4f * k, cw).forEach { if (draw) halo(pen, it, cx, yy, pen.geist, 8.4f * k, Ergos.INK2); yy += 10.8f * k }
                    bottom = max(bottom, yy)
                }
                y = bottom + 6f * k
            }
            y += 4f
            pen.wrap(b.closing, pen.italic, 11.5f * k, tw - 40f).forEach { if (draw) halo(pen, it, W / 2, y, pen.italic, 11.5f * k, Ergos.INK); y += 14f * k }
            if (draw) halo(pen, "${b.closingRef.uppercase(es)}  ·  RVR1960", W / 2, y + 2f, pen.geistSemi, 6.6f * k, Red.ROSE, Align.CENTER, 1.5f)
            return y
        }

        var k = 1f
        while (k > .74f && lay(k, false) > BOTTOM + 4f) k -= .02f
        lay(k, true)
    }
}

/** Textos de la hoja 6 (assets/batalla.json). */
internal class BattleText(
    val intro: String,
    val closing: String,
    val closingRef: String,
    val gifts: Map<String, Gift>,
) {
    data class Gift(val attack: String, val lie: String, val truth: String, val ref: String, val defense: String)

    companion object {
        fun load(context: Context): BattleText? = runCatching {
            val o = JSONObject(context.assets.open("batalla.json").bufferedReader().use { it.readText() })
            val g = o.getJSONObject("gifts")
            BattleText(
                o.optString("intro"), o.optString("closing"), o.optString("closingRef"),
                g.keys().asSequence().associateWith { code ->
                    val x = g.getJSONObject(code)
                    Gift(x.optString("attack"), x.optString("lie"), x.optString("truth"), x.optString("ref"), x.optString("defense"))
                },
            )
        }.getOrNull()
    }
}
