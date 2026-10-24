package com.dones.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dones.app.DonesViewModel
import com.dones.app.Onboarding
import com.dones.core.JoinCode

// W1: primera pantalla de la app.
@Composable
fun JoinCodeScreen(vm: DonesViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    Screen(
        bottom = {
            PrimaryButton("Entrar", trailing = ErgosIcons.ChevronRight, onClick = { vm.submitCode(code) }, enabled = code.length == 6)
            SecondaryButton("Leer el código QR con la cámara", icon = ErgosIcons.Qr, onClick = vm::openScanner)
            if (vm.canGoToProfiles) BackLink("Volver", onClick = vm::switchProfile)
        },
    ) {
        PageHeader(
            title = "Entra a tu clase",
            icon = ErgosIcons.Key,
            eyebrow = "Llamados",
            subtitle = "Escribe el código de 6 números que ves en la pantalla.",
        )
        OutlinedTextField(
            value = code,
            onValueChange = { new ->
                code = new.filter { it.isDigit() }.take(6)
                if (code.length == 6) vm.submitCode(code) // con el 6.º número entra solo
            },
            colors = ergosFieldColors(null),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontSize = 40.sp, letterSpacing = 12.sp, textAlign = TextAlign.Center),
            placeholder = { Text("______", fontSize = 40.sp, letterSpacing = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { vm.submitCode(code) }),
        )
        vm.joinError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge) }
        Hint("Demo: el código de prueba es ${JoinCode.DEMO_CODE}.")
    }
}

// W2: conectado.
@Composable
fun ConnectedScreen(vm: DonesViewModel) {
    val info = vm.classState?.info
    Screen(bottom = { PrimaryButton("Continuar", trailing = ErgosIcons.ChevronRight, onClick = { vm.goTo(Onboarding.Purpose) }) }) {
        PageHeader(title = "¡Estás en la clase!", icon = ErgosIcons.Check, eyebrow = "Conectado")
        info?.let {
            ErgosCard(ornament = true) {
                Eyebrow("Curso", icon = ErgosIcons.Book)
                Text(it.courseName, style = MaterialTheme.typography.titleLarge)
                Eyebrow("Iglesia", icon = ErgosIcons.Church)
                Body(it.churchName)
            }
        }
        Body("Antes de comenzar necesitamos 4 datos.", center = true, modifier = Modifier.fillMaxWidth())
    }
}

// W2b: para qué son los datos.
@Composable
fun PurposeScreen(vm: DonesViewModel) {
    Screen(
        bottom = {
            PrimaryButton("Entendido", onClick = { vm.goTo(Onboarding.Name) })
            BackLink(onClick = vm::onboardingBack)
        },
    ) {
        PageHeader(title = "Para qué te pedimos datos", icon = ErgosIcons.Shield)
        PurposeRow(ErgosIcons.Person, "Tu nombre", "Va en tu certificado.")
        PurposeRow(ErgosIcons.Chat, "Tu WhatsApp", "Para enviarte tus resultados y tu certificado.")
        PurposeRow(ErgosIcons.Rings, "Tu estado civil", "Para no mostrarte preguntas que no son para ti.")
        PurposeRow(ErgosIcons.Calendar, "Tu edad", "Para conocer mejor a los grupos del curso. No aparece en tu certificado.")
    }
}

// W2c: dato 1 de 4.
@Composable
fun NameScreen(vm: DonesViewModel) {
    var name by rememberSaveable { mutableStateOf(vm.participant.name) }
    val valid = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size >= 2
    Screen(
        bottom = {
            PrimaryButton("Continuar", trailing = ErgosIcons.ChevronRight, onClick = { vm.setName(name) }, enabled = valid)
            BackLink(onClick = vm::onboardingBack)
        },
    ) {
        StepHeader(1, ErgosIcons.Person, "¿Cuál es tu nombre completo?", "Así aparecerá en tu certificado.")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            colors = ergosFieldColors(null),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (valid) vm.setName(name) }),
        )
        if (name.isNotBlank() && !valid) Hint("Escribe tu nombre y al menos un apellido.")
    }
}

// W2c: dato 2 de 4.
@Composable
fun AgeScreen(vm: DonesViewModel) {
    Screen(bottom = { BackLink(onClick = vm::onboardingBack) }) {
        StepHeader(2, ErgosIcons.Calendar, "¿Cuál es tu edad?")
        vm.content.ageRanges.forEach { range ->
            ChoiceButton(range, selected = vm.participant.ageRange == range, onClick = { vm.setAge(range) })
        }
    }
}

