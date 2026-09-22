package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.analytics.sievePlaces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Одноимённые торговые точки различимы в отборе карты.
 *
 * Имя точки уникальным не бывает: в сети показа из 1002 точек триста
 * названий носит по нескольку магазинов, а «Канцтовары «Тұмар»» —
 * девять. Список отбора шёл по именам, и девять разных магазинов
 * стояли в нём девятью одинаковыми строками подряд.
 */
class AnalyticsSievePlacesTest {

    @Test
    fun `одноимённые точки названы вместе с адресом, а прочие — нет`() {
        val view = Look.view(
            listOf(
                Look.kkm(1, place = "Канцтовары «Тұмар»", placeId = "p1", address = "Алматы, Абая, 10"),
                Look.kkm(2, place = "Канцтовары «Тұмар»", placeId = "p2", address = "Астана, Кунаева, 12"),
                Look.kkm(3, place = "Аптека «Шапағат»", placeId = "p3", address = "Шымкент, Жетиасар, 8")
            )
        )
        val places = sievePlaces(view)

        assertEquals(3, places.size)
        val names = places.map { it.name }
        assertEquals(names.size, names.toSet().size, "две разные точки названы одинаково: $names")
        assertTrue(names.any { it.contains("Алматы, Абая, 10") }, names.toString())
        assertTrue(names.any { it.contains("Астана, Кунаева, 12") }, names.toString())
        assertTrue(
            names.contains("Аптека «Шапағат»"),
            "к единственной точке с таким именем адрес добавлять незачем: $names"
        )
    }

    /** Две кассы одной точки — по-прежнему одна строка отбора. */
    @Test
    fun `кассы одной точки не удваивают строку`() {
        val view = Look.view(
            listOf(
                Look.kkm(1, place = "Магазин на Абая", placeId = "p1"),
                Look.kkm(2, place = "Магазин на Абая", placeId = "p1")
            )
        )
        assertEquals(listOf("Магазин на Абая"), sievePlaces(view).map { it.name })
    }
}
