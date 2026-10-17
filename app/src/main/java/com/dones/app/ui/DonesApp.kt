package com.dones.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dones.app.DonesViewModel
import com.dones.app.Onboarding
import com.dones.app.Overlay
import com.dones.app.qr.QrScannerScreen
import com.dones.core.ClassStep
import com.dones.core.ParticipantView

/** Decide qué pantalla mostrar. No hay menú: la clase (el maestro) marca el ritmo. */
@Composable
fun DonesApp(vm: DonesViewModel) {
    val onboarding = vm.onboarding
    if (onboarding != null) {
        BackHandler(enabled = onboarding !in setOf(Onboarding.JoinCode, Onboarding.Connected)) { vm.onboardingBack() }
        when (onboarding) {
            Onboarding.JoinCode -> JoinCodeScreen(vm)
            Onboarding.Scanner -> QrScannerScreen(onResult = vm::onQrScanned, onBack = vm::onboardingBack)
            Onboarding.Connected -> ConnectedScreen(vm)
            Onboarding.Purpose -> PurposeScreen(vm)
            Onboarding.Name -> NameScreen(vm)
            Onboarding.Age -> AgeScreen(vm)
            Onboarding.Marital -> MaritalScreen(vm)
            Onboarding.WhatsApp -> WhatsappScreen(vm)
            Onboarding.Review -> ReviewScreen(vm)
            Onboarding.Consent -> ConsentScreen(vm)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            val overlay = vm.overlay
            if (overlay != null) {
                BackHandler { vm.closeOverlay() }
                when (overlay) {
                    Overlay.Curve -> CurveScreen(vm)
                    is Overlay.GiftDetail -> GiftDetailScreen(vm, overlay.code)
                    Overlay.Ministries -> MinistriesScreen(vm)
                    Overlay.Documents -> DocumentsScreen(vm)
                }
            } else {
                when (val v = vm.view) {
                    null, ParticipantView.Waiting -> WaitingScreen()
                    is ParticipantView.Question -> QuestionScreen(vm, v)
                    is ParticipantView.BlockDone -> BlockDoneScreen(vm, v)
                    is ParticipantView.Results -> ResultsScreen(vm)
                }
            }
        }
        DemoTeacherBar(vm)
    }
}

/** Solo en el demo: reemplaza a la PC del maestro mientras no exista la consola web. */
@Composable
private fun DemoTeacherBar(vm: DonesViewModel) {
    val state = vm.classState ?: return
    val label = when (val s = state.current) {
        ClassStep.Waiting -> "esperando inicio"
        is ClassStep.HoutsBlock -> "bloque ${s.block} abierto"
        ClassStep.Results -> "resultados abiertos"
        ClassStep.Closed -> "curso cerrado"
    }
    Column(Modifier.navigationBarsPadding()) {
        SimulatedTeacherBar(
            stepLabel = "paso ${state.stepIndex + 1} de ${state.steps.size}, $label",
            onPrevious = vm::teacherPrevious,
            onNext = vm::teacherNext,
            canPrevious = !state.isFirst,
            canNext = !state.isLast,
        )
        if (state.isLast) {
            BackLink("Reiniciar demo (borra todo)", onClick = vm::resetDemo)
        }
    }
}