// W2c: dato 3 de 4.
@Composable
fun MaritalScreen(vm: DonesViewModel) {
    Screen(bottom = { BackLink(onClick = vm::onboardingBack) }) {
        StepHeader(3, ErgosIcons.Rings, "¿Cuál es tu estado civil?", "Algunas preguntas solo aplican a personas sin pareja.")
        vm.content.maritalStatus.forEach { option ->
            ChoiceButton(option.label, selected = vm.participant.maritalStatus == option.id, onClick = { vm.setMarital(option.id) })
        }
    }
}

// W2c: dato 4 de 4.
@Composable
fun WhatsappScreen(vm: DonesViewModel) {
    var country by rememberSaveable { mutableStateOf("+56") }
    var number by rememberSaveable { mutableStateOf("") }
    val valid = number.count { it.isDigit() } in 8..12 && country.count { it.isDigit() } in 1..3
    Screen(
        bottom = {
            PrimaryButton("Continuar", trailing = ErgosIcons.ChevronRight, onClick = { vm.setWhatsapp(country, number) }, enabled = valid)
            SecondaryButton("Prefiero no darlo", onClick = vm::declineWhatsapp)
            BackLink(onClick = vm::onboardingBack)
        },
    ) {
        StepHeader(4, ErgosIcons.Chat, "¿Cuál es tu WhatsApp?", "Ahí te llegarán tus resultados y tu certificado.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = country,
                onValueChange = { v -> country = "+" + v.filter { it.isDigit() }.take(3) },
                colors = ergosFieldColors(null),
                modifier = Modifier.width(110.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            OutlinedTextField(
                value = number,
                onValueChange = { v -> number = v.filter { it.isDigit() || it == ' ' }.take(15) },
                colors = ergosFieldColors(null),
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("9 1234 5678") },
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) vm.setWhatsapp(country, number) }),
            )
        }
    }
}

// Resumen para corregir antes del certificado.
@Composable
fun ReviewScreen(vm: DonesViewModel) {
    val p = vm.participant
    val marital = vm.content.maritalStatus.firstOrNull { it.id == p.maritalStatus }?.label ?: "—"
    Screen(
        bottom = {
            PrimaryButton("Sí, están bien", icon = ErgosIcons.Check, onClick = { vm.goTo(Onboarding.Consent) })
            BackLink(onClick = vm::onboardingBack)
        },
    ) {
        PageHeader(title = "¿Están bien tus datos?", icon = ErgosIcons.Document)
        ErgosCard {
            ReviewRow(ErgosIcons.Person, "Nombre", p.name) { vm.goTo(Onboarding.Name) }
            ReviewRow(ErgosIcons.Calendar, "Edad", p.ageRange ?: "—") { vm.goTo(Onboarding.Age) }
            ReviewRow(ErgosIcons.Rings, "Estado civil", marital) { vm.goTo(Onboarding.Marital) }
            ReviewRow(ErgosIcons.Chat, "WhatsApp", if (p.whatsappDeclined) "No lo diste" else p.whatsapp) { vm.goTo(Onboarding.WhatsApp) }
        }
    }
}

@Composable
private fun ReviewRow(icon: ImageVector, label: String, value: String, onEdit: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Hint(label)
            Body(value)
        }
        BackLink(text = "Cambiar", icon = ErgosIcons.Pencil, onClick = onEdit)
    }
}

/** Fila con ícono: para qué sirve cada dato. */
@Composable
private fun PurposeRow(icon: ImageVector, title: String, text: String) {
    ErgosCard(padding = 16.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = Ergos.AccentText, modifier = Modifier.size(30.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Hint(text)
            }
        }
    }
}

/** Encabezado de los 4 datos: puntitos de avance, ícono y pregunta. */
@Composable
private fun StepHeader(step: Int, icon: ImageVector, title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepDots(step, 4)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconFrame(icon, size = 64.dp)
            Title(title, modifier = Modifier.weight(1f))
        }
        if (subtitle != null) Body(subtitle, color = Ergos.Ink2)
    }
}

// W3: consentimiento.
@Composable
fun ConsentScreen(vm: DonesViewModel) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    Screen(
        bottom = {
            PrimaryButton("Comenzar", icon = ErgosIcons.Sparkle, onClick = vm::acceptConsent, enabled = accepted)
            BackLink(onClick = vm::onboardingBack)
        },
    ) {
        PageHeader(title = "Cuidamos tus datos", icon = ErgosIcons.Shield)
        Body("Tus respuestas tratan sobre tu fe. Solo las usamos para darte tus resultados y tu certificado.")
        Body("En este demo todo queda guardado solo en tu teléfono.", color = Ergos.Ink2)
        ChoiceButton("Acepto", selected = accepted, onClick = { accepted = !accepted })
        if (!accepted) Hint("Sin aceptar no podemos guardar ni enviarte tus resultados.")
    }
}
