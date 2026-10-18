package com.dones.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dones.app.DonesViewModel
import com.dones.core.Blanks
import com.dones.core.LessonGate
import com.dones.core.ParticipantView
import kotlinx.coroutines.delay

/** Ícono de cada parte del curso, para reconocerla de un vistazo. */
fun sectionIcon(section: String): ImageVector = when {
    section.startsWith("I.") -> ErgosIcons.Church
    section.startsWith("II.") -> ErgosIcons.Book
    section.startsWith("III.") -> ErgosIcons.Cross
    section.startsWith("IV.") -> ErgosIcons.Sparkle
    section.startsWith("V.") -> ErgosIcons.Seal
    section.startsWith("C ") -> ErgosIcons.Heart
    section.startsWith("O ") -> ErgosIcons.Sparkle
    section.startsWith("R ") -> ErgosIcons.Person
    section.startsWith("E ") -> ErgosIcons.Calendar
    section.startsWith("T ") -> ErgosIcons.Key
    section == "Cuestionario" -> ErgosIcons.Bars
    else -> ErgosIcons.People
}

// W6/W7: una página de la enseñanza. Texto, completar espacios o pregunta abierta; la persona avanza sola.
@Composable
fun LessonScreen(vm: DonesViewModel, v: ParticipantView.LessonStep) {
    val page = v.page
    val key = v.index
    val fields = if (page.write) 1 else page.blanks.size
    val values = remember(key) {
        mutableStateListOf(*Array(fields) { i -> vm.participant.notes["$key.$i"].orEmpty() })
    }
    val selected = remember(key) {
        mutableStateListOf(*vm.participant.notes["$key.0"].orEmpty().lines().filter { it in page.choices }.toTypedArray())
    }
    // Una página de espacios ya revisada se muestra con su corrección al volver a ella.
    var checked by remember(key) { mutableStateOf(page.blanks.isNotEmpty() && "$key.0" in vm.participant.notes) }
    val mustCheck = page.blanks.isNotEmpty() && !checked
    // No se avanza sin responder.
    val answered = LessonGate.canContinue(page, values, selected)
    Screen(
        top = {
            ProgressHeader(
                label = page.section,
                icon = sectionIcon(page.section),
                count = "${v.positionInSection} de ${v.sectionSize}",
                fraction = v.positionInSection / v.sectionSize.toFloat(),
            )
        },
        bottom = {
            if (!answered) LessonGate.hint(page)?.let { Hint(it) }
            if (mustCheck) {
                PrimaryButton(
                    "Revisar mis respuestas", icon = ErgosIcons.Check, enabled = answered,
                    onClick = { vm.saveNotes(key, values.toList()); checked = true },
                )
            } else {
                PrimaryButton("Siguiente", trailing = ErgosIcons.ChevronRight, enabled = answered, onClick = {
                    if (page.write || page.blanks.isNotEmpty()) vm.saveNotes(key, values.toList())
                    if (page.choices.isNotEmpty()) vm.saveNotes(key, listOf(selected.joinToString("\n")))
                    vm.nextPage()
                })
            }
            if (key > 0) BackLink("Anterior", onClick = vm::previousPage)
        },
    ) {
        page.title?.let {
            Title(it)
            Ornament(width = 160.dp)
        }
        page.ref?.let { RefChip(it) }
        Text(
            lessonText(page.text, page.blanks, values, checked),
            style = MaterialTheme.typography.bodyLarge,
            color = Ergos.Ink,
        )

        if (page.blanks.isNotEmpty()) {
            ErgosCard(ornament = true) {
                Eyebrow(if (fields > 1) "Completa los espacios" else "Completa el espacio", icon = ErgosIcons.Pencil)
                page.blanks.forEachIndexed { i, expected ->
                    BlankField(
                        value = values[i],
                        onValueChange = { values[i] = it },
                        label = if (fields > 1) "Espacio ${i + 1}" else "Tu respuesta",
                        expected = expected,
                        checked = checked,
                    )
                }
                if (mustCheck) {
                    Hint("Escribe lo que crees y toca «Revisar mis respuestas».")
                } else {
                    val right = page.blanks.indices.count { Blanks.isCorrect(page.blanks[it], values[it]) }
                    Hint(
                        if (right == fields) "¡Muy bien! Puedes seguir."
                        else "Acertaste $right de $fields. Puedes corregir lo que escribiste y seguir.",
                    )
                }
            }
        }

        if (page.choices.isNotEmpty()) {
            Eyebrow(
                when (page.pick) {
                    1 -> "Elige una"
                    0 -> "Elige las que quieras"
                    else -> "Elige ${page.pick}"
                },
                icon = ErgosIcons.Choose,
            )
            page.choices.forEach { choice ->
                ChoiceButton(choice, selected = choice in selected, onClick = {
                    when {
                        choice in selected -> selected.remove(choice)
                        page.pick == 1 -> { selected.clear(); selected.add(choice) }
                        page.pick == 0 || selected.size < page.pick -> selected.add(choice)
                    }
                })
            }
            if (page.pick > 1 && selected.size == page.pick) Hint("Ya elegiste ${page.pick}. Toca uno para quitarlo.")
        }

        if (page.write) {
            ErgosCard {
                Eyebrow("Escribe tu respuesta", icon = ErgosIcons.Pencil)
                OutlinedTextField(
                    value = values[0],
                    onValueChange = { values[0] = it },
                    minLines = 4,
                    placeholder = { Text("Escribe aquí", style = MaterialTheme.typography.bodyLarge) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = ergosFieldColors(null),
                    modifier = Modifier.fillMaxWidth(),
                )
                Hint("Solo tú lo ves. Queda guardado en este teléfono.")
            }
        }
    }
}

/**
 * Un espacio para completar. Después de revisar sigue siendo editable:
 * borde y respuesta en verde si acertó, en rojo si no (y cambia en vivo si lo corrige).
 */
@Composable
private fun BlankField(value: String, onValueChange: (String) -> Unit, label: String, expected: String, checked: Boolean) {
    val ok = Blanks.isCorrect(expected, value)
    val state: Boolean? = if (checked) ok else null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label) },
        textStyle = MaterialTheme.typography.bodyLarge,
        trailingIcon = if (state != null) ({ StatusDot(state) }) else null,
        colors = ergosFieldColors(state),
        modifier = Modifier.fillMaxWidth(),
    )
    if (state != null) {
        val color = if (state) Ergos.Positive else Ergos.Negative
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (state) ErgosIcons.Check else ErgosIcons.Close,
                contentDescription = null, tint = color, modifier = Modifier.size(22.dp),
            )
            Text(
                buildAnnotatedString {
                    append(if (state) "¡Correcto! " else "Respuesta: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(Blanks.display(expected)) }
                },
                color = color,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/** Colores del campo: violeta normal, verde si acertó, rojo si no. */
@Composable
internal fun ergosFieldColors(correct: Boolean?) = when (correct) {
    null -> OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Ergos.VioletHi,
        unfocusedBorderColor = Ergos.LineStrong,
        focusedLabelColor = Ergos.AccentText,
        unfocusedLabelColor = Ergos.Muted,
        cursorColor = Ergos.VioletHi,
        focusedContainerColor = Ergos.Night.copy(alpha = .5f),
        unfocusedContainerColor = Ergos.Night.copy(alpha = .5f),
    )
    else -> {
        val c = if (correct) Ergos.Positive else Ergos.Negative
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c,
            unfocusedBorderColor = c,
            focusedLabelColor = c,
            unfocusedLabelColor = c,
            cursorColor = c,
            focusedContainerColor = c.copy(alpha = .08f),
            unfocusedContainerColor = c.copy(alpha = .08f),
        )
    }
}

