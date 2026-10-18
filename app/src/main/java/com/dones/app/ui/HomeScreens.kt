package com.dones.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.semantics.Role
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
    Screen(bottom = { SecondaryButton("+  Soy una persona nueva", onClick = vm::startNewProfile) }) {
        Title("¿Quién eres?")
        Body("Toca tu nombre para seguir donde quedaste.")
        vm.book.profiles.sortedByDescending { it.id == vm.book.activeId }.forEach { p ->
            Card(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = BigTouch)
                    .clickable(role = Role.Button) { vm.selectProfile(p.id) },
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.name.ifBlank { "Persona sin nombre" }, style = MaterialTheme.typography.titleLarge)
                    Hint(progressLabel(vm, p))
                }
            }
        }
        Hint("Tus datos y respuestas se guardan solo en este teléfono.")
    }
}

// Inicio: se abre desde la barra de arriba.
@Composable
fun HomeScreen(vm: DonesViewModel) {
    val p = vm.participant
    var confirmDelete by remember { mutableStateOf(false) }
    Screen(bottom = { BackLink(onClick = vm::closeOverlay) }) {
        Title("Hola, ${p.name.trim().split(" ").firstOrNull().orEmpty()}")
        Hint(progressLabel(vm, p))
        PrimaryButton("Seguir donde quedé ›", onClick = vm::goToFurthest)
        SecondaryButton("Ver la lección desde el principio", onClick = vm::goToStart)
        SecondaryButton("Cambiar de persona", onClick = vm::switchProfile)
        BackLink("Borrar mi perfil de este teléfono", onClick = { confirmDelete = true })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Borrar tu perfil?") },
            text = { Text("Se borran tus respuestas, lo que escribiste y tus PDFs de este teléfono. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteCurrentProfile() }) { Text("Sí, borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("No") } },
        )
    }
}

/** Barra de arriba en la clase: "⌂ Inicio" y, si volvió atrás, "Ir a donde quedé ›". */
@Composable
fun ClassTopBar(vm: DonesViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackLink("⌂ Inicio", onClick = { vm.open(com.dones.app.Overlay.Home) })
        if (vm.participant.isBehind) BackLink("Ir a donde quedé ›", onClick = vm::goToFurthest)
    }
}
