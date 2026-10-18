package com.dones.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
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
            PrimaryButton("Entrar", onClick = { vm.submitCode(code) }, enabled = code.length == 6)
            SecondaryButton("📷  Usar la cámara para leer el código QR", onClick = vm::openScanner)
            if (vm.canGoToProfiles) BackLink("‹ Volver", onClick = vm::switchProfile)
        },
    ) {
        Title("Escribe el código que ves en la pantalla")
        OutlinedTextField(
            value = code,
            onValueChange = { new ->
                code = new.filter { it.isDigit() }.take(6)
                if (code.length == 6) vm.submitCode(code) // con el 6.º número entra solo
            },
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
    Screen(bottom = { PrimaryButton("Continuar", onClick = { vm.goTo(Onboarding.Purpose) }) }) {
        Title("Conectado a la clase ✓")
        info?.let {
            Body(it.courseName)
            Body(it.churchName)
        }
        Body("Antes de comenzar necesitamos 4 datos.")
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
        Title("Para qué te pedimos datos")
        Body("• Tu nombre va en tu certificado.")
        Body("• Tu WhatsApp, para enviarte tus resultados y tu certificado.")
        Body("• Tu estado civil, para no mostrarte preguntas que no son para ti.")
        Body("• Tu edad, para conocer mejor a los grupos del curso. No aparece en tu certificado.")
    }
}

// W2c: dato 1 de 4.
@Composable
fun NameScreen(vm: DonesViewModel) {
    var name by rememberSaveable { mutableStateOf(vm.participant.name) }
    val valid = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size >= 2
    Screen(
        bottom = {
            PrimaryButton("Continuar", onClick = { vm.setName(name) }, enabled = valid)
            BackLink(onClick = vm::onboardingBack)
        },
    ) {
        Hint("Dato 1 de 4")
        Title("¿Cuál es tu nombre completo?")
        Body("Así aparecerá en tu certificado.")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
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
        Hint("Dato 2 de 4")
        Title("¿Cuál es tu edad?")
        vm.content.ageRanges.forEach { range ->
            ChoiceButton(range, selected = vm.participant.ageRange == range, onClick = { vm.setAge(range) })
        }
    }
}

// W2c: dato 3 de 4.
@Composable
fun MaritalScreen(vm: DonesViewModel) {
    Screen(bottom = { BackLink(onClick = vm::onboardingBack) }) {
        Hint("Dato 3 de 4")
        Title("¿Cuál es tu estado civil?")
        Body("Algunas preguntas solo aplican a personas sin pareja.")
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
            PrimaryButton("Continuar", onClick = { vm.setWhatsapp(country, number) }, enabled = valid)
            SecondaryButton("Prefiero no darlo", onClick = vm::declineWhatsapp)
            BackLink(onClick = vm::onboardingBack)
        },
    ) {
        Hint("Dato 4 de 4")
        Title("¿Cuál es tu WhatsApp?")
        Body("Ahí te llegarán tus resultados y tu certificado.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = country,
                onValueChange = { v -> country = "+" + v.filter { it.isDigit() }.take(3) },
                modifier = Modifier.width(110.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            OutlinedTextField(
                value = number,
                onValueChange = { v -> number = v.filter { it.isDigit() || it == ' ' }.take(15) },
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
            PrimaryButton("Sí, están bien", onClick = { vm.goTo(Onboarding.Consent) })
            BackLink(onClick = vm::onboardingBack)
        },
    ) {
        Title("¿Están bien tus datos?")
        ReviewRow("Nombre", p.name) { vm.goTo(Onboarding.Name) }
        ReviewRow("Edad", p.ageRange ?: "—") { vm.goTo(Onboarding.Age) }
        ReviewRow("Estado civil", marital) { vm.goTo(Onboarding.Marital) }
        ReviewRow("WhatsApp", if (p.whatsappDeclined) "No lo diste" else p.whatsapp) { vm.goTo(Onboarding.WhatsApp) }
    }
}

@Composable
private fun ReviewRow(label: String, value: String, onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Hint(label)
            Body(value)
        }
        BackLink(text = "Cambiar", onClick = onEdit)
    }
}

// W3: consentimiento.
@Composable
fun ConsentScreen(vm: DonesViewModel) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    Screen(
        bottom = {
            PrimaryButton("Comenzar", onClick = vm::acceptConsent, enabled = accepted)
            BackLink(onClick = vm::onboardingBack)
        },
    ) {
        Title("Cuidamos tus datos")
        Body("Tus respuestas tratan sobre tu fe. Solo las usamos para darte tus resultados y tu certificado.")
        Body("En este demo todo queda guardado solo en tu teléfono.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = accepted, onCheckedChange = { accepted = it })
            Body("Acepto")
        }
        if (!accepted) Hint("Sin aceptar no podemos guardar ni enviarte tus resultados.")
    }
}
