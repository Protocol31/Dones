package com.dones.core

/** Un libro de la Biblia: número (1 a 66), nombre en español y código USFM (para bible.com). */
data class BibleBook(val number: Int, val name: String, val usfm: String, val aliases: List<String>)

/**
 * Una cita como "1 Co 12:10, 28", "Hch 8:5-6, 26-40", "He 11" o "Job 10:8a".
 * [ranges] vacío = el capítulo completo. Las letras a/b de medio versículo se ignoran.
 */
data class BibleRef(val book: BibleBook, val chapter: Int, val ranges: List<IntRange>) {
    val isWholeChapter: Boolean get() = ranges.isEmpty()

    fun includes(verse: Int): Boolean = ranges.isEmpty() || ranges.any { verse in it }

    /** "1 Corintios 12:10, 28" */
    val title: String
        get() = if (ranges.isEmpty()) "${book.name} $chapter"
        else "${book.name} $chapter:" + ranges.joinToString(", ") { if (it.first == it.last) "${it.first}" else "${it.first}-${it.last}" }

    /** Enlace a la RVR 1960 en bible.com (versión 149), por si no hay internet para cargarla aquí. */
    val bibleComUrl: String
        get() = "https://www.bible.com/bible/149/${book.usfm}.$chapter" +
            (ranges.firstOrNull()?.let { r -> if (r.first == r.last) ".${r.first}" else ".${r.first}-${r.last}" } ?: "") +
            ".RVR1960"
}

object Bible {
    val books: List<BibleBook> = listOf(
        b(1, "Génesis", "GEN", "gn", "gen", "génesis"), b(2, "Éxodo", "EXO", "ex", "éx", "éxodo"),
        b(3, "Levítico", "LEV", "lv", "lev", "levítico"), b(4, "Números", "NUM", "nm", "núm", "números"),
        b(5, "Deuteronomio", "DEU", "dt", "deut", "deuteronomio"), b(6, "Josué", "JOS", "jos", "josué"),
        b(7, "Jueces", "JDG", "jue", "jueces"), b(8, "Rut", "RUT", "rt", "rut"),
        b(9, "1 Samuel", "1SA", "1 s", "1 sam", "1 samuel"), b(10, "2 Samuel", "2SA", "2 s", "2 sam", "2 samuel"),
        b(11, "1 Reyes", "1KI", "1 r", "1 re", "1 reyes"), b(12, "2 Reyes", "2KI", "2 r", "2 re", "2 reyes"),
        b(13, "1 Crónicas", "1CH", "1 cr", "1 crón", "1 crónicas"), b(14, "2 Crónicas", "2CH", "2 cr", "2 crón", "2 crónicas"),
        b(15, "Esdras", "EZR", "esd", "esdras"), b(16, "Nehemías", "NEH", "neh", "nehemías"),
        b(17, "Ester", "EST", "est", "ester"), b(18, "Job", "JOB", "job"),
        b(19, "Salmos", "PSA", "sal", "salmo", "salmos"), b(20, "Proverbios", "PRO", "pr", "prov", "proverbios"),
        b(21, "Eclesiastés", "ECC", "ec", "ecl", "eclesiastés"), b(22, "Cantares", "SNG", "cnt", "cant", "cantares"),
        b(23, "Isaías", "ISA", "is", "isa", "isaías"), b(24, "Jeremías", "JER", "jer", "jeremías"),
        b(25, "Lamentaciones", "LAM", "lm", "lam", "lamentaciones"), b(26, "Ezequiel", "EZK", "ez", "ezeq", "ezequiel"),
        b(27, "Daniel", "DAN", "dn", "dan", "daniel"), b(28, "Oseas", "HOS", "os", "oseas"),
        b(29, "Joel", "JOL", "jl", "joel"), b(30, "Amós", "AMO", "am", "amós"),
        b(31, "Abdías", "OBA", "abd", "abdías"), b(32, "Jonás", "JON", "jon", "jonás"),
        b(33, "Miqueas", "MIC", "mi", "miq", "miqueas"), b(34, "Nahúm", "NAM", "nah", "nahúm"),
        b(35, "Habacuc", "HAB", "hab", "habacuc"), b(36, "Sofonías", "ZEP", "sof", "sofonías"),
        b(37, "Hageo", "HAG", "hag", "hageo"), b(38, "Zacarías", "ZEC", "zac", "zacarías"),
        b(39, "Malaquías", "MAL", "mal", "malaquías"),
        b(40, "Mateo", "MAT", "mt", "mat", "mateo"), b(41, "Marcos", "MRK", "mr", "mc", "marcos"),
        b(42, "Lucas", "LUK", "lc", "luc", "lucas"), b(43, "Juan", "JHN", "jn", "juan"),
        b(44, "Hechos", "ACT", "hch", "hech", "hechos"), b(45, "Romanos", "ROM", "ro", "rom", "romanos"),
        b(46, "1 Corintios", "1CO", "1 co", "1 cor", "1 corintios"), b(47, "2 Corintios", "2CO", "2 co", "2 cor", "2 corintios"),
        b(48, "Gálatas", "GAL", "gá", "ga", "gál", "gálatas"), b(49, "Efesios", "EPH", "ef", "efesios"),
        b(50, "Filipenses", "PHP", "fil", "flp", "filipenses"), b(51, "Colosenses", "COL", "col", "colosenses"),
        b(52, "1 Tesalonicenses", "1TH", "1 ts", "1 tes", "1 tesalonicenses"), b(53, "2 Tesalonicenses", "2TH", "2 ts", "2 tes", "2 tesalonicenses"),
        b(54, "1 Timoteo", "1TI", "1 ti", "1 tim", "1 timoteo"), b(55, "2 Timoteo", "2TI", "2 ti", "2 tim", "2 timoteo"),
        b(56, "Tito", "TIT", "tit", "tito"), b(57, "Filemón", "PHM", "flm", "filemón"),
        b(58, "Hebreos", "HEB", "he", "heb", "hebreos"), b(59, "Santiago", "JAS", "stg", "sant", "santiago"),
        b(60, "1 Pedro", "1PE", "1 p", "1 pe", "1 ped", "1 pedro"), b(61, "2 Pedro", "2PE", "2 p", "2 pe", "2 ped", "2 pedro"),
        b(62, "1 Juan", "1JN", "1 jn", "1 juan"), b(63, "2 Juan", "2JN", "2 jn", "2 juan"),
        b(64, "3 Juan", "3JN", "3 jn", "3 juan"), b(65, "Judas", "JUD", "jud", "judas"),
        b(66, "Apocalipsis", "REV", "ap", "apoc", "apocalipsis"),
    )

