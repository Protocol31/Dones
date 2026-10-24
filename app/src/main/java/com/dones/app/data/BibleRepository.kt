package com.dones.app.data

import android.content.Context
import com.dones.core.BibleRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Verse(val number: Int, val text: String)

/**
 * Textos de la Biblia Reina-Valera 1960. Se descargan una vez por capítulo
 * (bolls.life, gratuito y sin clave) y quedan guardados en el teléfono,
 * así que después se leen sin internet.
 */
class BibleRepository(context: Context) {
    private val dir = File(context.filesDir, "biblia").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    /** Los versículos de la cita, en orden. Lanza una excepción si no hay internet y no estaba guardado. */
    suspend fun load(ref: BibleRef): List<Verse> = withContext(Dispatchers.IO) {
        chapter(ref.book.number, ref.chapter).filter { ref.includes(it.number) }
    }

    private fun chapter(book: Int, chapter: Int): List<Verse> {
        val cache = File(dir, "RV1960-$book-$chapter.json")
        if (cache.exists()) {
            runCatching { return parse(cache.readText()) }
        }
        val body = download(book, chapter)
        val verses = parse(body)
        // Se guarda tal como llegó, para leerlo después sin internet.
        val tmp = File(dir, cache.name + ".tmp")
        tmp.writeText(body)
        tmp.renameTo(cache)
        return verses
    }

    private fun download(book: Int, chapter: Int): String {
        val conn = URL("https://bolls.life/get-text/RV1960/$book/$chapter/").openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        try {
            if (conn.responseCode != 200) error("Respuesta ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(body: String): List<Verse> {
        val verses = json.parseToJsonElement(body).jsonArray.map { el ->
            val o = el.jsonObject
            Verse(o.getValue("verse").jsonPrimitive.int, clean(o.getValue("text").jsonPrimitive.content))
        }
        if (verses.isEmpty()) error("Capítulo vacío")
        return verses.sortedBy { it.number }
    }

    /** Quita etiquetas HTML y números de Strong que a veces vienen en el texto. */
    private fun clean(text: String): String = text
        .replace(Regex("<S>.*?</S>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
