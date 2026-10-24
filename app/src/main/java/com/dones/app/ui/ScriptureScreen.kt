package com.dones.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dones.app.DonesViewModel
import com.dones.app.ScriptureState
import com.dones.core.Bible

// Pantalla para leer una cita en la Reina-Valera 1960. "Volver" regresa a la pantalla anterior.
@Composable
fun ScriptureScreen(vm: DonesViewModel, ref: String) {
    LaunchedEffect(ref) { vm.loadScripture(ref) }
    val context = LocalContext.current
    val parsed = Bible.parse(ref)
    // Mientras llega la cita nueva no se muestra la anterior.
    val state = vm.scripture.let {
        if ((it is ScriptureState.Loaded && it.ref != parsed) || (it is ScriptureState.Failed && it.ref != parsed)) ScriptureState.Loading else it
    }
    Screen(
        bottom = {
            if (state is ScriptureState.Failed && state.ref != null) {
                SecondaryButton("Abrir en la Biblia de bible.com", icon = ErgosIcons.Book, onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.ref.bibleComUrl)))
                    }
                })
            }
            BackLink("Volver", onClick = vm::closeOverlay)
        },
    ) {
        PageHeader(title = parsed?.title ?: ref, icon = ErgosIcons.Book, eyebrow = "Reina-Valera 1960")
        when (state) {
            ScriptureState.Loading -> Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator(color = Ergos.Gold, modifier = Modifier.size(44.dp))
                    Hint("Buscando el pasaje…")
                }
            }
            is ScriptureState.Loaded -> {
                ErgosCard(ornament = true, padding = 22.dp) {
                    // Si la cita tiene partes separadas ("12:10, 28"), se marca el salto con "…".
                    var previous: Int? = null
                    state.verses.forEach { v ->
                        if (previous != null && v.number != previous!! + 1) {
                            Text("…", style = MaterialTheme.typography.bodyLarge, color = Ergos.Muted)
                        }
                        previous = v.number
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = Ergos.Gold, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)) {
                                    append("${v.number}  ")
                                }
                                append(v.text)
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp, lineHeight = 35.sp),
                            color = Ergos.Ink,
                        )
                    }
                }
                Hint(Bible.RVR1960_NOTICE)
            }
            is ScriptureState.Failed -> ErgosCard(border = Ergos.Gold.copy(alpha = .6f)) {
                Eyebrow("No se pudo cargar", icon = ErgosIcons.Book, color = Ergos.Gold)
                Body(
                    if (state.ref == null) "No reconocemos esta cita."
                    else "Para leerlo aquí la primera vez se necesita internet. Después queda guardado en tu teléfono. " +
                        "También puedes abrirlo en bible.com con el botón de abajo.",
                )
                if (state.ref != null) {
                    SecondaryButton("Intentar de nuevo", icon = ErgosIcons.Restart, onClick = { vm.loadScripture(ref) })
                }
            }
        }
    }
}
