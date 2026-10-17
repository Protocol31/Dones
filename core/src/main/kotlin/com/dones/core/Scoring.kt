package com.dones.core

/** Puntaje de un don (fila del cuestionario). */
data class GiftScore(
    val gift: Gift,
    /** Suma de las respuestas (0 a 3 cada una). */
    val raw: Int,
    /** Puntaje máximo posible con los ítems respondidos. */
    val max: Int,
    /** Puesto en el ranking. Dos dones empatados comparten el mismo puesto. */
    val rank: Int,
) {
    val fraction: Double get() = if (max == 0) 0.0 else raw.toDouble() / max
    val percent: Int get() = Math.round(fraction * 100).toInt()
}

data class MinistrySuggestion(val ministry: Ministry, val matchingGifts: List<Gift>)

data class HoutsResult(
    /** Todos los dones evaluados, de mayor a menor. */
    val ranking: List<GiftScore>,
    /** Puestos 1 a 3 (puede haber más de 3 si hay empates). */
    val dominant: List<GiftScore>,
    /** Puestos 4 a 6 (puede haber más de 3 si hay empates). */
    val subordinate: List<GiftScore>,
    val ministries: List<MinistrySuggestion>,
)

object HoutsScorer {

    /** Don de un ítem según la clave Wagner-Houts: fila = (n - 1) mod 25. */
    fun giftCodeOf(n: Int): String = ('A' + ((n - 1) % 25)).toString()

    /** Ítems que esta persona debe responder (los de Celibato se ocultan a las personas casadas). */
    fun visibleItems(content: HoutsContent, maritalStatus: String?): List<Item> =
        content.items.filter { it.isVisibleFor(maritalStatus) }

    fun score(content: HoutsContent, answers: Map<Int, Int>, maritalStatus: String?): HoutsResult {
        val visible = visibleItems(content, maritalStatus)
        val byGift = visible.groupBy { it.gift }

        val unranked = content.gifts
            .filter { byGift[it.code].orEmpty().isNotEmpty() } // sin ítems visibles = fuera del ranking
            .map { gift ->
                val values = byGift.getValue(gift.code).mapNotNull { answers[it.n] }
                Triple(gift, values.sum(), values.size * 3)
            }
            .sortedWith(
                compareByDescending<Triple<Gift, Int, Int>> { fraction(it.second, it.third) }
                    .thenByDescending { it.second }
                    .thenBy { it.first.code }
            )

        // Ranking de competencia: 1, 2, 2, 4… (los empates comparten puesto).
        val ranking = mutableListOf<GiftScore>()
        unranked.forEachIndexed { i, (gift, raw, max) ->
            val prev = ranking.lastOrNull()
            val rank = if (prev != null && prev.raw == raw && prev.max == max) prev.rank else i + 1
            ranking += GiftScore(gift, raw, max, rank)
        }

        val dominant = ranking.filter { it.rank <= 3 && it.raw > 0 }
        val subordinate = ranking.filter { it.rank in 4..6 && it.raw > 0 && it !in dominant }

        return HoutsResult(
            ranking = ranking,
            dominant = dominant,
            subordinate = subordinate,
            ministries = suggestMinistries(content, dominant, subordinate),
        )
    }

    /**
     * Sugiere hasta 3 ministerios: los dones dominantes suman 2 puntos y los subordinados 1.
     * El mapa dones → ministerios está en el JSON y debe validarlo el pastor.
     */
    fun suggestMinistries(
        content: HoutsContent,
        dominant: List<GiftScore>,
        subordinate: List<GiftScore>,
        limit: Int = 3,
    ): List<MinistrySuggestion> {
        val weight = buildMap {
            subordinate.forEach { put(it.gift.code, 1) }
            dominant.forEach { put(it.gift.code, 2) }
        }
        return content.ministries
            .mapIndexed { index, m -> Triple(m, m.gifts.sumOf { weight[it] ?: 0 }, index) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Triple<Ministry, Int, Int>> { it.second }.thenBy { it.third })
            .take(limit)
            .map { (m, _, _) ->
                MinistrySuggestion(m, m.gifts.filter { it in weight }.map { content.gift(it) })
            }
    }

    private fun fraction(raw: Int, max: Int) = if (max == 0) 0.0 else raw.toDouble() / max
}
