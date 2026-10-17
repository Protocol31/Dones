package com.dones.core

/** Señales de que las respuestas pueden no ser confiables. Nunca se muestran como "mentiste". */
data class QualityReport(
    val longestSameAnswerRun: Int,
    val averageMillisPerAnswer: Long?,
) {
    val straightLining: Boolean get() = longestSameAnswerRun > STRAIGHT_LINE_LIMIT
    val tooFast: Boolean get() = averageMillisPerAnswer != null && averageMillisPerAnswer < FAST_LIMIT_MILLIS
    val needsConversation: Boolean get() = straightLining || tooFast

    companion object {
        const val STRAIGHT_LINE_LIMIT = 20
        const val FAST_LIMIT_MILLIS = 1500L
    }
}

object QualityChecker {
    fun check(log: List<AnswerEvent>): QualityReport {
        // Solo cuenta la última respuesta de cada ítem, en el orden en que se respondió.
        val finalAnswers = log.reversed().distinctBy { it.item }.reversed()
        var longest = 0
        var run = 0
        var last: Int? = null
        for (e in finalAnswers) {
            run = if (e.value == last) run + 1 else 1
            last = e.value
            longest = maxOf(longest, run)
        }
        val timed = log.map { it.elapsedMillis }.filter { it > 0 }
        return QualityReport(
            longestSameAnswerRun = longest,
            averageMillisPerAnswer = if (timed.size < 10) null else timed.average().toLong(),
        )
    }
}

object CertificateCode {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // sin 0/O ni 1/I

    /** Código corto y estable, por ejemplo "DMM-4K7Q-9ZRX". */
    fun generate(name: String, classCode: String, dateIso: String): String {
        var h = 1125899906842597L
        for (c in "$name|$classCode|$dateIso".lowercase()) h = 31 * h + c.code
        val chars = (0 until 8).map { i -> ALPHABET[((h ushr (i * 5)) and 31L).toInt()] }
        return "DMM-" + chars.take(4).joinToString("") + "-" + chars.drop(4).joinToString("")
    }
}
