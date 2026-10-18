package com.dones.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Todo lo que la app guarda del participante. Se guarda en el teléfono en cada toque. */
@Serializable
data class ParticipantState(
    /** Identifica el perfil dentro del teléfono (varias personas pueden usar el mismo celular). */
    val id: String = "",
    val createdAtMillis: Long = 0,
    val classCode: String? = null,
    val name: String = "",
    val ageRange: String? = null,
    val maritalStatus: String? = null,
    val whatsapp: String = "",
    val whatsappDeclined: Boolean = false,
    val consentAtMillis: Long? = null,
    /** Número de ítem → valor (3 Mucho, 2 Algo, 1 Poco, 0 Nada). */
    val answers: Map<Int, Int> = emptyMap(),
    /** Orden en que se respondió y cuánto tardó cada respuesta (para los indicadores de calidad). */
    val answerLog: List<AnswerEvent> = emptyList(),
    /** Página de la enseñanza en la que va (índice en Lesson.pages); al pasar la última, ve su resultado. */
    val page: Int = 0,
    /** Página más lejana a la que ha llegado: "Ir a donde quedé" vuelve aquí. */
    val furthestPage: Int = 0,
    /** Lo que escribió en la enseñanza: "página.espacio" → texto. */
    val notes: Map<String, String> = emptyMap(),
    val certificateCode: String? = null,
) {
    val onboardingDone: Boolean get() = consentAtMillis != null

    fun answer(item: Int, value: Int, elapsedMillis: Long): ParticipantState =
        copy(
            answers = answers + (item to value),
            answerLog = answerLog + AnswerEvent(item, value, elapsedMillis),
        )

    /** Mueve a una página sin pasarse del final ([last] = Lesson.pages.size, que son los resultados). */
    fun goToPage(target: Int, last: Int): ParticipantState {
        val p = target.coerceIn(0, last)
        return copy(page = p, furthestPage = maxOf(furthestPage, p))
    }

    /** ¿Está más atrás de donde llegó? Entonces se muestra "Ir a donde quedé". */
    val isBehind: Boolean get() = page < furthestPage

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String): ParticipantState = json.decodeFromString(serializer(), text)
    }
}

@Serializable
data class AnswerEvent(val item: Int, val value: Int, val elapsedMillis: Long)

/** Qué debe ver el participante, según la página en la que va y lo que ya respondió. */
sealed interface ParticipantView {
    /** Página de la enseñanza: texto, completar espacios o pregunta abierta. */
    data class LessonStep(
        val page: LessonPage,
        val index: Int,
        /** Posición dentro de su sección, desde 1. */
        val positionInSection: Int,
        val sectionSize: Int,
    ) : ParticipantView

    data class Question(
        val item: Item,
        /** Posición dentro de su bloque, desde 1. */
        val positionInBlock: Int,
        val blockSize: Int,
        val block: Int,
        val totalBlocks: Int,
        val currentAnswer: Int?,
        /** Ítem anterior del bloque al que puede volver con "‹ Anterior"; si es nulo, vuelve a la página anterior. */
        val previousItem: Int?,
    ) : ParticipantView

    data class BlockDone(
        val block: Int,
        val answered: Int,
        val total: Int,
        /** Último bloque del cuestionario: después vienen los resultados. */
        val isLastBlock: Boolean,
    ) : ParticipantView
    data object Results : ParticipantView
}

object ParticipantFlow {

    /**
     * Calcula la pantalla según la página en la que va la persona.
     * - Las afirmaciones de un bloque solo aparecen al llegar a su página en la enseñanza.
     * - [reviewItem] permite volver a una pregunta ya respondida ("‹ Anterior").
     */
    fun view(
        content: HoutsContent,
        lesson: Lesson,
        participant: ParticipantState,
        reviewItem: Int? = null,
    ): ParticipantView {
        val index = participant.page
        val page = lesson.pages.getOrNull(index) ?: return ParticipantView.Results
        val block = page.houtsBlock ?: run {
            val section = lesson.pages.indices.filter { lesson.pages[it].section == page.section && lesson.pages[it].houtsBlock == null }
            return ParticipantView.LessonStep(page, index, section.indexOf(index) + 1, section.size)
        }

        val all = HoutsScorer.visibleItems(content, participant.maritalStatus)
        val inBlock = all.filter { it.block == block }
        val target = reviewItem?.let { r -> inBlock.firstOrNull { it.n == r } }
            ?: inBlock.firstOrNull { it.n !in participant.answers }

        if (target != null) {
            val position = inBlock.indexOfFirst { it.n == target.n }
            return ParticipantView.Question(
                item = target,
                positionInBlock = position + 1,
                blockSize = inBlock.size,
                block = block,
                totalBlocks = content.blocks.size,
                currentAnswer = participant.answers[target.n],
                previousItem = inBlock.getOrNull(position - 1)?.n,
            )
        }
        return ParticipantView.BlockDone(
            block = block,
            answered = all.count { it.n in participant.answers },
            total = all.size,
            isLastBlock = block == content.blocks.size,
        )
    }

    /** Última pregunta del bloque, para que "‹ Anterior" funcione desde "Bloque terminado". */
    fun lastInBlock(content: HoutsContent, participant: ParticipantState, block: Int): Int? =
        HoutsScorer.visibleItems(content, participant.maritalStatus).lastOrNull { it.block == block }?.n
}
