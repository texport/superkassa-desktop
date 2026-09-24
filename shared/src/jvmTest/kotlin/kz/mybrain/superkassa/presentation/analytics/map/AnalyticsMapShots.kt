package kz.mybrain.superkassa.presentation.analytics.map

import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.Placement
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kotlin.test.Test

/**
 * Снимки карты касс во всех отказных состояниях.
 *
 * Разметку проверяют проверки разметки, а вид — только человек: на месте
 * карты без единой точки должно стоять объяснение, причина «ещё ищем»
 * не должна быть красной, а ярлычок трёх касс — показывать число
 * и цвет худшей из них.
 */
class AnalyticsMapShots {

    /** Ни одной кассы: и карта, и список рядом объясняют пустоту. */
    @Test
    fun `ни одной кассы`() = look("an-map-no-kkm", AnalyticsLook.view(emptyList()), emptyMap())

    /** Все кассы без координат: карта пуста, а список полон причин. */
    @Test
    fun `все кассы без координат`() {
        val nowhere = AnalyticsLook.view(emptyList(), (1..4).map { AnalyticsLook.kkm(it) })
        look("an-map-all-without-point", nowhere, emptyMap())
    }

    /** Часть без координат: рядом с картой список с причинами. */
    @Test
    fun `часть касс без координат`() {
        val placed = (1..2).map { AnalyticsLook.kkm(it, address = HERE) }
        val without = (3..5).map { AnalyticsLook.kkm(it, address = null) }
        look("an-map-some-without-point", AnalyticsLook.view(placed, without), mapOf(HERE to POINT))
    }

    /** Адрес ещё ищется: строка ожидания, а не отказа. */
    @Test
    fun `адрес ещё ищется`() {
        val kkms = (1..3).map { AnalyticsLook.kkm(it, address = "г. Алматы, ул. Сатпаева, $it") }
        look("an-map-searching", AnalyticsLook.view(kkms), kkms.associate { it.address.orEmpty() to null })
    }

    /** Одна касса: ярлычок без числа и сразу её карточка. */
    @Test
    fun `одна касса`() {
        val kkm = AnalyticsLook.kkm(1, address = HERE)
        look("an-map-one-kkm", AnalyticsLook.view(listOf(kkm)), mapOf(HERE to POINT))
    }

    /** Три кассы в одном доме: ярлычок с числом и список места под картой. */
    @Test
    fun `три кассы в одном доме`() {
        val kkms = (1..3).map { AnalyticsLook.kkm(it, address = HERE) }
        look("an-map-three-in-house", AnalyticsLook.view(kkms), mapOf(HERE to POINT)) { model, groups ->
            model.copy(spot = groups.first().id)
        }
    }

    /** Заблокированная касса среди трёх красит ярлычок целиком. */
    @Test
    fun `в доме есть заблокированная касса`() {
        val kkms = listOf(
            AnalyticsLook.kkm(1, address = HERE),
            AnalyticsLook.kkm(2, address = HERE),
            AnalyticsLook.kkm(3, address = HERE, blocked = true)
        )
        look("an-map-blocked-in-house", AnalyticsLook.view(kkms), mapOf(HERE to POINT)) { model, groups ->
            model.copy(spot = groups.first().id)
        }
    }

    /** Сотня касс по стране: ярлычки не должны слипаться в кашу. */
    @Test
    fun `сотня касс по стране`() {
        val kkms = (1..HUNDRED).map {
            AnalyticsLook.kkm(it, place = "Магазин $it", placeId = "p-$it", address = "дом $it")
        }
        val found = kkms.associate { it.address.orEmpty() to scattered(it.kkmId) }
        look("an-map-hundred", AnalyticsLook.view(kkms), found)
    }

    /** Узкое окно: ни один ряд не должен обрезаться. */
    @Test
    fun `узкое окно`() {
        val kkms = (1..3).map { AnalyticsLook.kkm(it, address = HERE) }
        look("an-map-narrow", AnalyticsLook.view(kkms), mapOf(HERE to POINT), NARROW to TALL) { model, groups ->
            model.copy(spot = groups.first().id)
        }
    }

    /**
     * Снимок раздела в заданном состоянии.
     *
     * Состояние доводится до нужного через сам показ: ярлычок раскрывают
     * так же, как это делает нажатие владельца, — иначе снимок показывал
     * бы не то, до чего он доходит руками.
     */
    private fun look(
        name: String,
        view: KkmMapView,
        found: Map<String, Pair<Double, Double>?>,
        window: Pair<Int, Int> = WIDE to HIGH,
        settle: (AnalyticsMapUiState, List<KkmGroup>) -> AnalyticsMapUiState = { model, _ -> model }
    ) {
        val start = AnalyticsLook.model()
        val laid: Placement = laidOut(view, found)
        // Карта ведётся к кассам так же, как в разделе, — и только после
        // этого считаются ярлычки: клетка места зависит от увеличения.
        AnalyticsLook.centre(start, laid.placed)
        val groups = groupsOf(laid, start.map.zoom)
        val model = settle(start, groups)
        RenderProbe(window.first, window.second) { MapLook(model, laid, groups, view) }
            .use { probe ->
                // Ярлычки встают только со второго кадра: своё окно карта
                // называет после первой отрисовки, а до этого его размер нулевой.
                repeat(SETTLE) { probe.frame() }
                Look.shot(name, probe.frame())
            }
    }

    /** Кассы по всей стране: сетка от Актобе до Алматы, без двух в одной клетке. */
    private fun scattered(seed: Int): Pair<Double, Double> =
        (43.0 + (seed % TEN) * STEP) to (68.0 + (seed / TEN % TEN) * STEP)

    private companion object {
        const val HERE = "г. Алматы, пр. Абая, 10"
        val POINT = AnalyticsLook.LATITUDE to AnalyticsLook.LONGITUDE

        const val HUNDRED = 100
        const val TEN = 10
        const val STEP = 0.9

        /** Кадров на установку: полотно называет свой размер после первого. */
        const val SETTLE = 24

        const val WIDE = 1180
        const val HIGH = 820
        const val NARROW = 780
        const val TALL = 620
    }
}
