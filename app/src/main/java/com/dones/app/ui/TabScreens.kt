package com.dones.app.ui

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dones.app.DonesViewModel
import com.dones.app.MainTab
import com.dones.app.Overlay
import com.dones.app.pdf.BattleText
import com.dones.app.pdf.PdfTexts
import com.dones.app.share.Sharing
import com.dones.core.GiftScore
import org.json.JSONObject
import java.util.Locale

// ---------- Pestañas de arriba ----------

/** Lecciones · Cuestionario · Resultados. */
@Composable
fun MainTabBar(vm: DonesViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TabItem("Lecciones", ErgosIcons.Book, vm.tab == MainTab.Lessons, false, Modifier.weight(1f)) { vm.openTab(MainTab.Lessons) }
        TabItem("Cuestionario", ErgosIcons.Pencil, vm.tab == MainTab.Quiz, false, Modifier.weight(1f)) { vm.openTab(MainTab.Quiz) }
        TabItem("Resultados", ErgosIcons.Sparkle, vm.tab == MainTab.Results, !vm.quizComplete, Modifier.weight(1f)) { vm.openTab(MainTab.Results) }
    }
}

@Composable
private fun TabItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, dim: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val color = when {
        selected -> Ergos.Ink
        dim -> Ergos.Muted.copy(alpha = .6f)
        else -> Ergos.Muted
    }
    Column(
        modifier
            .heightIn(min = 62.dp)
            .clip(shape)
            .background(if (selected) Ergos.Surface2 else Ergos.Surface.copy(alpha = .5f))
            .border(if (selected) 2.dp else 1.dp, if (selected) Ergos.VioletHi else Ergos.Line, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Ergos.AccentText else color, modifier = Modifier.size(24.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, lineHeight = 19.sp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------- Fin de las lecciones ----------

@Composable
fun LessonsDoneScreen(vm: DonesViewModel) {
    Screen(
        bottom = {
            if (vm.quizComplete) {
                PrimaryButton("Ver mis resultados", onClick = { vm.openTab(MainTab.Results) }, icon = ErgosIcons.Sparkle, trailing = ErgosIcons.ChevronRight)
            } else {
                PrimaryButton("Ir al cuestionario", onClick = { vm.openTab(MainTab.Quiz) }, icon = ErgosIcons.Pencil, trailing = ErgosIcons.ChevronRight)
            }
            SecondaryButton("Mis documentos y certificado", onClick = { vm.open(Overlay.Documents) }, icon = ErgosIcons.Scroll)
            BackLink("Volver a las lecciones", onClick = vm::previousPage)
        },
    ) {
        CenteredMessage(
            title = "¡Terminaste las lecciones!",
            body = if (vm.quizComplete) "Ya respondiste el cuestionario: tus resultados están en la pestaña Resultados."
            else "Cuando respondas el cuestionario podrás ver tus resultados.",
            icon = ErgosIcons.Check,
        )
        if (!vm.courseClosed) Hint("Tu certificado estará listo cuando el maestro cierre el curso.")
    }
}

// ---------- Resultados ----------

@Composable
fun ResultsLockedScreen(vm: DonesViewModel) {
    Screen(
        bottom = { PrimaryButton("Ir al cuestionario", onClick = { vm.openTab(MainTab.Quiz) }, icon = ErgosIcons.Pencil, trailing = ErgosIcons.ChevronRight) },
    ) {
        CenteredMessage(
            title = "Tus resultados te esperan",
            body = "Se abren cuando termines el cuestionario. Llevas ${vm.quizAnswered} de ${vm.quizTotal} afirmaciones.",
            icon = ErgosIcons.Shield,
        )
    }
}

private val sectionNames = listOf("Dominantes", "Subordinados", "Tu curva", "Dónde servir", "Tu personaje", "La batalla")

/** Las 6 hojas del PDF, adaptadas al teléfono: una sección a la vez. */
@Composable
fun ResultsTabScreen(vm: DonesViewModel) {
    val context = LocalContext.current
    val texts = remember { PdfTexts(context) }
    var section by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val chipsScroll = rememberScrollState()
    val result = vm.result
    key(section) {
        Screen(
            top = { SectionChips(section, chipsScroll) { section = it } },
            bottom = {
                PrimaryButton(
                    "Guardar mi PDF para imprimir",
                    onClick = {
                        error = runCatching { Sharing.open(context, vm.resultsPdf()) }
                            .exceptionOrNull()?.let { "No se pudo abrir el PDF: ${it.message}" }
                    },
                    icon = ErgosIcons.Document,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            },
        ) {
            when (section) {
                0 -> {
                    SectionIntro("Tus dones dominantes", "Son los que Dios ha puesto con más fuerza en ti.")
                    if (result.dominant.isEmpty()) Body("Todavía no hay suficientes respuestas para mostrar tus dones.")
                    result.dominant.forEach { GiftBlock(vm, texts, it) }
                }
                1 -> {
                    SectionIntro("Tus dones subordinados", "Acompañan y fortalecen a tus dones dominantes. Son herramientas que Dios también puso en tus manos.")
                    if (result.subordinate.isEmpty()) Body("Aún no hay dones subordinados.")
                    result.subordinate.forEach { GiftBlock(vm, texts, it) }
                }
                2 -> CurveTab(vm)
                3 -> MinistriesSection(vm, texts)
                4 -> CharacterSection(vm, texts)
                else -> BattleSection(vm)
            }
        }
    }
}

@Composable
private fun SectionChips(selected: Int, scroll: ScrollState, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sectionNames.forEachIndexed { i, name ->
            val on = i == selected
            val shape = RoundedCornerShape(24.dp)
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .background(if (on) Ergos.ButtonBottom else Ergos.Surface)
                    .border(1.5.dp, if (on) Ergos.VioletHi else Ergos.Line, shape)
                    .clickable(role = Role.Tab, onClick = { onSelect(i) })
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, lineHeight = 20.sp),
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) Color.White else Ergos.Ink2,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SectionIntro(title: String, sub: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Title(title, center = true)
        Ornament()
        Body(sub, center = true, color = Ergos.Ink2)
    }
}

@Composable
private fun GiftBlock(vm: DonesViewModel, texts: PdfTexts, s: GiftScore) {
    val t = texts.gifts[s.gift.code]
    ErgosCard(ornament = true, background = Ergos.Surface2, border = Ergos.Violet.copy(alpha = .5f), padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${s.rank}", style = MaterialTheme.typography.titleLarge, color = Ergos.Gold)
            Text(s.gift.name, style = MaterialTheme.typography.titleLarge, color = Ergos.Ink, modifier = Modifier.weight(1f))
            Text("${s.percent} %", style = MaterialTheme.typography.labelLarge, color = Ergos.AccentText)
        }
        Body(s.gift.definition, color = Ergos.Ink2)
        if (t != null) {
            if (t.life.isNotBlank()) {
                Eyebrow("Así se ve en tu vida", icon = ErgosIcons.Sparkle)
                Body(t.life)
            }
            t.traits.forEach { tr ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(ErgosIcons.Check, contentDescription = null, tint = Ergos.Positive, modifier = Modifier.padding(top = 4.dp).size(20.dp))
                    Body(tr)
                }
            }
            if (t.serve.isNotBlank()) {
                Eyebrow("Cómo puedes servir", icon = ErgosIcons.Heart)
                Body(t.serve)
            }
            if (t.care.isNotBlank()) {
                Eyebrow("Cuida tu don", icon = ErgosIcons.Shield)
                Body(t.care)
            }
        }
        s.gift.refs.forEach { r -> RefChip(r, onClick = { vm.openScripture(r) }) }
    }
}

@Composable
private fun CurveTab(vm: DonesViewModel) {
    val result = vm.result
    val others = result.ranking.filter { it !in result.dominant && it !in result.subordinate }
    SectionIntro(
        "Tu curva completa",
        "Cada don se midió con 5 frases de hasta 3 puntos: el máximo es 15. Los dones con pocos puntos no son una falla: allí otros miembros del cuerpo te completan, porque «ni el ojo puede decir a la mano: No te necesito» (1 Co 12:21).",
    )
    CurveGroup(vm, "Dominantes", result.dominant, Ergos.Gold)
    CurveGroup(vm, "Subordinados", result.subordinate, Ergos.VioletHi)
    CurveGroup(vm, "Tus otros dones", others, Ergos.LineStrong)
}

@Composable
private fun CurveGroup(vm: DonesViewModel, title: String, scores: List<GiftScore>, color: Color) {
    if (scores.isEmpty()) return
    ErgosCard(padding = 18.dp) {
        Eyebrow(title, color = if (color == Ergos.LineStrong) Ergos.AccentText else color)
        scores.forEach { s ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { vm.open(Overlay.GiftDetail(s.gift.code)) }
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Body("${s.rank}. ${s.gift.name}", Modifier.weight(1f))
                    Text("${s.raw} de ${s.max}", style = MaterialTheme.typography.labelLarge, color = Ergos.Ink2)
                }
                Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Ergos.Line)) {
                    Box(
                        Modifier
                            .fillMaxWidth(s.fraction.toFloat().coerceIn(0f, 1f))
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(color.copy(alpha = .75f), color))),
                    )
                }
            }
        }
    }
}

