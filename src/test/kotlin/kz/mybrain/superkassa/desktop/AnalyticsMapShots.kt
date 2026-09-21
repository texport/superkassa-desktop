package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsMapModel
import kz.mybrain.superkassa.desktop.ui.analytics.KkmGroup
import kz.mybrain.superkassa.desktop.ui.analytics.Placement
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
    fun `ни одной кассы`() = look("an-map-no-kkm", Look.view(emptyList()), emptyMap())

    /** Все кассы без координат: карта пуста, а список полон причин. */
    @Test
    fun `все кассы без координат`() =
        look("an-map-all-without-point", Look.view(emptyList(), (1..4).map { Look.kkm(it) }), emptyMap())

    /** Часть без координат: рядом с картой список с причинами. */
    @Test
    fun `часть касс без координат`() {
        val placed = (1..2).map { Look.kkm(it, address = HERE) }
        val without = (3..5).map { Look.kkm(it, address = null) }
        look("an-map-some-without-point", Look.view(placed, without), mapOf(HERE to POINT))
    }

    /** Адрес ещё ищется: строка ожидания, а не отказа. */
    @Test
    fun `адрес ещё ищется`() {
        val kkms = (1..3).map { Look.kkm(it, address = "г. Алматы, ул. Сатпаева, $it") }
        look("an-map-searching", Look.view(kkms), kkms.associate { it.address.orEmpty() to null })
    }

    /** Одна касса: ярлычок без числа и сразу её карточка. */
    @Test
    fun `одна касса`() {
        val kkm = Look.kkm(1, address = HERE)
        look("an-map-one-kkm", Look.view(listOf(kkm)), mapOf(HERE to POINT))
    }

    /** Три кассы в одном доме: ярлычок с числом и список места под картой. */
    @Test
    fun `три кассы в одном доме`() {
        val kkms = (1..3).map { Look.kkm(it, address = HERE) }
        look("an-map-three-in-house", Look.view(kkms), mapOf(HERE to POINT)) { model, groups ->
            model.spot = groups.first().id
        }
    }

    /** Заблокированная касса среди трёх красит ярлычок целиком. */
    @Test
    fun `в доме есть заблокированная касса`() {
        val kkms = listOf(
            Look.kkm(1, address = HERE),
            Look.kkm(2, address = HERE),
            Look.kkm(3, address = HERE, blocked = true)
        )
        look("an-map-blocked-in-house", Look.view(kkms), mapOf(HERE to POINT)) { model, groups ->
            model.spot = groups.first().id
        }
    }

    /** Сотня касс по стране: ярлычки не должны слипаться в кашу. */
    @Test
    fun `сотня касс по стране`() {
        val kkms = (1..HUNDRED).map { Look.kkm(it, place = "Магазин $it", placeId = "p-$it", address = "дом $it") }
        val found = kkms.associate { it.address.orEmpty() to scattered(it.kkmId) }
        look("an-map-hundred", Look.view(kkms), found)
    }

    /** Узкое окно: ни один ряд не должен обрезаться. */
    @Test
    fun `узкое окно`() {
        val kkms = (1..3).map { Look.kkm(it, address = HERE) }
        look("an-map-narrow", Look.view(kkms), mapOf(HERE to POINT), NARROW, TALL) { model, groups ->
            model.spot = groups.first().id
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
        width: Int = WIDE,
        height: Int = HIGH,
        settle: (AnalyticsMapModel, List<KkmGroup>) -> Unit = { _, _ -> }
    ) {
        val model = Look.model()
        val laid: Placement = laidOut(view, found)
        // Карта ведётся к кассам так же, как в разделе, — и только после
        // этого считаются ярлычки: клетка места зависит от увеличения.
        model.centre(laid.placed)
        val groups = groupsOf(laid, model.map.zoom)
        settle(model, groups)
        RenderProbe(width, height) { MapLook(model, laid, groups, view) }
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
        val POINT = Look.LATITUDE to Look.LONGITUDE

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
