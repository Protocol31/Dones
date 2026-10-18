package com.dones.app.data

import android.content.Context
import com.dones.core.HoutsContent
import com.dones.core.Lesson
import com.dones.core.ParticipantState
import com.dones.core.ProfileBook
import java.io.File
import java.util.UUID

/** Lee el cuestionario de assets y guarda los perfiles en el teléfono (nada se sube a internet). */
class Storage(private val context: Context) {

    /** Archivo de la versión anterior (una sola persona). Se pasa a perfiles la primera vez. */
    private val legacyFile = File(context.filesDir, "participante.json")
    private val profilesFile = File(context.filesDir, "perfiles.json")

    fun loadContent(): HoutsContent =
        context.assets.open("houts-v1.json").bufferedReader().use { HoutsContent.parse(it.readText()) }

    fun loadLesson(): Lesson =
        context.assets.open("leccion-v1.json").bufferedReader().use { Lesson.parse(it.readText()) }

    fun loadProfiles(): ProfileBook {
        if (profilesFile.exists()) {
            return runCatching { ProfileBook.fromJson(profilesFile.readText()) }.getOrElse { ProfileBook() }
        }
        val legacy = runCatching { ParticipantState.fromJson(legacyFile.readText()) }.getOrNull() ?: return ProfileBook()
        return ProfileBook.fromLegacy(legacy, newId()).also { saveProfiles(it); legacyFile.delete() }
    }

    fun saveProfiles(book: ProfileBook) {
        // Escribe a un archivo temporal y lo renombra, para no dejar el archivo a medias.
        val tmp = File(profilesFile.parentFile, profilesFile.name + ".tmp")
        tmp.writeText(book.toJson())
        if (!tmp.renameTo(profilesFile)) {
            profilesFile.delete()
            tmp.renameTo(profilesFile)
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    /** PDFs de cada persona en su propia carpeta. */
    fun documentsDir(profileId: String): File =
        File(context.filesDir, "documentos/${profileId.ifBlank { "sin-perfil" }}").apply { mkdirs() }
}
