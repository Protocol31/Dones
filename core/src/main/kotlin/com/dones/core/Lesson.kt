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
    /** Opciones para marcar (latidos del corazón, rasgos, talentos…). */
    val choices: List<String> = emptyList(),
    /** Cuántas opciones puede marcar: 1 = solo una, 0 = sin límite. */
    val pick: Int = 0,
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

/** No se avanza sin responder: qué necesita cada página para dejar pasar. */
object LessonGate {
    /**
     * - Espacios para completar: todos escritos (luego se toca "Revisar").
     * - Opciones para marcar: al menos una.
     * - Pregunta abierta: algo escrito.
     * - Texto o versículo: siempre deja pasar.
     */
    fun canContinue(page: LessonPage, values: List<String>, selected: Collection<String>): Boolean = when {
        page.blanks.isNotEmpty() -> values.size >= page.blanks.size && values.take(page.blanks.size).all { it.isNotBlank() }
        page.choices.isNotEmpty() -> selected.isNotEmpty()
        page.write -> values.firstOrNull()?.isNotBlank() == true
        else -> true
    }

    /** Lo que se le dice a la persona cuando todavía no puede avanzar. */
    fun hint(page: LessonPage): String? = when {
        page.blanks.size > 1 -> "Completa todos los espacios para seguir."
        page.blanks.isNotEmpty() -> "Escribe tu respuesta para seguir."
        page.choices.isNotEmpty() -> "Marca al menos una opción para seguir."
        page.write -> "Escribe tu respuesta para seguir."
        else -> null
    }
}
