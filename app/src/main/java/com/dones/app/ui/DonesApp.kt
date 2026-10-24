package com.dones.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.dones.app.DonesViewModel
import com.dones.app.MainTab
import com.dones.app.Onboarding
import com.dones.app.Overlay
import com.dones.core.ParticipantView

/** Decide qué pantalla mostrar. No hay menú: cada persona avanza página por página. */
@Composable
fun DonesApp(vm: DonesViewModel) {
    val onboarding = vm.onboarding
    if (onboarding != null) {
        val canGoBack = when (onboarding) {
            Onboarding.Profiles -> false
            Onboarding.Purpose -> vm.canGoToProfiles
            else -> true
        }
        BackHandler(enabled = canGoBack) { vm.onboardingBack() }
        when (onboarding) {
            Onboarding.Profiles -> ProfilesScreen(vm)
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
    // Las pantallas que se abren encima (un pasaje, un don...) se apilan sin cerrar las de abajo,
    // así al volver cada una sigue exactamente donde la persona la dejó.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                ClassTopBar(vm)
                MainTabBar(vm)
                when (vm.tab) {
                    MainTab.Lessons -> when (val v = vm.view) {
                        null -> Unit
                        is ParticipantView.LessonStep -> LessonScreen(vm, v)
                        is ParticipantView.Question -> QuestionScreen(vm, v)
                        is ParticipantView.BlockDone -> BlockDoneScreen(vm, v)
                        ParticipantView.Results -> LessonsDoneScreen(vm)
                    }
                    MainTab.Quiz -> when (val v = vm.quizView) {
                        is ParticipantView.Question -> QuestionScreen(vm, v, inQuiz = true)
                        is ParticipantView.BlockDone -> BlockDoneScreen(vm, v, inQuiz = true)
                        else -> Unit
                    }
                    MainTab.Results -> if (vm.quizComplete) ResultsTabScreen(vm) else ResultsLockedScreen(vm)
                }
            }
            vm.overlayStack.forEachIndexed { i, overlay ->
                key(i) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .pointerInput(Unit) { detectTapGestures { } },
                    ) {
                        when (overlay) {
                            Overlay.Home -> HomeScreen(vm)
                            Overlay.Curve -> CurveScreen(vm)
                            is Overlay.GiftDetail -> GiftDetailScreen(vm, overlay.code)
                            Overlay.Ministries -> MinistriesScreen(vm)
                            Overlay.Documents -> DocumentsScreen(vm)
                            is Overlay.Scripture -> ScriptureScreen(vm, overlay.ref)
                        }
                    }
                }
            }
            if (vm.overlayStack.isNotEmpty()) BackHandler { vm.closeOverlay() }
        }
    }
}
