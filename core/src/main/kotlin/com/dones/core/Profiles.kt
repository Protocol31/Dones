package com.dones.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Los perfiles guardados en el teléfono (nada se sube a internet).
 * Cada persona sigue desde donde quedó en la clase siguiente; varias personas pueden compartir celular.
 */
@Serializable
data class ProfileBook(
    val profiles: List<ParticipantState> = emptyList(),
    val activeId: String? = null,
) {
    val active: ParticipantState? get() = profiles.firstOrNull { it.id == activeId }

    /** Guarda los cambios del perfil (o lo agrega si es nuevo) y lo deja como el activo. */
    fun save(profile: ParticipantState): ProfileBook {
        require(profile.id.isNotBlank()) { "El perfil necesita un id." }
        val exists = profiles.any { it.id == profile.id }
        return copy(
            profiles = if (exists) profiles.map { if (it.id == profile.id) profile else it } else profiles + profile,
            activeId = profile.id,
        )
    }

    fun select(id: String?): ProfileBook = copy(activeId = id?.takeIf { i -> profiles.any { it.id == i } })

    fun remove(id: String): ProfileBook =
        copy(profiles = profiles.filterNot { it.id == id }, activeId = activeId.takeIf { it != id })

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String): ProfileBook = json.decodeFromString(serializer(), text)

        /** Pasa el archivo de la versión anterior (una sola persona) a un perfil. */
        fun fromLegacy(state: ParticipantState, newId: String): ProfileBook =
            if (state.name.isBlank() && state.answers.isEmpty()) ProfileBook()
            else ProfileBook().save(state.copy(id = newId, furthestPage = maxOf(state.furthestPage, state.page)))
    }
}
