package com.dones.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dones.app.DonesViewModel
import com.dones.app.Onboarding
import com.dones.app.Overlay
import com.dones.app.qr.QrScannerScreen
import com.dones.core.ParticipantView

/** Decide qué pantalla mostrar. No hay menú: cada persona avanza página por página. */
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
                null -> JoinCodeScreen(vm)
                is ParticipantView.LessonStep -> LessonScreen(vm, v)
                is ParticipantView.Question -> QuestionScreen(vm, v)
                is ParticipantView.BlockDone -> BlockDoneScreen(vm, v)
                ParticipantView.Results -> ResultsScreen(vm)
            }
        }
    }
}
