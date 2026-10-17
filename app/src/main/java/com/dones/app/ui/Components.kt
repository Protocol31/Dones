package com.dones.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Altura mínima de los botones: 64 dp, más que el mínimo de Android (48 dp). */
val BigTouch = 64.dp

/** Esqueleto común: una sola tarea por pantalla, con desplazamiento si la letra es muy grande. */
@Composable
fun Screen(
    modifier: Modifier = Modifier,
    top: (@Composable () -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            top?.invoke()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
            if (bottom != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = bottom,
                )
            }
        }
    }
}

@Composable
fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium)
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
    )
}

@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = BigTouch),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = BigTouch),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
}

/** Opción de respuesta: ancho completo, y la elegida se marca con ✓ además del color. */
@Composable
fun ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val isSelected = selected
    val colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
    val modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = BigTouch)
        .semantics {
            this.selected = isSelected
            role = Role.RadioButton
        }
    if (selected) {
        Button(onClick = onClick, modifier = modifier, colors = colors) { ChoiceLabel("✓  $text") }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, colors = colors) { ChoiceLabel(text) }
    }
}

@Composable
private fun ChoiceLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start,
    )
}

@Composable
fun BackLink(text: String = "‹ Volver", onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 56.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Progreso con palabras y barra: "Bloque 2 de 5 · 14 de 25". */
@Composable
fun ProgressHeader(label: String, fraction: Float) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.padding(top = 6.dp))
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun CenteredMessage(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Body(body, center = true)
    }
}

/** Barra del "maestro simulado": solo existe en el demo, para avanzar la clase sin la PC del maestro. */
@Composable
fun SimulatedTeacherBar(stepLabel: String, onPrevious: () -> Unit, onNext: () -> Unit, canPrevious: Boolean, canNext: Boolean) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("DEMO · Maestro simulado: $stepLabel", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onPrevious, enabled = canPrevious, modifier = Modifier.weight(1f)) { Text("◄ Anterior") }
                Button(onClick = onNext, enabled = canNext, modifier = Modifier.weight(1f)) { Text("Siguiente ►") }
            }
        }
    }
}
