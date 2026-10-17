package com.dones.app

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dones.app.data.Storage
import com.dones.app.pdf.PdfDocuments
import com.dones.core.CertificateCode
import com.dones.core.ClassState
import com.dones.core.ClassStep
import com.dones.core.HoutsContent
import com.dones.core.HoutsResult
import com.dones.core.HoutsScorer
import com.dones.core.JoinResult
import com.dones.core.ParticipantFlow
import com.dones.core.ParticipantState
import com.dones.core.ParticipantView
import com.dones.core.QualityChecker
import com.dones.core.QualityReport
import com.dones.core.SimulatedClassSession
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/** Pantallas antes de entrar a la clase (una tarea por pantalla). */
enum class Onboarding { JoinCode, Scanner, Connected, Purpose, Name, Age, Marital, WhatsApp, Review, Consent }

/** Pantallas que se abren encima del resultado. */
sealed interface Overlay {
    data object Curve : Overlay
    data class GiftDetail(val code: String) : Overlay
    data object Ministries : Overlay
    data object Documents : Overlay
}

class DonesViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = Storage(app)
    val content: HoutsContent = storage.loadContent()

    var participant by mutableStateOf(storage.loadState())
        private set

    private val session = SimulatedClassSession(
        blocks = content.blocks.map { it.block },
        initialStepIndex = if (participant.classCode != null) participant.simulatedStepIndex else 0,
        onStepChanged = { index -> update { it.copy(simulatedStepIndex = index) } },
    )

    var classState by mutableStateOf<ClassState?>(null)
        private set
    var onboarding by mutableStateOf<Onboarding?>(null)
        private set
    var overlay by mutableStateOf<Overlay?>(null)
        private set
    var joinError by mutableStateOf<String?>(null)
        private set
    /** Pregunta ya respondida a la que la persona volvió con "‹ Anterior". */
    private var reviewItem by mutableStateOf<Int?>(null)
    private var questionShownAt = SystemClock.elapsedRealtime()

    init {
        viewModelScope.launch { session.state.collect { classState = it } }
        participant.classCode?.let { session.join(it) }
        onboarding = when {
            participant.classCode == null -> Onboarding.JoinCode
            !participant.onboardingDone -> Onboarding.Connected
            else -> null
        }
    }

    // ---------- Conectarse a la clase ----------

    fun submitCode(raw: String) {
        when (val r = session.join(raw)) {
            is JoinResult.Joined -> {
                joinError = null
                update { it.copy(classCode = r.state.info.code) }
                onboarding = if (participant.onboardingDone) null else Onboarding.Connected
            }
            JoinResult.InvalidCode -> joinError = "El código tiene 6 números. Revísalo en la pantalla."
            JoinResult.ClassNotFound -> joinError =
                "No encontramos una clase con ese código. En este demo, usa el código de prueba 000000."
        }
    }

    fun openScanner() { joinError = null; onboarding = Onboarding.Scanner }
    fun onQrScanned(raw: String) { onboarding = Onboarding.JoinCode; submitCode(raw) }

    // ---------- Datos ----------

    fun goTo(step: Onboarding) { onboarding = step }

    fun setName(name: String) {
        update { it.copy(name = name.trim().replace(Regex("\\s+"), " ")) }
        onboarding = Onboarding.Age
    }

    fun setAge(range: String) { update { it.copy(ageRange = range) }; onboarding = Onboarding.Marital }

    fun setMarital(id: String) { update { it.copy(maritalStatus = id) }; onboarding = Onboarding.WhatsApp }

    fun setWhatsapp(countryCode: String, number: String) {
        val digits = (countryCode + number).filter { it.isDigit() }
        update { it.copy(whatsapp = "+$digits", whatsappDeclined = false) }
        onboarding = Onboarding.Review
    }

    fun declineWhatsapp() {
        update { it.copy(whatsapp = "", whatsappDeclined = true) }
        onboarding = Onboarding.Review
    }

    fun acceptConsent() {
        update { it.copy(consentAtMillis = System.currentTimeMillis()) }
        onboarding = null
        questionShownAt = SystemClock.elapsedRealtime()
    }

    /** Botón "‹ Volver" de las pantallas de datos. */
    fun onboardingBack() {
        onboarding = when (onboarding) {
            Onboarding.Scanner -> Onboarding.JoinCode
            Onboarding.Purpose -> Onboarding.Connected
            Onboarding.Name -> Onboarding.Purpose
            Onboarding.Age -> Onboarding.Name
            Onboarding.Marital -> Onboarding.Age
            Onboarding.WhatsApp -> Onboarding.Marital
            Onboarding.Review -> Onboarding.WhatsApp
            Onboarding.Consent -> Onboarding.Review
            else -> onboarding
        }
    }

    // ---------- Clase ----------

    val view: ParticipantView?
        get() = classState?.let { ParticipantFlow.view(content, it.current, participant, reviewItem) }

    fun answer(item: Int, value: Int) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (reviewItem == null) now - questionShownAt else 0L // corregir no cuenta como tiempo de lectura
        update { it.answer(item, value, elapsed) }
        reviewItem = null
        questionShownAt = now
    }

    fun goBackTo(item: Int) { reviewItem = item }

    fun backFromBlockDone() {
        val open = (classState?.current as? ClassStep.HoutsBlock)?.block ?: content.blocks.size
        reviewItem = ParticipantFlow.lastAnswered(content, participant, open)
    }

    // Maestro simulado (solo demo)
    fun teacherNext() { session.teacherNext(); reviewItem = null }
    fun teacherPrevious() { session.teacherPrevious(); reviewItem = null }

    /** Borra todo y vuelve a la pantalla del código (para repetir la prueba). */
    fun resetDemo() {
        session.leave()
        storage.documentsDir().listFiles()?.forEach { it.delete() }
        update { ParticipantState() }
        overlay = null
        reviewItem = null
        joinError = null
        onboarding = Onboarding.JoinCode
    }

    // ---------- Resultados ----------

    val result: HoutsResult
        get() = HoutsScorer.score(content, participant.answers, participant.maritalStatus)

    val quality: QualityReport
        get() = QualityChecker.check(participant.answerLog)

    val courseClosed: Boolean
        get() = classState?.current == ClassStep.Closed

    fun open(o: Overlay) { overlay = o }
    fun closeOverlay() {
        overlay = when (overlay) {
            is Overlay.GiftDetail -> Overlay.Curve
            else -> null
        }
    }

    fun resultsPdf(): File = PdfDocuments.results(
        dir = storage.documentsDir(),
        content = content,
        participant = participant,
        result = result,
        info = classState?.info,
    )

    /** El certificado existe solo cuando el maestro cierra el curso. */
    fun certificatePdf(): File? {
        if (!courseClosed) return null
        val info = classState?.info ?: return null
        val today = LocalDate.now()
        val code = participant.certificateCode
            ?: CertificateCode.generate(participant.name, info.code, today.toString()).also { c ->
                update { it.copy(certificateCode = c) }
            }
        return PdfDocuments.certificate(storage.documentsDir(), participant.name, info, today, code)
    }

    private fun update(change: (ParticipantState) -> ParticipantState) {
        participant = change(participant)
        storage.saveState(participant)
    }
}