/**
 * El texto de la página con sus espacios: "(1) ______" mientras están vacíos y,
 * cuando la persona escribe, su respuesta subrayada en el mismo lugar
 * (en verde o rojo después de revisar).
 */
private fun lessonText(text: String, blanks: List<String>, values: List<String>, checked: Boolean): AnnotatedString {
    val parts = text.split(Blanks.MARK)
    val numbered = blanks.size > 1
    return buildAnnotatedString {
        parts.forEachIndexed { i, part ->
            append(part)
            if (i == parts.lastIndex) return@forEachIndexed
            val typed = values.getOrNull(i)?.trim().orEmpty()
            if (typed.isEmpty() || i >= blanks.size) {
                withStyle(SpanStyle(color = Ergos.Muted)) { append(if (numbered) "(${i + 1}) ______" else "______") }
            } else {
                val color: Color = when {
                    !checked -> Ergos.AccentText
                    Blanks.isCorrect(blanks[i], typed) -> Ergos.Positive
                    else -> Ergos.Negative
                }
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                    append(typed)
                }
            }
        }
    }
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
                label = "Cuestionario · bloque ${q.block} de ${q.totalBlocks}",
                icon = ErgosIcons.Bars,
                count = "${q.positionInBlock} de ${q.blockSize}",
                fraction = q.positionInBlock / q.blockSize.toFloat(),
            )
        },
        bottom = {
            val prev = q.previousItem
            if (prev != null) BackLink("Anterior", onClick = { vm.goBackTo(prev) })
            else BackLink("Anterior", onClick = vm::previousPage)
        },
    ) {
        ErgosCard(ornament = true, background = Ergos.Surface2, border = Ergos.Violet.copy(alpha = .45f), padding = 22.dp) {
            Eyebrow("Afirmación ${q.item.n}", icon = ErgosIcons.Sparkle)
            Text("“${q.item.text}”", style = MaterialTheme.typography.headlineSmall, color = Ergos.Ink)
        }
        Body(vm.content.prompt, color = Ergos.Ink2)
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
    val finished = v.isLastBlock
    Screen(
        bottom = {
            PrimaryButton(
                if (finished) "Ver mis resultados" else "Continuar",
                icon = if (finished) ErgosIcons.Sparkle else null,
                trailing = ErgosIcons.ChevronRight,
                onClick = vm::nextPage,
            )
            BackLink("Revisar mi última respuesta", onClick = { vm.backFromBlockDone(v.block) })
        },
    ) {
        CenteredMessage(
            title = "¡Bloque ${v.block} terminado!",
            body = if (finished) "Respondiste las ${v.total} afirmaciones. Tus dones están listos."
            else "Llevas ${v.answered} de ${v.total} afirmaciones. La lección continúa.",
            icon = if (finished) ErgosIcons.Sparkle else ErgosIcons.Check,
        )
    }
}
