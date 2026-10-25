package com.dones.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dones.app.DonesViewModel
import com.dones.app.Overlay
import com.dones.app.share.Sharing
import com.dones.core.GiftScore

private fun firstName(full: String) = full.trim().split(" ").firstOrNull().orEmpty()

// W11: la pantalla más importante.
@Composable
fun ResultsScreen(vm: DonesViewModel) {
    val result = vm.result
    Screen(
        bottom = {
            if (vm.courseClosed) {
                PrimaryButton("Mis documentos y certificado", icon = ErgosIcons.Scroll, onClick = { vm.open(Overlay.Documents) })
            } else {
                SecondaryButton("Mis documentos", icon = ErgosIcons.Document, onClick = { vm.open(Overlay.Documents) })
            }
            BackLink("Volver al curso", onClick = vm::previousPage)
        },
    ) {
        PageHeader(
            title = "${firstName(vm.participant.name)}, estos son tus dones principales",
            icon = ErgosIcons.Sparkle,
            eyebrow = "Tus resultados",
        )
        if (result.dominant.isEmpty()) {
            Body("Todavía no hay suficientes respuestas para mostrar tus dones.")
        }
        result.dominant.forEach { score ->
            GiftCard(score, onClick = { vm.open(Overlay.GiftDetail(score.gift.code)) })
        }
        ErgosCard(padding = 16.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(ErgosIcons.Cross, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(28.dp))
                Body("Es un punto de partida: confírmalo con otros que te conocen y con tu pastor.", color = Ergos.Ink2)
            }
        }
        if (vm.quality.needsConversation) {
            ErgosCard(border = Ergos.Gold.copy(alpha = .6f), padding = 16.dp) {
                Eyebrow("Para conversar", icon = ErgosIcons.Chat, color = Ergos.Gold)
                Body("Tus respuestas muestran un patrón poco común. Te sugerimos conversar tu resultado con tu líder.")
            }
        }
        DoorCard(ErgosIcons.Bars, "Ver mi curva completa", onClick = { vm.open(Overlay.Curve) }, subtitle = "Los 25 dones, de mayor a menor")
        DoorCard(ErgosIcons.Heart, "Dónde podría servir", onClick = { vm.open(Overlay.Ministries) }, subtitle = "Ministerios según tus dones")
        if (!vm.courseClosed) Hint("Tu certificado estará listo cuando el maestro cierre el curso.")
    }
}

/** Puesto del don dentro de un rombo dorado. */
@Composable
private fun RankDiamond(rank: Int) {
    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(56.dp)) {
            val w = size.width
            val d = Path().apply { moveTo(w / 2, 2f); lineTo(w - 2f, w / 2); lineTo(w / 2, w - 2f); lineTo(2f, w / 2); close() }
            drawPath(d, Brush.verticalGradient(listOf(Ergos.GoldHi.copy(alpha = .25f), Ergos.Gold.copy(alpha = .08f))))
            drawPath(d, Ergos.Gold, style = Stroke(1.5.dp.toPx()))
        }
        Text("$rank", style = MaterialTheme.typography.titleLarge, color = Ergos.GoldHi)
    }
}

@Composable
private fun GiftCard(score: GiftScore, onClick: () -> Unit) {
    ErgosCard(onClick = onClick, ornament = true, background = Ergos.Surface2, border = Ergos.Violet.copy(alpha = .5f), padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RankDiamond(score.rank)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(score.gift.name, style = MaterialTheme.typography.titleLarge, color = Ergos.Ink)
                Text("${score.percent} % · Ver qué significa", style = MaterialTheme.typography.labelMedium, color = Ergos.AccentText)
            }
            Icon(ErgosIcons.ChevronRight, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(24.dp))
        }
    }
}

// W12: barras horizontales ordenadas (más legibles que una curva de línea).
@Composable
fun CurveScreen(vm: DonesViewModel) {
    val result = vm.result
    var showAll by remember { mutableStateOf(false) }
    val others = result.ranking.filter { it !in result.dominant && it !in result.subordinate }
    Screen(
        bottom = {
            if (!showAll && others.size > 3) SecondaryButton("Mostrar todos", icon = ErgosIcons.Eye, onClick = { showAll = true })
            BackLink(onClick = vm::closeOverlay)
        },
    ) {
        PageHeader(title = "Tu curva de dones", icon = ErgosIcons.Bars, eyebrow = "Tus resultados")
        CurveSection("Dominantes", ErgosIcons.Sparkle, Ergos.Gold, result.dominant, vm)
        CurveSection("Subordinados", ErgosIcons.Heart, Ergos.VioletHi, result.subordinate, vm)
        CurveSection("Otros", ErgosIcons.Bars, Ergos.LineStrong, if (showAll) others else others.take(3), vm)
    }
}

