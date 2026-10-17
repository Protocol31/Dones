package com.dones.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dones.app.DonesViewModel
import com.dones.core.ParticipantView
import kotlinx.coroutines.delay

// W5
@Composable
fun WaitingScreen() {
    Screen {
        CenteredMessage("Ya estás en la clase ✓", "Espera a que el maestro comience.")
        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
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
                label = "Bloque ${q.block} de ${q.totalBlocks} · ${q.positionInBlock} de ${q.blockSize}",
                fraction = q.positionInBlock / q.blockSize.toFloat(),
            )
        },
        bottom = { q.previousItem?.let { prev -> BackLink("‹ Anterior", onClick = { vm.goBackTo(prev) }) } },
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
    Screen(bottom = { BackLink("‹ Revisar mi última respuesta", onClick = vm::backFromBlockDone) }) {
        CenteredMessage(
            "¡Bloque ${v.block} terminado! ✓",
            "Llevas ${v.answered} de ${v.total}.\n\nEspera al maestro para continuar.",
        )
    }
}
