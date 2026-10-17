package com.dones.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Todo lo que la app guarda del participante. Se guarda en el teléfono en cada toque. */
@Serializable
data class ParticipantState(
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
    /** Paso del maestro simulado, para retomar si se cierra la app. */
    val simulatedStepIndex: Int = 0,
    val certificateCode: String? = null,
) {
    val onboardingDone: Boolean get() = consentAtMillis != null

    fun answer(item: Int, value: Int, elapsedMillis: Long): ParticipantState =
        copy(
            answers = answers + (item to value),
            answerLog = answerLog + AnswerEvent(item, value, elapsedMillis),
        )

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String): ParticipantState = json.decodeFromString(serializer(), text)
    }
}

@Serializable
data class AnswerEvent(val item: Int, val value: Int, val elapsedMillis: Long)

/** Qué debe ver el participante, según el paso del maestro y lo que ya respondió. */
sealed interface ParticipantView {
    data object Waiting : ParticipantView

    data class Question(
        val item: Item,
        /** Posición dentro de su bloque, desde 1. */
        val positionInBlock: Int,
        val blockSize: Int,
        val block: Int,
        val totalBlocks: Int,
        val currentAnswer: Int?,
        /** Ítem anterior al que puede volver con "‹ Anterior", si existe. */
        val previousItem: Int?,
    ) : ParticipantView

    data class BlockDone(val block: Int, val answered: Int, val total: Int) : ParticipantView
    data class Results(val courseClosed: Boolean) : ParticipantView
}

object ParticipantFlow {

    /**
     * Calcula la pantalla de la clase.
     * - Nunca muestra preguntas de un bloque que el maestro aún no abrió.
     * - Si llegó tarde o se saltó preguntas, primero le muestra las pendientes de bloques ya abiertos.
     * - [reviewItem] permite volver a una pregunta ya respondida ("‹ Anterior").
     */
    fun view(
        content: HoutsContent,
        step: ClassStep,
        participant: ParticipantState,
        reviewItem: Int? = null,
    ): ParticipantView {
        val totalBlocks = content.blocks.size
        val openBlocks = when (step) {
            ClassStep.Waiting -> 0
            is ClassStep.HoutsBlock -> step.block
            ClassStep.Results, ClassStep.Closed -> totalBlocks
        }
        if (openBlocks == 0) return ParticipantView.Waiting

        val visible = HoutsScorer.visibleItems(content, participant.maritalStatus)
            .filter { it.block <= openBlocks }

        val target = reviewItem?.let { r -> visible.firstOrNull { it.n == r } }
            ?: visible.firstOrNull { it.n !in participant.answers }

        if (target != null) {
            val inBlock = visible.filter { it.block == target.block }
            val position = inBlock.indexOfFirst { it.n == target.n }
            val previous = visible.getOrNull(visible.indexOfFirst { it.n == target.n } - 1)?.n
            return ParticipantView.Question(
                item = target,
                positionInBlock = position + 1,
                blockSize = inBlock.size,
                block = target.block,
                totalBlocks = totalBlocks,
                currentAnswer = participant.answers[target.n],
                previousItem = previous,
            )
        }

        return when (step) {
            is ClassStep.HoutsBlock -> ParticipantView.BlockDone(
                block = step.block,
                answered = visible.size,
                total = HoutsScorer.visibleItems(content, participant.maritalStatus).size,
            )
            ClassStep.Results -> ParticipantView.Results(courseClosed = false)
            ClassStep.Closed -> ParticipantView.Results(courseClosed = true)
            ClassStep.Waiting -> ParticipantView.Waiting
        }
    }

    /** Última pregunta respondida, para que "‹ Anterior" funcione desde "Bloque terminado". */
    fun lastAnswered(content: HoutsContent, participant: ParticipantState, openBlocks: Int): Int? =
        HoutsScorer.visibleItems(content, participant.maritalStatus)
            .filter { it.block <= openBlocks && it.n in participant.answers }
            .lastOrNull()?.n
}
