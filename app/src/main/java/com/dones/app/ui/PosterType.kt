package com.dones.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dones.app.R

/*
 * Tipografía de los PDF llevada a la app: nombre con la letra del logo y degradado,
 * "Eres como" en cursiva, rótulos en versalitas espaciadas y palabras unidas por líneas.
 */

val Fraktur = FontFamily(Font(R.font.unifraktur_maguntia))
private val Rocker = FontFamily(Font(R.font.new_rocker))
val CormorantItalic = FontFamily(Font(R.font.cormorant_semibold_italic, FontWeight.SemiBold, FontStyle.Italic))

/** Dorado de los títulos del PDF. */
val GoldText = Brush.verticalGradient(listOf(Color(0xFFF6E3B4), Color(0xFFD9B877), Color(0xFF9C7744)))
/** Rosa de la hoja de la batalla. */
val RoseText = Brush.verticalGradient(listOf(Color(0xFFFFE3E7), Color(0xFFFFB8C2), Color(0xFFE0566A)))

private val Carve = Shadow(Color.Black.copy(alpha = .75f), Offset(3f, 4f), 6f)

/** Texto con la letra del logo; la S, la A y la X (que en gótica parece una «r») van en una letra que se lee bien (como en el PDF). */
fun frakturString(s: String): AnnotatedString = buildAnnotatedString {
    for (ch in s) {
        if (ch in "SAxX") withStyle(SpanStyle(fontFamily = Rocker, fontSize = .93.em)) { append(ch) } else append(ch)
    }
}

@Composable
fun PosterName(text: String, size: TextUnit = 64.sp, brush: Brush = GoldText, modifier: Modifier = Modifier) {
    Text(
        frakturString(text),
        modifier = modifier,
        style = TextStyle(fontFamily = Fraktur, fontSize = size, lineHeight = size * 1.05f, brush = brush, shadow = Carve, textAlign = TextAlign.Center),
    )
}

@Composable
fun PosterItalic(text: String, color: Color, size: TextUnit = 26.sp, modifier: Modifier = Modifier, center: Boolean = true) {
    Text(
        text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = CormorantItalic, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic,
            fontSize = size, lineHeight = size * 1.2f, color = color, shadow = Carve,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
        ),
    )
}

/** Rótulo en mayúsculas espaciadas, como "N.º 01 · TU PERSONAJE BÍBLICO". */
@Composable
fun SpacedCaps(text: String, color: Color, size: TextUnit = 13.sp, modifier: Modifier = Modifier, center: Boolean = true) {
    Text(
        text.uppercase(java.util.Locale("es")),
        modifier = modifier,
        style = TextStyle(
            fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = size, letterSpacing = .22.em,
            color = color, shadow = Carve, textAlign = if (center) TextAlign.Center else TextAlign.Start,
        ),
    )
}

/** Palabras en píldoras unidas por una línea fina (las tres palabras del personaje). */
@Composable
fun LinkedChips(words: List<String>, color: Color, line: Color = Color(0xFFD9B877)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        words.forEachIndexed { i, w ->
            if (i > 0) Box(Modifier.width(10.dp).height(1.5.dp).border(1.dp, line))
            Box(
                Modifier
                    .border(1.5.dp, line, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) { SpacedCaps(w, color, 11.sp) }
        }
    }
}

// ---------- Íconos de batalla ----------

private fun DrawScope.sword(cx: Float, cy: Float, len: Float, color: Color) {
    val w = len * .085f
    val top = cy - len / 2
    val guard = cy + len * .22f
    val bottom = cy + len / 2
    drawPath(Path().apply {
        moveTo(cx, top); lineTo(cx + w / 2, top + w * 1.6f); lineTo(cx + w / 2, guard); lineTo(cx - w / 2, guard); lineTo(cx - w / 2, top + w * 1.6f); close()
    }, color)
    drawRect(color, Offset(cx - w * 2.6f, guard), androidx.compose.ui.geometry.Size(w * 5.2f, w * .9f))
    drawRect(color, Offset(cx - w * .38f, guard + w * .9f), androidx.compose.ui.geometry.Size(w * .76f, bottom - guard - w * 2f))
    drawCircle(color, w * .6f, Offset(cx, bottom - w * .6f))
}

/** Dos espadas cruzadas. */
@Composable
fun CrossedSwords(size: Dp = 40.dp, color: Color = Color(0xFFFFE3E7)) {
    Canvas(Modifier.size(size)) {
        val c = center
        rotate(-38f, c) { sword(c.x, c.y, this.size.height, color) }
        rotate(38f, c) { sword(c.x, c.y, this.size.height, color) }
    }
}

/** Una espada (inclinada [angle] grados). */
@Composable
fun SwordIcon(size: Dp = 18.dp, color: Color, angle: Float = 0f) {
    Canvas(Modifier.size(size)) { rotate(angle, center) { sword(center.x, center.y, this.size.height, color) } }
}

/** Título con espadas a los lados. */
@Composable
fun SwordsTitle(content: @Composable () -> Unit, color: Color = Color(0xFFFFE3E7)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CrossedSwords(34.dp, color)
        content()
        CrossedSwords(34.dp, color)
    }
}