@Composable
private fun MinistriesSection(vm: DonesViewModel, texts: PdfTexts) {
    val suggestions = vm.result.ministries
    SectionIntro("Dónde podrías servir", "Ministerios que encajan con tus dones. Tu pastor te ayudará a confirmarlos.")
    if (suggestions.isEmpty()) Body("Aún no hay sugerencias. Conversa tu resultado con tu líder.")
    suggestions.forEach { s ->
        val t = texts.ministries[s.ministry.name]
        ErgosCard(padding = 18.dp) {
            Text(s.ministry.name, style = MaterialTheme.typography.titleLarge, color = Ergos.Ink)
            Eyebrow("Por tus dones", icon = ErgosIcons.Sparkle, color = Ergos.Gold)
            Body(s.matchingGifts.joinToString(", ") { it.name }, color = Ergos.Ink2)
            if (t != null) {
                if (t.what.isNotBlank()) {
                    Eyebrow("En qué consiste")
                    Body(t.what)
                }
                if (t.you.isNotBlank()) {
                    Eyebrow("Cómo te encaja")
                    Body(t.you)
                }
                if (t.bible.isNotBlank()) {
                    Eyebrow("En la Biblia", icon = ErgosIcons.Book)
                    Body(t.bible)
                }
                t.refs.forEach { r -> RefChip(r, onClick = { vm.openScripture(r) }) }
            }
        }
    }
}

