package com.dones.app.data

import android.content.Context
import com.dones.core.HoutsContent
import com.dones.core.Lesson
import com.dones.core.ParticipantState
import java.io.File

/** Lee el cuestionario de assets y guarda el estado del participante en el teléfono. */
class Storage(private val context: Context) {

    private val stateFile = File(context.filesDir, "participante.json")

    fun loadContent(): HoutsContent =
        context.assets.open("houts-v1.json").bufferedReader().use { HoutsContent.parse(it.readText()) }

    fun loadLesson(): Lesson =
        context.assets.open("leccion-v1.json").bufferedReader().use { Lesson.parse(it.readText()) }

    fun loadState(): ParticipantState =
        runCatching { ParticipantState.fromJson(stateFile.readText()) }.getOrElse { ParticipantState() }

    fun saveState(state: ParticipantState) {
        // Escribe a un archivo temporal y lo renombra, para no dejar el archivo a medias.
        val tmp = File(stateFile.parentFile, stateFile.name + ".tmp")
        tmp.writeText(state.toJson())
        tmp.renameTo(stateFile)
    }

    fun documentsDir(): File = File(context.filesDir, "documentos").apply { mkdirs() }
}
