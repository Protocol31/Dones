package com.dones.app

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dones.app.data.BibleRepository
import com.dones.app.data.Storage
import com.dones.app.data.Verse
import com.dones.core.Bible
import com.dones.core.BibleRef
import com.dones.app.pdf.PdfDocuments
import com.dones.core.CertificateCode
import com.dones.core.ClassState
import com.dones.core.HoutsContent
import com.dones.core.HoutsResult
import com.dones.core.HoutsScorer
import com.dones.core.JoinResult
import com.dones.core.Lesson
import com.dones.core.ParticipantFlow
import com.dones.core.ParticipantState
import com.dones.core.ParticipantView
import com.dones.core.ProfileBook
import com.dones.core.QualityChecker
import com.dones.core.QualityReport
import com.dones.core.SimulatedClassSession
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/** Pantallas antes de entrar a la clase (una tarea por pantalla). Profiles: "¿Quién eres?". */
enum class Onboarding { Profiles, JoinCode, Scanner, Connected, Purpose, Name, Age, Marital, WhatsApp, Review, Consent }

/** Pantallas que se abren encima (se apilan: "‹ Volver" regresa a la anterior). */
sealed interface Overlay {
    /** Inicio: seguir donde quedó, ver la lección desde el principio, cambiar de persona. */
    data object Home : Overlay
    data object Curve : Overlay
    data class GiftDetail(val code: String) : Overlay
    data object Ministries : Overlay
    data object Documents : Overlay
    /** Un pasaje bíblico (RVR 1960), por ejemplo "Ef 4:11-14". */
    data class Scripture(val ref: String) : Overlay
}

/** Estado de la pantalla del pasaje bíblico. */
sealed interface ScriptureState {
    data object Loading : ScriptureState
    data class Loaded(val ref: BibleRef, val verses: List<Verse>) : ScriptureState
    /** Sin internet (y no estaba guardado) o cita que no se entiende. */
    data class Failed(val ref: BibleRef?) : ScriptureState
}

class DonesViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = Storage(app)
    private val bible = BibleRepository(app)
    val content: HoutsContent = storage.loadContent()
    val lesson: Lesson = storage.loadLesson()

    /** Todos los perfiles guardados en este teléfono. */
    var book by mutableStateOf(storage.loadProfiles())
        private set

    /** La persona que está usando la app ahora. */
    var participant by mutableStateOf(book.active ?: ParticipantState())
        private set

    private val session = SimulatedClassSession()

    var classState by mutableStateOf<ClassState?>(null)
        private set
    var onboarding by mutableStateOf<Onboarding?>(null)
        private set
    private var overlays by mutableStateOf(listOf<Overlay>())
    val overlay: Overlay? get() = overlays.lastOrNull()
    var joinError by mutableStateOf<String?>(null)
        private set
    /** Pregunta ya respondida a la que la persona volvió con "‹ Anterior". */
    private var reviewItem by mutableStateOf<Int?>(null)
    private var questionShownAt = SystemClock.elapsedRealtime()

    init {
        viewModelScope.launch { session.state.collect { classState = it } }
        // Al abrir la app: si ya hay perfiles en el teléfono, primero "¿Quién eres?".
        onboarding = if (book.profiles.isEmpty()) {
            participant = newProfile()
            Onboarding.JoinCode
        } else {
            Onboarding.Profiles
        }
    }

    // ---------- Perfiles (guardados solo en el teléfono) ----------

    private fun newProfile() = ParticipantState(id = storage.newId(), createdAtMillis = System.currentTimeMillis())

    /** Elige su perfil y sigue exactamente donde quedó. */
    fun selectProfile(id: String) {
        val chosen = book.profiles.firstOrNull { it.id == id } ?: return
        session.leave()
        book = book.select(id)
        storage.saveProfiles(book)
        participant = chosen
        chosen.classCode?.let { session.join(it) }
        overlays = emptyList()
        reviewItem = null
        joinError = null
        onboarding = when {
            chosen.classCode == null -> Onboarding.JoinCode
            !chosen.onboardingDone -> Onboarding.Connected
            else -> null
        }
        if (onboarding == null) goToFurthest()
    }

    /** Persona nueva en este teléfono. */
    fun startNewProfile() {
        session.leave()
        participant = newProfile()
        overlays = emptyList()
        reviewItem = null
        joinError = null
        onboarding = Onboarding.JoinCode
    }

    /** Vuelve a "¿Quién eres?" sin borrar nada. */
    fun switchProfile() {
        overlays = emptyList()
        reviewItem = null
        onboarding = if (book.profiles.isEmpty()) Onboarding.JoinCode else Onboarding.Profiles
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

    /** Desde la pantalla del código: volver a "¿Quién eres?" si hay perfiles guardados. */
    val canGoToProfiles: Boolean get() = book.profiles.isNotEmpty()

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
            Onboarding.JoinCode -> if (book.profiles.isNotEmpty()) Onboarding.Profiles else Onboarding.JoinCode
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
        get() = classState?.let { ParticipantFlow.view(content, lesson, participant, reviewItem) }

    /** Cada persona avanza sola por la enseñanza. */
    fun nextPage() = goToPage(participant.page + 1)

    fun previousPage() = goToPage(participant.page - 1)

    /** "Ir a donde quedé": la página más lejana a la que llegó (y ahí, la primera pregunta sin responder). */
    fun goToFurthest() = goToPage(participant.furthestPage)

    /** "Ver la lección desde el principio". */
    fun goToStart() = goToPage(0)

    private fun goToPage(target: Int) {
        update { it.goToPage(target, lesson.pages.size) }
        reviewItem = null
        overlays = emptyList()
        questionShownAt = SystemClock.elapsedRealtime()
    }

    /** Guarda lo escrito en una página: espacios para completar o pregunta abierta. */
    fun saveNotes(page: Int, values: List<String>) {
        update { p -> p.copy(notes = p.notes + values.mapIndexed { i, v -> "$page.$i" to v.trim() }) }
    }

    fun answer(item: Int, value: Int) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (reviewItem == null) now - questionShownAt else 0L // corregir no cuenta como tiempo de lectura
        update { it.answer(item, value, elapsed) }
        reviewItem = null
        questionShownAt = now
    }

    fun goBackTo(item: Int) { reviewItem = item }

    fun backFromBlockDone(block: Int) {
        reviewItem = ParticipantFlow.lastInBlock(content, participant, block)
    }

    /** Borra el perfil de esta persona (respuestas, notas y PDFs) y vuelve a "¿Quién eres?". */
    fun deleteCurrentProfile() {
        session.leave()
        storage.documentsDir(participant.id).deleteRecursively()
        book = book.remove(participant.id)
        storage.saveProfiles(book)
        participant = newProfile()
        overlays = emptyList()
        reviewItem = null
        joinError = null
        onboarding = if (book.profiles.isEmpty()) Onboarding.JoinCode else Onboarding.Profiles
    }

    // ---------- Resultados ----------

    val result: HoutsResult
        get() = HoutsScorer.score(content, participant.answers, participant.maritalStatus)

    val quality: QualityReport
        get() = QualityChecker.check(participant.answerLog)

    /** Al terminar la última página el curso queda cerrado para esa persona y recibe su certificado. */
    val courseClosed: Boolean
        get() = view == ParticipantView.Results

    /** Abre una pantalla encima; "‹ Volver" regresa exactamente a la anterior. */
    fun open(o: Overlay) { if (overlay != o) overlays = overlays + o }
    fun closeOverlay() { overlays = overlays.dropLast(1) }

    // ---------- Biblia ----------

    var scripture by mutableStateOf<ScriptureState>(ScriptureState.Loading)
        private set

    /** Abre la pantalla para leer la cita en RVR 1960. */
    fun openScripture(ref: String) = open(Overlay.Scripture(ref))

    fun loadScripture(text: String) {
        val ref = Bible.parse(text)
        if (ref == null) { scripture = ScriptureState.Failed(null); return }
        scripture = ScriptureState.Loading
        viewModelScope.launch {
            scripture = runCatching { bible.load(ref) }
                .fold({ ScriptureState.Loaded(ref, it) }, { ScriptureState.Failed(ref) })
        }
    }

    fun resultsPdf(): File = PdfDocuments.results(
        dir = storage.documentsDir(participant.id),
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
        return PdfDocuments.certificate(storage.documentsDir(participant.id), participant.name, info, today, code)
    }

    /** Cada cambio se guarda al tiro en el perfil, en el teléfono. */
    private fun update(change: (ParticipantState) -> ParticipantState) {
        participant = change(participant)
        book = book.save(participant)
        storage.saveProfiles(book)
    }
}