// ---------- Tu personaje ----------

private class CharTheme(val accent: Color, val words: List<String>, val motto: String)

private fun charTheme(context: Context, code: String): CharTheme = runCatching {
    val o = JSONObject(context.assets.open("personajes-tema.json").bufferedReader().use { it.readText() })
        .getJSONObject("characters").getJSONObject(code)
    val w = o.getJSONArray("words")
    CharTheme(Color(android.graphics.Color.parseColor(o.getString("accent"))), (0 until w.length()).map { w.getString(it) }, o.optString("motto"))
}.getOrElse { CharTheme(Ergos.AccentText, emptyList(), "") }

/** Busca assets/<carpeta>/<prefijo>-… (png, jpg…). */
private fun findAsset(context: Context, folder: String, prefix: String): String? =
    runCatching {
        context.assets.list(folder)?.firstOrNull { it.startsWith("$prefix-") || it.startsWith("$prefix.") }
    }.getOrNull()?.let { "$folder/$it" }

@Composable
private fun rememberAssetImage(path: String?, sample: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(path) {
        if (path == null) null else runCatching {
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
        }.getOrNull()
    }
}

@Composable
private fun CharacterSection(vm: DonesViewModel, texts: PdfTexts) {
    val context = LocalContext.current
    val result = vm.result
    val top = result.dominant.firstOrNull() ?: result.ranking.firstOrNull()
    val code = top?.gift?.code
    val ch = code?.let { texts.gifts[it]?.character }
    if (code == null || ch == null) {
        Body("Aún no hay un personaje para mostrar.")
        return
    }
    val th = remember(code) { charTheme(context, code) }
    val art = rememberAssetImage(remember(code) { findAsset(context, "esculturas", code) }, 2)
    val icon = rememberAssetImage("iconos/$code.png", 1)
    val others = result.dominant.drop(1).map { it.gift.name }
    val extra = when (others.size) {
        0 -> ""
        1 -> " Además, Dios puso en ti el don de ${others[0]}: juntos forman tu manera única de servir."
        else -> " Además, Dios puso en ti los dones de ${others.dropLast(1).joinToString(", ")} y ${others.last()}: juntos forman tu manera única de servir."
    }
    if (art != null) {
        Image(
            bitmap = art,
            contentDescription = "Escultura de ${ch.name}",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(art.width.toFloat() / art.height.toFloat())
                .clip(RoundedCornerShape(12.dp)),
        )
    }
    val number = vm.content.gifts.indexOfFirst { it.code == code } + 1
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SpacedCaps("N.º ${"%02d".format(number)}  ·  Tu personaje bíblico", Ergos.Ink2, 12.sp)
        PosterItalic("Eres como", Ergos.Ink, 30.sp)
        PosterName(ch.name, 68.sp)
        if (th.words.isNotEmpty()) LinkedChips(th.words.take(3), th.accent)
    }
    // El versículo, tal cual, como en el PDF: comilla grande, texto serif y la cita en versalitas.
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("“", style = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 40.sp, brush = GoldText))
        Text(ch.quote, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Cormorant, fontWeight = FontWeight.Medium, lineHeight = 36.sp), color = Ergos.Ink)
        SpacedCaps("—  ${ch.ref}  ·  RVR1960", th.accent, 12.sp, center = false)
        RefChip(ch.ref, onClick = { vm.openScripture(ch.ref) })
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SpacedCaps("↘↘  Su historia, tu llamado", th.accent, 12.sp, center = false)
        Text(ch.text + extra, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 23.sp, lineHeight = 33.sp), color = Ergos.Ink)
    }
    if (th.motto.isNotEmpty()) {
        ErgosCard(padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = null, colorFilter = ColorFilter.tint(th.accent), modifier = Modifier.size(56.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SpacedCaps("Su lema", th.accent, 12.sp, center = false)
                    PosterItalic("«${th.motto}»", Ergos.Ink, 26.sp, center = false)
                }
            }
        }
    }
    ErgosCard(padding = 18.dp) {
        Eyebrow("Tus dones dominantes", icon = ErgosIcons.Heart)
        Body(result.dominant.joinToString("  ·  ") { it.gift.name }, color = Ergos.Ink2)
    }
}