    private fun b(n: Int, name: String, usfm: String, vararg aliases: String) = BibleBook(n, name, usfm, aliases.toList())

    private val byAlias: Map<String, BibleBook> =
        books.flatMap { book -> (book.aliases + book.name.lowercase()).map { it to book } }.toMap()

    private val refPattern = Regex("""^\s*(.+?)\s+(\d+)(?::\s*(.+))?\s*$""")

    /** Lee una cita en español. Devuelve null si no la entiende. */
    fun parse(text: String): BibleRef? {
        val m = refPattern.matchEntire(text) ?: return null
        val bookKey = m.groupValues[1].lowercase().replace(".", "").replace(Regex("\\s+"), " ").trim()
        val book = byAlias[bookKey] ?: return null
        val chapter = m.groupValues[2].toIntOrNull() ?: return null
        val versesText = m.groupValues[3]
        if (versesText.isBlank()) return BibleRef(book, chapter, emptyList())
        val ranges = versesText.split(",").map { part ->
            val nums = part.split("-", "–").map { it.trim().trimEnd('a', 'b', 'c').toIntOrNull() ?: return null }
            when (nums.size) {
                1 -> nums[0]..nums[0]
                2 -> nums[0]..nums[1]
                else -> return null
            }
        }
        return BibleRef(book, chapter, ranges)
    }

    /** Aviso de derechos que pide la Sociedad Bíblica para citar la RVR 1960. */
    const val RVR1960_NOTICE = "Texto bíblico: Reina-Valera 1960 © Sociedades Bíblicas en América Latina, 1960. " +
        "Renovado © Sociedades Bíblicas Unidas, 1988. Utilizado con permiso."
}