@Composable
private fun CurveSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, scores: List<GiftScore>, vm: DonesViewModel) {
    if (scores.isEmpty()) return
    ErgosCard(padding = 18.dp) {
        Eyebrow(title, icon = icon, color = if (color == Ergos.LineStrong) Ergos.AccentText else color)
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
                    Body(s.gift.name, Modifier.weight(1f))
                    Text("${s.percent} %", style = MaterialTheme.typography.labelLarge, color = Ergos.Ink2)
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

// W13
@Composable
fun GiftDetailScreen(vm: DonesViewModel, code: String) {
    val gift = vm.content.gift(code)
    val ministries = vm.content.ministries.filter { code in it.gifts }
    Screen(bottom = { BackLink(onClick = vm::closeOverlay) }) {
        PageHeader(title = gift.name, icon = ErgosIcons.Sparkle, eyebrow = "Don espiritual")
        Body(gift.definition)
        if (gift.refs.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { gift.refs.forEach { r -> RefChip(r, onClick = { vm.openScripture(r) }) } }
        }
        if (ministries.isNotEmpty()) {
            ErgosCard(ornament = true) {
                Eyebrow("Dónde podrías servir", icon = ErgosIcons.Heart)
                ministries.forEach { m ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(ErgosIcons.ChevronRight, contentDescription = null, tint = Ergos.Gold, modifier = Modifier.size(18.dp))
                        Body(m.name)
                    }
                }
            }
        }
    }
}

@Composable
fun MinistriesScreen(vm: DonesViewModel) {
    val suggestions = vm.result.ministries
    Screen(bottom = { BackLink(onClick = vm::closeOverlay) }) {
        PageHeader(title = "Dónde podrías servir", icon = ErgosIcons.Heart, eyebrow = "Tus resultados")
        if (suggestions.isEmpty()) Body("Aún no hay sugerencias. Conversa tu resultado con tu líder.")
        suggestions.forEach { s ->
            ErgosCard(padding = 18.dp) {
                Text(s.ministry.name, style = MaterialTheme.typography.titleLarge)
                Eyebrow("Por tus dones", icon = ErgosIcons.Sparkle, color = Ergos.Gold)
                Body(s.matchingGifts.joinToString(", ") { it.name }, color = Ergos.Ink2)
            }
        }
        Hint("Sugerencias iniciales. Tu pastor te ayudará a confirmarlas.", center = true, modifier = Modifier.fillMaxWidth())
    }
}

// W14: los PDFs quedan en el teléfono y se envían por WhatsApp.
@Composable
fun DocumentsScreen(vm: DonesViewModel) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val p = vm.participant
    Screen(
        bottom = {
            PrimaryButton(
                "Enviarme por WhatsApp",
                icon = ErgosIcons.Chat,
                onClick = {
                    error = runCatching {
                        val files = listOfNotNull(vm.resultsPdf(), vm.certificatePdf())
                        Sharing.toWhatsApp(context, files, "Mis resultados de \"Descubriendo Mi Ministerio\"")
                    }.exceptionOrNull()?.let { "No se pudo abrir WhatsApp: ${it.message}" }
                },
            )
            BackLink(onClick = vm::closeOverlay)
        },
    ) {
        PageHeader(title = "Mis documentos", icon = ErgosIcons.Scroll)
        DoorCard(ErgosIcons.Document, "Mis dones (PDF)", subtitle = "Tocar para abrir", onClick = {
            error = runCatching { Sharing.open(context, vm.resultsPdf()) }.exceptionOrNull()?.message
        })
        if (vm.courseClosed) {
            DoorCard(ErgosIcons.Scroll, "Certificado (PDF)", subtitle = "Tocar para abrir", onClick = {
                error = runCatching { vm.certificatePdf()?.let { Sharing.open(context, it) } }.exceptionOrNull()?.message
            })
        } else {
            Hint("El certificado aparece cuando el maestro cierra el curso.")
        }
        Body(
            if (p.whatsappDeclined) "No diste un WhatsApp. Puedes enviártelos tú con el botón de abajo."
            else "Se enviarán a tu WhatsApp ${p.whatsapp}.",
        )
        Hint("Demo: el botón abre WhatsApp con los PDFs adjuntos; elige tu propio chat y toca Enviar. Más adelante llegarán solos.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start) }
    }
}