// ---------- La batalla ----------

@Composable
private fun BattleSection(vm: DonesViewModel) {
    val context = LocalContext.current
    val b = remember { BattleText.load(context) }
    val result = vm.result
    val doms = (result.dominant.ifEmpty { result.ranking.take(1) }).take(3)
    val code = doms.firstOrNull()?.gift?.code
    // Imagen larga de la app: assets/batalla/<código>-…-app.jpg; si no, una sin sufijo; si no, la general.
    val path = remember(code) {
        code?.let { c ->
            runCatching {
                context.assets.list("batalla")?.filter { it.startsWith("$c-") || it.startsWith("$c.") }?.let { l ->
                    l.firstOrNull { it.substringBeforeLast('.').endsWith("-app") }
                        ?: l.firstOrNull { !it.substringBeforeLast('.').endsWith("-pdf") }
                }
            }.getOrNull()?.let { "batalla/$it" }
        } ?: "batalla/default.jpg"
    }
    val art = rememberAssetImage(path, 2)
    // Opción B: la imagen larga queda de fondo (a todo el ancho, sin recortar ni estirar), baja fundiéndose a
    // rojo oscuro y todo el texto se desliza por encima.
    val deepRed = Color(0xFF2A0618)
    val shape = RoundedCornerShape(16.dp)
    val rose = Color(0xFFFFB8C2)
    val roseHi = Color(0xFFFFE3E7)
    val alarm = Color(0xFFFF6B76)
    // La tira se recorre entera: mientras bajas, avanza más lento que el texto, de modo que al llegar
    // al final del texto se ve justo el final de la tira (sin estirarla ni recortarla).
    var boxTop by remember { mutableStateOf(0f) }
    var boxH by remember { mutableStateOf(1f) }
    val viewH = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val imgHpx = with(LocalDensity.current) { if (art != null) (maxWidth * (art.height.toFloat() / art.width.toFloat())).toPx() else 0f }
        val travel = (boxH - imgHpx).coerceAtLeast(0f)
        val progress = if (boxH > viewH) ((viewH * .15f - boxTop) / (boxH - viewH * .85f)).coerceIn(0f, 1f) else 0f
        Box(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { boxTop = it.positionInWindow().y; boxH = it.size.height.toFloat() }
                .clip(shape)
                .background(deepRed)
                .border(1.5.dp, Ergos.Negative.copy(alpha = .7f), shape),
        ) {
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .graphicsLayer { translationY = travel * progress },
                )
                // Velo para que el texto se lea siempre sobre la imagen.
                Box(Modifier.matchParentSize().background(Color(0xFF1A0410).copy(alpha = .35f)))
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SpacedCaps("N.º 06  ·  Advertencia", roseHi, 12.sp)
                PosterItalic("Prepárate para", roseHi, 30.sp)
                SwordsTitle({ PosterName("La batalla", 56.sp, RoseText) })
                SpacedCaps("Por tus dones", alarm, 13.sp)
                // Espacio para que se vea la escultura antes del texto.
                Spacer(Modifier.height(220.dp))
                Text(
                    b?.intro.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 23.sp, lineHeight = 32.sp, shadow = Shadow(Color.Black, Offset(0f, 3f), 6f)),
                    color = Ergos.Ink,
                    textAlign = TextAlign.Center,
                )
                doms.forEach { s ->
                    val g = b?.gifts?.get(s.gift.code) ?: return@forEach
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2A0712).copy(alpha = .82f))
                            .border(1.5.dp, alarm.copy(alpha = .6f), RoundedCornerShape(14.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            SwordIcon(20.dp, rose, -90f)
                            Spacer(Modifier.width(10.dp))
                            SpacedCaps(s.gift.name, roseHi, 15.sp)
                            Spacer(Modifier.width(10.dp))
                            SwordIcon(20.dp, rose, 90f)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(ErgosIcons.Close, contentDescription = null, tint = alarm, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            SpacedCaps("La mentira", alarm, 12.sp, center = false)
                        }
                        PosterItalic("«${g.lie}»", Ergos.Ink, 28.sp, center = false)
                        Body(g.attack, color = Color(0xFFE7A9B3))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SwordIcon(18.dp, roseHi)
                            Spacer(Modifier.width(8.dp))
                            SpacedCaps("La verdad", roseHi, 12.sp, center = false)
                        }
                        Text(g.truth, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 23.sp, lineHeight = 32.sp), color = Ergos.Ink)
                        if (g.ref.isNotBlank()) RefChip(g.ref, onClick = { vm.openScripture(g.ref) })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(ErgosIcons.Shield, contentDescription = null, tint = roseHi, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            SpacedCaps("Cómo resistir", roseHi, 12.sp, center = false)
                        }
                        Body(g.defense, color = Ergos.Ink2)
                    }
                }
                if (b != null) {
                    PosterItalic(b.closing, Ergos.Ink, 24.sp)
                    if (b.closingRef.isNotBlank()) SpacedCaps("${b.closingRef}  ·  RVR1960", rose, 12.sp)
                }
            }
        }
    }
    DoorCard(ErgosIcons.Scroll, "Mis documentos y certificado", onClick = { vm.open(Overlay.Documents) }, subtitle = "Abrir o enviar por WhatsApp")
}
