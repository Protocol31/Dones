package com.dones.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Contenido del cuestionario, tal como viene en assets/houts-v1.json. */
@Serializable
data class HoutsContent(
    val version: Int,
    val maritalStatus: List<Option>,
    val ageRanges: List<String>,
    val scale: List<ScaleOption>,
    val prompt: String,
    val blocks: List<Block>,
    val gifts: List<Gift>,
    val items: List<Item>,
    val ministries: List<Ministry>,
) {
    fun gift(code: String): Gift = gifts.first { it.code == code }
    fun item(n: Int): Item = items.first { it.n == n }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): HoutsContent = json.decodeFromString(serializer(), text)
    }
}

@Serializable
data class Option(val id: String, val label: String)

@Serializable
data class ScaleOption(val label: String, val value: Int)

@Serializable
data class Block(val block: Int, val items: List<Int>)

@Serializable
data class Gift(
    val code: String,
    val name: String,
    val definition: String,
    val refs: List<String> = emptyList(),
)

@Serializable
data class Item(
    val n: Int,
    val text: String,
    val gift: String,
    val block: Int,
    /** Si no es nulo, el ítem solo se muestra a personas con uno de estos estados civiles. */
    val showIfMaritalStatus: List<String>? = null,
) {
    fun isVisibleFor(maritalStatus: String?): Boolean =
        showIfMaritalStatus == null || (maritalStatus != null && maritalStatus in showIfMaritalStatus)
}

@Serializable
data class Ministry(val name: String, val gifts: List<String>)
