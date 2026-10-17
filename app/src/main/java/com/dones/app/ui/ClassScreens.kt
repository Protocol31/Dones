package com.dones.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dones.app.DonesViewModel
import com.dones.core.Blanks
import com.dones.core.ParticipantView
import kotlinx.coroutines.delay

// W6/W7: una página de la enseñanza. Texto, completar espacios o pregunta abierta; la persona avanza sola.
@Composable
fun LessonScreen(vm: DonesViewModel, v: ParticipantView.LessonStep) {
    val page = v.page
    val key = v.index
    val fields = if (page.write) 1 else page.blanks.size
    val values = remember(key) {
        mutableStateListOf(*Array(fields) { i -> vm.participant.notes["$key.$i"].orEmpty() })
    }
    // Una página de espacios ya revisada se muestra con sus respuestas al volver a ella.
    var checked by remember(key) { mutableStateOf(page.blanks.isNotEmpty() && "$key.0" in vm.participant.notes) }
    val mustCheck = page.blanks.isNotEmpty() && !checked
    Screen(
        top = {
            ProgressHeader(
                label = "${page.section} · ${v.positionInSection} de ${v.sectionSize}",
                fraction = v.positionInSection / v.sectionSize.toFloat(),
            )
        },
        bottom = {
            if (mustCheck) {
                PrimaryButton("Revisar", onClick = { vm.saveNotes(key, values.toList()); checked = true })
            } else {
                PrimaryButton("Siguiente ›", onClick = {
                    if (page.write) vm.saveNotes(key, values.toList())
                    vm.nextPage()
                })
            }
            if (key > 0) BackLink("‹ Anterior", onClick = vm::previousPage)
        },
    ) {
        page.title?.let { Title(it) }
        page.ref?.let { Hint(it) }
        Text(numberBlanks(page.text, page.blanks.size > 1), style = MaterialTheme.typography.bodyLarge)

        page.blanks.forEachIndexed { i, expected ->
            OutlinedTextField(
                value = values[i],
                onValueChange = { values[i] = it },
                readOnly = checked,
                singleLine = true,
                label = { Text(if (fields > 1) "Espacio ${i + 1}" else "Tu respuesta") },
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            if (checked) {
                if (Blanks.isCorrect(expected, values[i])) {
                    Text("✓ ¡Correcto!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Respuesta: ${Blanks.display(expected)}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (mustCheck) Hint("Escribe lo que crees y toca Revisar. Si no sabes, déjalo en blanco.")

        if (page.write) {
            OutlinedTextField(
                value = values[0],
                onValueChange = { values[0] = it },
                minLines = 4,
                label = { Text("Escribe aquí (opcional)") },
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Con varios espacios en la misma página: "(1) ______", "(2) ______"… para saber cuál es cuál. */
private fun numberBlanks(text: String, numbered: Boolean): String {
    var i = 0
    return Regex.fromLiteral(Blanks.MARK).replace(text) { i++; if (numbered) "($i) ______" else "______" }
}

// W8: una afirmación por pantalla. Al tocar se marca, pausa breve y pasa sola a la siguiente.
@Composable
fun QuestionScreen(vm: DonesViewModel, q: ParticipantView.Question) {
    // Respuesta recién tocada: se ve marcada un momento antes de avanzar.
    var pending by remember(q.item.n) { mutableStateOf<Int?>(null) }
    LaunchedEffect(pending) {
        val value = pending ?: return@LaunchedEffect
        delay(350)
        vm.answer(q.item.n, value)
    }
    val selected = pending ?: q.currentAnswer
    Screen(
        top = {
            ProgressHeader(
                label = "Bloque ${q.block} de ${q.totalBlocks} · ${q.positionInBlock} de ${q.blockSize}",
                fraction = q.positionInBlock / q.blockSize.toFloat(),
            )
        },
        bottom = {
            val prev = q.previousItem
            if (prev != null) BackLink("‹ Anterior", onClick = { vm.goBackTo(prev) })
            else BackLink("‹ Anterior", onClick = vm::previousPage)
        },
    ) {
        Text("“${q.item.text}”", style = MaterialTheme.typography.headlineSmall)
        Body(vm.content.prompt)
        vm.content.scale.forEach { option ->
            ChoiceButton(
                text = option.label,
                selected = selected == option.value,
                onClick = { if (pending == null) pending = option.value },
            )
        }
    }
}

// W10
@Composable
fun BlockDoneScreen(vm: DonesViewModel, v: ParticipantView.BlockDone) {
    val finished = v.answered == v.total
    Screen(
        bottom = {
            PrimaryButton(if (finished) "Ver mis resultados ›" else "Continuar ›", onClick = vm::nextPage)
            BackLink("‹ Revisar mi última respuesta", onClick = { vm.backFromBlockDone(v.block) })
        },
    ) {
        CenteredMessage("¡Bloque ${v.block} terminado! ✓", "Llevas ${v.answered} de ${v.total}.")
    }
}
