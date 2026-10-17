package com.dones.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer

/** La enseñanza del curso, tal como viene en assets/leccion-v1.json: una idea por página. */
@Serializable
data class Lesson(val version: Int, val pages: List<LessonPage>) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): Lesson = json.decodeFromString(serializer(), text)
    }
}

@Serializable
data class LessonPage(
    val section: String,
    val title: String? = null,
    val text: String = "",
    val ref: String? = null,
    /** Respuesta de cada "___" del texto, en orden. Varias aceptadas separadas por "|". */
    val blanks: List<String> = emptyList(),
    /** Pregunta abierta: la persona escribe lo que quiera (opcional). */
    val write: Boolean = false,
    /** Si no es nulo, esta página son las afirmaciones de ese bloque del cuestionario. */
    val houtsBlock: Int? = null,
)

object Blanks {
    const val MARK = "___"

    /** Acepta sin tildes ni mayúsculas, y respuestas parciales ("institución" por "la institución"). */
    fun isCorrect(expected: String, given: String): Boolean {
        val g = normalize(given)
        if (g.isEmpty()) return false
        return expected.split("|").map(::normalize).any { e ->
            e == g || (g.length >= 4 && (e.contains(g) || g.contains(e)))
        }
    }

    /** Lo que se muestra como respuesta: la primera opción. */
    fun display(expected: String): String = expected.split("|").first()

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("[^a-z0-9ñ ]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
