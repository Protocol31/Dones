package com.dones.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
                PrimaryButton("Mis documentos y certificado", onClick = { vm.open(Overlay.Documents) })
            } else {
                SecondaryButton("Mis documentos", onClick = { vm.open(Overlay.Documents) })
            }
        },
    ) {
        Title("${firstName(vm.participant.name)}, estos son tus dones principales")
        if (result.dominant.isEmpty()) {
            Body("Todavía no hay suficientes respuestas para mostrar tus dones.")
        }
        result.dominant.forEach { score ->
            GiftCard(score, onClick = { vm.open(Overlay.GiftDetail(score.gift.code)) })
        }
        Body("Es un punto de partida: confírmalo con otros que te conocen y con tu pastor.")
        if (vm.quality.needsConversation) {
            Card(Modifier.fillMaxWidth()) {
                Body(
                    "Tus respuestas muestran un patrón poco común. Te sugerimos conversar tu resultado con tu líder.",
                    Modifier.padding(16.dp),
                )
            }
        }
        SecondaryButton("Ver mi curva completa", onClick = { vm.open(Overlay.Curve) })
        SecondaryButton("Dónde podría servir", onClick = { vm.open(Overlay.Ministries) })
        if (!vm.courseClosed) Hint("Tu certificado estará listo cuando el maestro cierre el curso.")
    }
}

@Composable
private fun GiftCard(score: GiftScore, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BigTouch)
            .clickable(onClickLabel = "Ver qué significa", role = Role.Button, onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${score.rank}", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.width(44.dp))
            Column(Modifier.weight(1f)) {
                Text(score.gift.name, style = MaterialTheme.typography.titleLarge)
                Hint("${score.raw} de ${score.max} · Ver qué significa ›")
            }
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
            if (!showAll && others.size > 3) SecondaryButton("Mostrar todos", onClick = { showAll = true })
            BackLink(onClick = vm::closeOverlay)
        },
    ) {
        Title("Tu curva de dones")
        CurveSection("Dominantes", result.dominant, vm)
        CurveSection("Subordinados", result.subordinate, vm)
        CurveSection("Otros", if (showAll) others else others.take(3), vm)
    }
}

@Composable
private fun CurveSection(title: String, scores: List<GiftScore>, vm: DonesViewModel) {
    if (scores.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium)
    scores.forEach { s ->
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { vm.open(Overlay.GiftDetail(s.gift.code)) }
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Body(s.gift.name, Modifier.weight(1f))
                Body("${s.percent} %")
            }
            LinearProgressIndicator(progress = { s.fraction.toFloat() }, modifier = Modifier.fillMaxWidth().heightIn(min = 14.dp))
        }
    }
}

// W13
@Composable
fun GiftDetailScreen(vm: DonesViewModel, code: String) {
    val gift = vm.content.gift(code)
    val ministries = vm.content.ministries.filter { code in it.gifts }
    Screen(bottom = { BackLink(onClick = vm::closeOverlay) }) {
        Title(gift.name)
        Body(gift.definition)
        Hint(gift.refs.joinToString(" · "))
        if (ministries.isNotEmpty()) {
            Text("Dónde podrías servir", style = MaterialTheme.typography.titleMedium)
            ministries.forEach { Body("• ${it.name}") }
        }
    }
}

@Composable
fun MinistriesScreen(vm: DonesViewModel) {
    val suggestions = vm.result.ministries
    Screen(bottom = { BackLink(onClick = vm::closeOverlay) }) {
        Title("Dónde podrías servir")
        if (suggestions.isEmpty()) Body("Aún no hay sugerencias. Conversa tu resultado con tu líder.")
        suggestions.forEach { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(s.ministry.name, style = MaterialTheme.typography.titleMedium)
                    Hint("Por tus dones: " + s.matchingGifts.joinToString(", ") { it.name })
                }
            }
        }
        Hint("Sugerencias iniciales. Tu pastor te ayudará a confirmarlas.")
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
        Title("Mis documentos")
        SecondaryButton("📄  Mis dones (PDF) · Abrir", onClick = {
            error = runCatching { Sharing.open(context, vm.resultsPdf()) }.exceptionOrNull()?.message
        })
        if (vm.courseClosed) {
            SecondaryButton("📄  Certificado (PDF) · Abrir", onClick = {
                error = runCatching { vm.certificatePdf()?.let { Sharing.open(context, it) } }.exceptionOrNull()?.message
            })
        } else {
            Hint("El certificado aparece cuando el maestro cierra el curso.")
        }
        Body(
            if (p.whatsappDeclined) "No diste un WhatsApp. Puedes enviártelos tú con el botón de abajo."
            else "Se enviarán a tu WhatsApp ${p.whatsapp}."
        )
        Hint("Demo: el botón abre WhatsApp con los PDFs adjuntos; elige tu propio chat y toca Enviar. Más adelante llegarán solos.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
