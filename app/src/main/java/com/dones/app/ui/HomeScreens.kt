package com.dones.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.dones.app.DonesViewModel
import com.dones.core.ParticipantState

/** Dónde va la persona, en palabras: "Va en: Corazón" / "Terminó el curso". */
private fun progressLabel(vm: DonesViewModel, p: ParticipantState): String {
    if (!p.onboardingDone) return "Falta completar sus datos"
    val pages = vm.lesson.pages
    val at = p.furthestPage
    if (at >= pages.size) return "Terminó el curso ✓"
    val page = pages[at]
    return page.houtsBlock?.let { "Va en: preguntas, bloque $it de ${vm.content.blocks.size}" } ?: "Va en: ${page.section}"
}

// W0: "¿Quién va a responder hoy?" — los perfiles guardados en este teléfono (mockup).
@Composable
fun ProfilesScreen(vm: DonesViewModel) {
    Screen(
        bottom = {
            Text(
                "Iglesia Anglicana San Andrés",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = CormorantItalic, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 17.sp),
                color = Ergos.Muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Ornament(width = 40.dp, color = Ergos.VioletHi)
            PosterName("Llamados", 58.sp, androidx.compose.ui.graphics.SolidColor(Ergos.Ink))
            Eyebrow("Descubriendo mi ministerio")
            Ornament(width = 140.dp)
            Spacer(Modifier.size(6.dp))
            PosterItalic("¿Quién va a responder hoy?", Ergos.Ink2, 26.sp)
        }
        Column(Modifier.fillMaxWidth()) {
            vm.book.profiles.sortedByDescending { it.id == vm.book.activeId }.forEach { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) { vm.selectProfile(p.id) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(p.name.ifBlank { "Persona sin nombre" }, style = MaterialTheme.typography.headlineSmall, color = Ergos.Ink)
                        Text(progressLabel(vm, p).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 1.8.sp), color = Ergos.Muted)
                    }
                    androidx.compose.material3.Icon(ErgosIcons.ChevronRight, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(24.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Ergos.Line))
            }
        }
        BackLink("Agregar a otra persona", icon = ErgosIcons.Plus, onClick = vm::startNewProfile)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.Icon(ErgosIcons.Shield, contentDescription = null, tint = Ergos.Muted, modifier = Modifier.size(20.dp))
            Hint("Tus datos y respuestas se guardan solo en este teléfono.")
        }
    }
}

// Inicio: se abre desde la barra de arriba.
@Composable
fun HomeScreen(vm: DonesViewModel) {
    val p = vm.participant
    // 0 = nada, 1 = primera pregunta, 2 = segunda (definitiva)
    var confirmStep by remember { mutableStateOf(0) }
    Screen(bottom = { BackLink("Volver a la lección", onClick = vm::closeOverlay) }) {
        PageHeader(
            title = "Hola, ${p.name.trim().split(" ").firstOrNull().orEmpty()}",
            icon = ErgosIcons.Church,
            eyebrow = "Inicio",
            subtitle = progressLabel(vm, p),
        )
        DoorCard(ErgosIcons.Bookmark, "Seguir donde quedé", onClick = vm::goToFurthest, subtitle = "Vuelve a tu última página", highlighted = true)
        DoorCard(ErgosIcons.Restart, "Ver la lección desde el principio", onClick = vm::goToStart, subtitle = "Tus respuestas no se borran")
        DoorCard(ErgosIcons.People, "Cambiar de persona", onClick = vm::switchProfile, subtitle = "Otro perfil en este teléfono")
        Spacer(Modifier.size(4.dp))
        BackLink(
            "Borrar mi perfil de este teléfono",
            icon = ErgosIcons.Trash,
            color = Ergos.Negative,
            onClick = { confirmStep = 1 },
        )
    }
    if (confirmStep == 1) {
        AlertDialog(
            onDismissRequest = { confirmStep = 0 },
            containerColor = Ergos.Surface2,
            icon = { androidx.compose.material3.Icon(ErgosIcons.Trash, contentDescription = null, tint = Ergos.Negative) },
            title = { Text("¿Borrar tu perfil?") },
            text = { Text("Se borran tus respuestas, lo que escribiste y tus PDFs de este teléfono. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmStep = 2 }) { Text("Sí, borrar", color = Ergos.Negative) }
            },
            dismissButton = { TextButton(onClick = { confirmStep = 0 }) { Text("No, conservar", color = Ergos.AccentText) } },
        )
    }
    if (confirmStep == 2) {
        AlertDialog(
            onDismissRequest = { confirmStep = 0 },
            containerColor = Ergos.Surface2,
            icon = { androidx.compose.material3.Icon(ErgosIcons.Trash, contentDescription = null, tint = Ergos.Negative) },
            title = { Text("¿Seguro que quieres borrar a ${p.name.trim().ifBlank { "esta persona" }}?") },
            text = { Text("Última pregunta: si borras este perfil, ya no se podrá recuperar. Toca «No, conservar» si fue sin querer.") },
            confirmButton = {
                TextButton(onClick = { confirmStep = 0; vm.deleteCurrentProfile() }) { Text("Sí, borrar para siempre", color = Ergos.Negative) }
            },
            dismissButton = { TextButton(onClick = { confirmStep = 0 }) { Text("No, conservar", color = Ergos.AccentText) } },
        )
    }
}

/** Barra de arriba en la clase: "Inicio", el emblema de la iglesia y, si volvió atrás, "Ir a donde quedé". */
@Composable
fun ClassTopBar(vm: DonesViewModel) {
    val behind = vm.isBehind
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackLink("Inicio", icon = ErgosIcons.Church, onClick = { vm.open(com.dones.app.Overlay.Home) })
        Spacer(Modifier.weight(1f))
        if (behind) {
            BackLink("Ir a donde quedé", icon = ErgosIcons.Bookmark, trailing = ErgosIcons.ChevronRight, color = Ergos.Gold, onClick = vm::goToFurthest)
        } else {
            Image(
                ChurchLogo.emblem(),
                contentDescription = "Iglesia Anglicana San Andrés",
                colorFilter = ColorFilter.tint(Ergos.Violet),
                modifier = Modifier.padding(end = 12.dp).size(width = 52.dp, height = 42.dp),
            )
        }
    }
}
