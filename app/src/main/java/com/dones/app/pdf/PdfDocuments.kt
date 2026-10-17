package com.dones.app.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.dones.core.ClassInfo
import com.dones.core.HoutsContent
import com.dones.core.HoutsResult
import com.dones.core.ParticipantState
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * PDFs generados en el teléfono, sin diseño gráfico todavía (solo texto ordenado).
 * Tamaño carta/A4 aproximado: 595 x 842 puntos.
 */
object PdfDocuments {

    private const val W = 595
    private const val H = 842
    private const val MARGIN = 48f
    private val spanishDate = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es"))

    fun results(
        dir: File,
        content: HoutsContent,
        participant: ParticipantState,
        result: HoutsResult,
        info: ClassInfo?,
    ): File {
        val file = File(dir, "mis-dones.pdf")
        val w = Writer()
        w.text("Mis dones espirituales", 24f, bold = true)
        w.text(participant.name, 16f)
        w.text(
            listOfNotNull(info?.courseName, info?.churchName, LocalDate.now().format(spanishDate)).joinToString(" · "),
            11f,
        )
        w.gap(16f)

        w.text("Dones dominantes", 16f, bold = true)
        result.dominant.forEach { s ->
            w.text("${s.rank}. ${s.gift.name} — ${s.raw} de ${s.max}", 14f, bold = true)
            w.text(s.gift.definition, 11f)
            w.text(s.gift.refs.joinToString(" · "), 10f)
            w.gap(6f)
        }
        if (result.subordinate.isNotEmpty()) {
            w.gap(6f)
            w.text("Dones subordinados", 16f, bold = true)
            result.subordinate.forEach { s -> w.text("${s.rank}. ${s.gift.name} — ${s.raw} de ${s.max}", 13f) }
        }

        w.gap(12f)
        w.text("Mi curva completa", 16f, bold = true)
        result.ranking.forEach { s -> w.bar(s.gift.name, s.fraction.toFloat(), "${s.percent} %") }

        if (result.ministries.isNotEmpty()) {
            w.gap(12f)
            w.text("Dónde podría servir", 16f, bold = true)
            result.ministries.forEach { m ->
                w.text("• ${m.ministry.name} (por: ${m.matchingGifts.joinToString(", ") { it.name }})", 12f)
            }
        }

        w.gap(16f)
        w.text(
            "Este resultado es un punto de partida para la oración y la experimentación. " +
                "Confírmalo con personas que te conocen, con tu pastor y con los ancianos.",
            10f,
        )
        w.text("Cuestionario Houts modificado por Wagner · contenido v${content.version}", 9f)
        w.save(file)
        return file
    }

    fun certificate(dir: File, name: String, info: ClassInfo, date: LocalDate, code: String): File {
        val file = File(dir, "certificado.pdf")
        val w = Writer()
        w.gap(120f)
        w.text("CERTIFICADO DE PARTICIPACIÓN", 26f, bold = true, center = true)
        w.gap(40f)
        w.text("Se certifica que", 14f, center = true)
        w.gap(8f)
        w.text(name.uppercase(Locale("es")), 24f, bold = true, center = true)
        w.gap(8f)
        w.text("participó en el curso", 14f, center = true)
        w.text("“${info.courseName}”", 20f, bold = true, center = true)
        w.gap(40f)
        w.text("Iglesia: ${info.churchName}", 13f, center = true)
        w.text("Fecha: ${date.format(spanishDate)}", 13f, center = true)
        w.gap(70f)
        w.text("______________________________", 13f, center = true)
        w.text(info.teacherName, 13f, center = true)
        w.gap(60f)
        w.text("Código de verificación: $code", 10f, center = true)
        w.save(file)
        return file
    }

    /** Escritor simple: texto con salto de línea automático y nueva página cuando se llena. */
    private class Writer {
        private val doc = PdfDocument()
        private var pageNumber = 0
        private lateinit var page: PdfDocument.Page
        private lateinit var canvas: Canvas
        private var y = 0f

        init { newPage() }

        private fun newPage() {
            if (pageNumber > 0) doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        private fun paint(size: Float, bold: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = Color.BLACK
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        fun gap(points: Float) { y += points }

        fun text(text: String, size: Float, bold: Boolean = false, center: Boolean = false) {
            if (text.isBlank()) return
            val p = paint(size, bold)
            val width = W - 2 * MARGIN
            val lineHeight = size * 1.35f
            for (line in wrap(text, p, width)) {
                if (y + lineHeight > H - MARGIN) newPage()
                y += size
                val x = if (center) (W - p.measureText(line)) / 2f else MARGIN
                canvas.drawText(line, x, y, p)
                y += lineHeight - size
            }
        }

        fun bar(label: String, fraction: Float, value: String) {
            val p = paint(11f, false)
            val rowHeight = 18f
            if (y + rowHeight > H - MARGIN) newPage()
            y += 12f
            canvas.drawText(label, MARGIN, y, p)
            val barX = MARGIN + 190f
            val barW = W - MARGIN - barX - 50f
            val outline = Paint().apply { style = Paint.Style.STROKE; color = Color.GRAY }
            val fill = Paint().apply { color = Color.DKGRAY }
            canvas.drawRect(barX, y - 10f, barX + barW, y, outline)
            canvas.drawRect(barX, y - 10f, barX + barW * fraction.coerceIn(0f, 1f), y, fill)
            canvas.drawText(value, barX + barW + 8f, y, p)
            y += rowHeight - 12f
        }

        private fun wrap(text: String, p: Paint, width: Float): List<String> {
            val lines = mutableListOf<String>()
            for (paragraph in text.split("\n")) {
                var current = ""
                for (word in paragraph.split(" ")) {
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (p.measureText(candidate) <= width || current.isEmpty()) current = candidate
                    else { lines += current; current = word }
                }
                lines += current
            }
            return lines
        }

        fun save(file: File) {
            doc.finishPage(page)
            file.outputStream().use { doc.writeTo(it) }
            doc.close()
        }
    }
}
