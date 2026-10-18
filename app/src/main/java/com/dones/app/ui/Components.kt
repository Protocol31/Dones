package com.dones.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors

/** Altura mínima de los botones: 64 dp, más que el mínimo de Android (48 dp). */
val BigTouch = 64.dp

/** Resplandor violeta suave arriba, como en Ergos. */
private val NightGlow = Brush.radialGradient(
    colors = listOf(Ergos.Violet.copy(alpha = 0.22f), Color.Transparent),
    center = androidx.compose.ui.geometry.Offset(0f, 0f),
    radius = 1100f,
)

private val ButtonShape = RoundedCornerShape(14.dp)

/** Esqueleto común: una sola tarea por pantalla, con desplazamiento si la letra es muy grande. */
@Composable
fun Screen(
    modifier: Modifier = Modifier,
    top: (@Composable () -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .background(NightGlow)
                .safeDrawingPadding(),
        ) {
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
    Text(text, style = MaterialTheme.typography.labelMedium, color = Ergos.Muted)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val fill = if (enabled) Brush.verticalGradient(listOf(Ergos.ButtonTop, Ergos.ButtonBottom))
    else Brush.verticalGradient(listOf(Ergos.Surface2, Ergos.Surface2))
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Ergos.Muted,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = BigTouch),
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = BigTouch).clip(ButtonShape).background(fill).padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = ButtonShape,
        border = BorderStroke(1.dp, Ergos.LineStrong),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Ergos.Surface.copy(alpha = 0.6f), contentColor = Ergos.Ink),
        modifier = Modifier.fillMaxWidth().heightIn(min = BigTouch),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
}

/** Opción de respuesta: ancho completo, y la elegida se marca con ✓ además del color. */
@Composable
fun ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val isSelected = selected
    val colors: ButtonColors = if (isSelected) {
        ButtonDefaults.buttonColors(containerColor = Ergos.VioletDeep, contentColor = Ergos.Ink)
    } else {
        ButtonDefaults.outlinedButtonColors(containerColor = Ergos.Surface.copy(alpha = 0.6f), contentColor = Ergos.Ink)
    }
    val modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = BigTouch)
        .semantics {
            this.selected = isSelected
            role = Role.RadioButton
        }
    if (selected) {
        OutlinedButton(
            onClick = onClick, modifier = modifier, colors = colors, shape = ButtonShape,
            border = BorderStroke(2.dp, Ergos.Gold),
        ) { ChoiceLabel("✓  $text") }
    } else {
        OutlinedButton(
            onClick = onClick, modifier = modifier, colors = colors, shape = ButtonShape,
            border = BorderStroke(1.dp, Ergos.LineStrong),
        ) { ChoiceLabel(text) }
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
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = Ergos.AccentText),
        modifier = Modifier.heightIn(min = 56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Progreso con palabras y barra: "Bloque 2 de 5 · 14 de 25". */
@Composable
fun ProgressHeader(label: String, fraction: Float) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Ergos.Gold)
        Spacer(Modifier.padding(top = 8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            color = Ergos.Gold,
            trackColor = Ergos.Line,
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
        )
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
