package com.dones.app.ui

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
import androidx.compose.ui.res.painterResource
import com.dones.app.R
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

// W0: "¿Quién eres?" — los perfiles guardados en este teléfono.
@Composable
fun ProfilesScreen(vm: DonesViewModel) {
    Screen(bottom = { SecondaryButton("Soy una persona nueva", icon = ErgosIcons.Plus, onClick = vm::startNewProfile) }) {
        PageHeader(
            title = "¿Quién eres?",
            icon = ErgosIcons.People,
            eyebrow = "Llamados",
            subtitle = "Toca tu nombre para seguir donde quedaste.",
        )
        vm.book.profiles.sortedByDescending { it.id == vm.book.activeId }.forEach { p ->
            DoorCard(
                icon = ErgosIcons.Person,
                title = p.name.ifBlank { "Persona sin nombre" },
                subtitle = progressLabel(vm, p),
                highlighted = p.id == vm.book.activeId,
                onClick = { vm.selectProfile(p.id) },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.Icon(ErgosIcons.Shield, contentDescription = null, tint = Ergos.Muted, modifier = Modifier.size(22.dp))
            Hint("Tus datos y respuestas se guardan solo en este teléfono.")
        }
    }
}

// Inicio: se abre desde la barra de arriba.
@Composable
fun HomeScreen(vm: DonesViewModel) {
    val p = vm.participant
    var confirmDelete by remember { mutableStateOf(false) }
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
            onClick = { confirmDelete = true },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Ergos.Surface2,
            icon = { androidx.compose.material3.Icon(ErgosIcons.Trash, contentDescription = null, tint = Ergos.Negative) },
            title = { Text("¿Borrar tu perfil?") },
            text = { Text("Se borran tus respuestas, lo que escribiste y tus PDFs de este teléfono. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteCurrentProfile() }) { Text("Sí, borrar", color = Ergos.Negative) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("No", color = Ergos.AccentText) } },
        )
    }
}

/** Barra de arriba en la clase: "Inicio", el emblema de la iglesia y, si volvió atrás, "Ir a donde quedé". */
@Composable
fun ClassTopBar(vm: DonesViewModel) {
    val behind = vm.participant.isBehind
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
                painterResource(R.drawable.emblema_san_andres),
                contentDescription = "Iglesia Anglicana San Andrés",
                colorFilter = ColorFilter.tint(Ergos.Violet),
                modifier = Modifier.padding(end = 12.dp).size(width = 52.dp, height = 42.dp),
            )
        }
    }
}
