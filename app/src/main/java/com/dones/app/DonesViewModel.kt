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
import com.dones.core.JoinCode
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
enum class Onboarding { Profiles, Purpose, Name, Age, Marital, WhatsApp, Review, Consent }

/** Las tres pestañas de arriba: lecciones de clase, el cuestionario de 125 afirmaciones y los resultados. */
enum class MainTab { Lessons, Quiz, Results }

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

    /** Pestaña abierta. */
    var tab by mutableStateOf(MainTab.Lessons)
        private set
    /** Bloque del cuestionario recién terminado (se muestra "¡Bloque terminado!" hasta que la persona continúe). */
    private var quizPause by mutableStateOf<Int?>(null)

    var classState by mutableStateOf<ClassState?>(null)
        private set
    var onboarding by mutableStateOf<Onboarding?>(null)
        private set
    private var overlays by mutableStateOf(listOf<Overlay>())
    val overlay: Overlay? get() = overlays.lastOrNull()
    /** Todas las pantallas abiertas encima, de la primera a la última: así cada una conserva dónde iba. */
    val overlayStack: List<Overlay> get() = overlays
    /** Pregunta ya respondida a la que la persona volvió con "‹ Anterior". */
    private var reviewItem by mutableStateOf<Int?>(null)
    private var questionShownAt = SystemClock.elapsedRealtime()

    init {
        // Ya no se pide ningún código: cada persona hace el curso en su propio teléfono.
        classState = (session.join(JoinCode.DEMO_CODE) as? JoinResult.Joined)?.state
        viewModelScope.launch { session.state.collect { classState = it } }
        // Al abrir la app: si ya hay perfiles en el teléfono, primero "¿Quién eres?".
        onboarding = if (book.profiles.isEmpty()) {
            participant = newProfile()
            Onboarding.Purpose
        } else {
            Onboarding.Profiles
        }
    }

    // ---------- Perfiles (guardados solo en el teléfono) ----------

    private fun newProfile() = ParticipantState(id = storage.newId(), createdAtMillis = System.currentTimeMillis(), classCode = JoinCode.DEMO_CODE)

    /** Elige su perfil y sigue exactamente donde quedó. */
    fun selectProfile(id: String) {
        val chosen = book.profiles.firstOrNull { it.id == id } ?: return
        book = book.select(id)
        storage.saveProfiles(book)
        participant = chosen
        overlays = emptyList()
        reviewItem = null
        quizPause = null
        tab = MainTab.Lessons
        onboarding = if (!chosen.onboardingDone) Onboarding.Purpose else null
        if (onboarding == null) goToFurthest()
    }

    /** Persona nueva en este teléfono. */
    fun startNewProfile() {
        participant = newProfile()
        overlays = emptyList()
        reviewItem = null
        quizPause = null
        tab = MainTab.Lessons
        onboarding = Onboarding.Purpose
    }

    /** Vuelve a "¿Quién eres?" sin borrar nada. */
    fun switchProfile() {
        overlays = emptyList()
        reviewItem = null
        onboarding = if (book.profiles.isEmpty()) Onboarding.Purpose else Onboarding.Profiles
    }

    // ---------- Datos ----------

    fun goTo(step: Onboarding) { onboarding = step }

    /** Volver a "¿Quién eres?" si hay perfiles guardados. */
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
            Onboarding.Purpose -> if (book.profiles.isNotEmpty()) Onboarding.Profiles else Onboarding.Purpose
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

    /** Pantalla de la pestaña "Lecciones": solo las páginas de enseñanza (las del cuestionario viven en su pestaña). */
    val view: ParticipantView?
        get() = classState?.let {
            val i = lessonDisplayIndex(participant.page)
            ParticipantFlow.view(content, lesson, if (i == participant.page) participant else participant.copy(page = i), null)
        }

    private fun isLessonPage(i: Int) = lesson.pages.getOrNull(i)?.houtsBlock == null

    /** Si la persona quedó en una página de preguntas (versión anterior), se muestra la siguiente de enseñanza. */
    private fun lessonDisplayIndex(i: Int): Int {
        var t = i
        while (t < lesson.pages.size && !isLessonPage(t)) t++
        return t
    }

    private fun lessonAfter(from: Int): Int = lessonDisplayIndex(from + 1)

    private fun lessonBefore(from: Int): Int {
        var t = from - 1
        while (t > 0 && !isLessonPage(t)) t--
        return t.coerceAtLeast(0)
    }

    private fun lessonAtOrBefore(i: Int): Int {
        var t = i.coerceAtMost(lesson.pages.size - 1)
        while (t > 0 && !isLessonPage(t)) t--
        return t.coerceAtLeast(0)
    }

    /** ¿Está más atrás de donde llegó en las lecciones? Entonces se muestra "Ir a donde quedé". */
    val isBehind: Boolean
        get() {
            val f = participant.furthestPage
            val at = if (f >= lesson.pages.size) lesson.pages.size else lessonAtOrBefore(f)
            return lessonDisplayIndex(participant.page) < at
        }

    /** Cada persona avanza sola por la enseñanza. */
    fun nextPage() = goToPage(lessonAfter(participant.page))

    fun previousPage() = goToPage(lessonBefore(participant.page))

    /** "Ir a donde quedé": la lección más lejana a la que llegó. */
    fun goToFurthest() {
        val f = participant.furthestPage
        goToPage(if (f >= lesson.pages.size) lesson.pages.size else lessonAtOrBefore(f))
    }

    /** "Ver la lección desde el principio". */
    fun goToStart() = goToPage(0)

    private fun goToPage(target: Int) {
        update { it.goToPage(target, lesson.pages.size) }
        reviewItem = null
        overlays = emptyList()
        tab = MainTab.Lessons
        questionShownAt = SystemClock.elapsedRealtime()
    }

    // ---------- Pestañas ----------

    fun openTab(t: MainTab) {
        tab = t
        overlays = emptyList()
        reviewItem = null
        questionShownAt = SystemClock.elapsedRealtime()
    }

    private fun quizItems() =
        HoutsScorer.visibleItems(content, participant.maritalStatus).sortedWith(compareBy({ it.block }, { it.n }))

    val quizTotal: Int get() = quizItems().size
    val quizAnswered: Int get() = quizItems().count { it.n in participant.answers }

    /** Los resultados se abren solo cuando respondió todas las afirmaciones que le tocan. */
    val quizComplete: Boolean get() = quizTotal > 0 && quizAnswered == quizTotal

    /** Pantalla de la pestaña "Cuestionario": las afirmaciones en orden, bloque por bloque. */
    val quizView: ParticipantView
        get() {
            val ordered = quizItems()
            val answered = ordered.count { it.n in participant.answers }
            val lastBlock = content.blocks.size
            val pause = quizPause
            if (pause != null) return ParticipantView.BlockDone(pause, answered, ordered.size, pause == lastBlock)
            val target = reviewItem?.let { r -> ordered.firstOrNull { it.n == r } }
                ?: ordered.firstOrNull { it.n !in participant.answers }
                ?: return ParticipantView.BlockDone(lastBlock, answered, ordered.size, true)
            val inBlock = ordered.filter { it.block == target.block }
            val at = ordered.indexOfFirst { it.n == target.n }
            return ParticipantView.Question(
                item = target,
                positionInBlock = inBlock.indexOfFirst { it.n == target.n } + 1,
                blockSize = inBlock.size,
                block = target.block,
                totalBlocks = lastBlock,
                currentAnswer = participant.answers[target.n],
                previousItem = ordered.getOrNull(at - 1)?.n,
            )
        }

    /** "Continuar" después de un bloque: sigue con el próximo o, al terminar todo, abre los resultados. */
    fun quizContinue() {
        quizPause = null
        reviewItem = null
        questionShownAt = SystemClock.elapsedRealtime()
        if (quizComplete) tab = MainTab.Results
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
        // Si con esta respuesta quedó completo su bloque, se muestra "¡Bloque terminado!".
        val block = content.item(item).block
        if (quizItems().none { it.block == block && it.n !in participant.answers }) quizPause = block
    }

    fun goBackTo(item: Int) { reviewItem = item }

    fun backFromBlockDone(block: Int) {
        quizPause = null
        reviewItem = ParticipantFlow.lastInBlock(content, participant, block)
    }

    /** Borra el perfil de esta persona (respuestas, notas y PDFs) y vuelve a "¿Quién eres?". */
    fun deleteCurrentProfile() {
        storage.documentsDir(participant.id).deleteRecursively()
        book = book.remove(participant.id)
        storage.saveProfiles(book)
        participant = newProfile()
        overlays = emptyList()
        reviewItem = null
        quizPause = null
        tab = MainTab.Lessons
        onboarding = if (book.profiles.isEmpty()) Onboarding.Purpose else Onboarding.Profiles
    }

    // ---------- Resultados ----------

    val result: HoutsResult
        get() = HoutsScorer.score(content, participant.answers, participant.maritalStatus)

    val quality: QualityReport
        get() = QualityChecker.check(participant.answerLog)

    /** Al terminar la última página el curso queda cerrado para esa persona y recibe su certificado. */
    val courseClosed: Boolean
        get() = participant.page >= lesson.pages.size

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
        context = getApplication<Application>(),
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
            ?: CertificateCode.random().also { c ->
                update { it.copy(certificateCode = c) }
            }
        return PdfDocuments.certificate(getApplication<Application>(), storage.documentsDir(participant.id), participant.name, info, today, code)
    }

    /** Cada cambio se guarda al tiro en el perfil, en el teléfono. */
    private fun update(change: (ParticipantState) -> ParticipantState) {
        participant = change(participant)
        book = book.save(participant)
        storage.saveProfiles(book)
    }
}
