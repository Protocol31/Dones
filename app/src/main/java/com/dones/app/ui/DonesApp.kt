package com.dones.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
        val canGoBack = when (onboarding) {
            Onboarding.Profiles, Onboarding.Connected -> false
            Onboarding.JoinCode -> vm.canGoToProfiles
            else -> true
        }
        BackHandler(enabled = canGoBack) { vm.onboardingBack() }
        when (onboarding) {
            Onboarding.Profiles -> ProfilesScreen(vm)
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

    // La barra de estado se cubre aquí una sola vez (las pantallas no la vuelven a sumar).
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        val overlay = vm.overlay
        if (overlay != null) {
            BackHandler { vm.closeOverlay() }
            when (overlay) {
                Overlay.Home -> HomeScreen(vm)
                Overlay.Curve -> CurveScreen(vm)
                is Overlay.GiftDetail -> GiftDetailScreen(vm, overlay.code)
                Overlay.Ministries -> MinistriesScreen(vm)
                Overlay.Documents -> DocumentsScreen(vm)
            }
        } else {
            ClassTopBar(vm)
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
}
