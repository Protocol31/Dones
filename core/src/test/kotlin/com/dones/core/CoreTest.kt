package com.dones.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreTest {

    private fun asset(name: String): String =
        listOf("../app/src/main/assets/$name", "app/src/main/assets/$name").map(::File).first { it.exists() }.readText()

    private val content: HoutsContent = HoutsContent.parse(asset("houts-v1.json"))
    private val lesson: Lesson = Lesson.parse(asset("leccion-v1.json"))
    private fun pageOf(block: Int) = lesson.pages.indexOfFirst { it.houtsBlock == block }

    @Test
    fun `el contenido tiene 125 items, 25 dones y 5 bloques con un item de cada don`() {
        assertEquals(125, content.items.size)
        assertEquals(25, content.gifts.size)
        assertEquals(5, content.blocks.size)
        content.blocks.forEach { b ->
            assertEquals(25, b.items.map { content.item(it).gift }.toSet().size)
        }
    }

    @Test
    fun `la clave del JSON coincide con la regla (n - 1) mod 25`() {
        content.items.forEach { assertEquals(HoutsScorer.giftCodeOf(it.n), it.gift, "ítem ${it.n}") }
        assertEquals("A", HoutsScorer.giftCodeOf(101))
        assertEquals("Y", HoutsScorer.giftCodeOf(125))
    }

    @Test
    fun `las personas casadas no ven los 5 items de celibato`() {
        val casado = HoutsScorer.visibleItems(content, "casado").map { it.n }
        assertEquals(120, casado.size)
        listOf(22, 47, 72, 97, 122).forEach { assertFalse(it in casado) }
        assertEquals(125, HoutsScorer.visibleItems(content, "soltero").size)
        assertEquals(125, HoutsScorer.visibleItems(content, "viudo").size)
    }

    @Test
    fun `puntaje maximo para un don y ranking con top 3`() {
        // Todo "Nada" excepto Misericordia (J) = 3, Hospitalidad (M) = 2, Ayuda (I) = 1.
        val answers = content.items.associate { item ->
            item.n to when (item.gift) { "J" -> 3; "M" -> 2; "I" -> 1; else -> 0 }
        }
        val result = HoutsScorer.score(content, answers, "soltero")
        assertEquals(listOf("J", "M", "I"), result.dominant.map { it.gift.code })
        assertEquals(15, result.dominant[0].raw)
        assertEquals(100, result.dominant[0].percent)
        assertEquals(10, result.dominant[1].raw)
        assertTrue(result.subordinate.isEmpty()) // los demás tienen 0
        assertTrue(result.ministries.isNotEmpty())
    }

    @Test
    fun `celibato queda fuera del ranking para casados`() {
        val answers = content.items.associate { it.n to 2 }
        val result = HoutsScorer.score(content, answers, "casado")
        assertEquals(24, result.ranking.size)
        assertFalse(result.ranking.any { it.gift.code == "V" })
    }

    @Test
    fun `los empates comparten puesto`() {
        val answers = content.items.associate { item ->
            item.n to when (item.gift) { "A", "B" -> 3; "C" -> 2; "D" -> 1; else -> 0 }
        }
        val r = HoutsScorer.score(content, answers, "soltero").ranking
        assertEquals(1, r[0].rank)
        assertEquals(1, r[1].rank)
        assertEquals(3, r[2].rank)
        assertEquals("C", r[2].gift.code)
    }

    @Test
    fun `codigo de clase desde texto o QR`() {
        assertEquals("481902", JoinCode.parse("481902"))
        assertEquals("481902", JoinCode.parse(" 481 902 "))
        assertEquals("481902", JoinCode.parse("dones://clase/481902"))
        assertNull(JoinCode.parse("12345"))
        assertNull(JoinCode.parse("1234567"))
    }

    @Test
    fun `la clase simulada solo acepta 000000`() {
        val session = SimulatedClassSession()
        assertIs<JoinResult.ClassNotFound>(session.join("123456"))
        assertIs<JoinResult.InvalidCode>(session.join("abc"))
        assertIs<JoinResult.Joined>(session.join("000000"))
    }

    @Test
    fun `la leccion tiene cada bloque una vez, en orden, y un espacio por respuesta`() {
        assertEquals(content.blocks.map { it.block }, lesson.pages.mapNotNull { it.houtsBlock })
        lesson.pages.forEachIndexed { i, page ->
            assertEquals(page.blanks.size, page.text.split(Blanks.MARK).size - 1, "página $i: ${page.title}")
            assertTrue(page.pick in 0..page.choices.size, "página $i: ${page.title}")
        }
        assertTrue(pageOf(1) > 10) // primero la enseñanza, después las preguntas
    }

    @Test
    fun `completar espacios acepta sin tildes ni mayusculas`() {
        assertTrue(Blanks.isCorrect("la institución|institución", "Institucion"))
        assertTrue(Blanks.isCorrect("los que equipan|equipan", "los que EQUIPAN"))
        assertTrue(Blanks.isCorrect("Señor", "senor"))
        assertFalse(Blanks.isCorrect("servir", ""))
        assertFalse(Blanks.isCorrect("físicas", "espirituales"))
        assertEquals("físicas", Blanks.display("físicas|fisicas"))
    }

    @Test
    fun `se empieza por la ensenanza y las preguntas aparecen al llegar a su bloque`() {
        var p = ParticipantState(maritalStatus = "casado", consentAtMillis = 1)
        val first = ParticipantFlow.view(content, lesson, p)
        assertIs<ParticipantView.LessonStep>(first)
        assertEquals(0, first.index)
        assertEquals(1, first.positionInSection)

        p = p.copy(page = pageOf(1))
        val q = ParticipantFlow.view(content, lesson, p)
        assertIs<ParticipantView.Question>(q)
        assertEquals(1, q.item.n)
        assertNull(q.previousItem) // vuelve a la página anterior
        assertEquals(24, q.blockSize) // sin el ítem 22 (celibato)

        content.blocks[0].items.forEach { p = p.answer(it, 2, 3000) }
        val done = ParticipantFlow.view(content, lesson, p)
        assertIs<ParticipantView.BlockDone>(done)
        assertEquals(24, done.answered)
        assertEquals(120, done.total)

        assertIs<ParticipantView.LessonStep>(ParticipantFlow.view(content, lesson, p.copy(page = pageOf(1) + 1)))
    }

    @Test
    fun `dentro del bloque puede volver atras`() {
        var p = ParticipantState(maritalStatus = "soltero", consentAtMillis = 1, page = pageOf(1))
        p = p.answer(1, 3, 2000)
        val q2 = ParticipantFlow.view(content, lesson, p)
        assertIs<ParticipantView.Question>(q2)
        assertEquals(2, q2.item.n)
        assertEquals(1, q2.previousItem)

        val back = ParticipantFlow.view(content, lesson, p, reviewItem = 1)
        assertIs<ParticipantView.Question>(back)
        assertEquals(3, back.currentAnswer)
    }

    @Test
    fun `despues de la ultima pagina vienen los resultados`() {
        val p = ParticipantState(maritalStatus = "soltero", consentAtMillis = 1, page = lesson.pages.size)
        assertEquals(ParticipantView.Results, ParticipantFlow.view(content, lesson, p))
    }

    @Test
    fun `indicadores de calidad`() {
        val same = (1..30).map { AnswerEvent(it, 3, 3000) }
        assertTrue(QualityChecker.check(same).straightLining)
        val fast = (1..30).map { AnswerEvent(it, it % 4, 500) }
        val r = QualityChecker.check(fast)
        assertTrue(r.tooFast)
        assertFalse(r.straightLining)
    }

    @Test
    fun `el estado se guarda y se recupera`() {
        val p = ParticipantState(name = "María", maritalStatus = "casado", page = 3, notes = mapOf("1.0" to "equipan"))
            .answer(5, 2, 1000)
        assertEquals(p, ParticipantState.fromJson(p.toJson()))
    }

    @Test
    fun `codigo de certificado estable`() {
        val a = CertificateCode.generate("María González", "000000", "2026-10-17")
        assertEquals(a, CertificateCode.generate("maría gonzález", "000000", "2026-10-17"))
        assertTrue(Regex("DMM-[A-Z2-9]{4}-[A-Z2-9]{4}").matches(a))
    }
}
