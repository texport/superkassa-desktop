package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsMapModel
import kz.mybrain.superkassa.desktop.ui.analytics.KkmGroup
import kz.mybrain.superkassa.desktop.ui.analytics.KkmMark
import kz.mybrain.superkassa.desktop.ui.analytics.MapSieve
import kz.mybrain.superkassa.desktop.ui.analytics.PlacedKkm
import kz.mybrain.superkassa.desktop.ui.analytics.Placement
import kz.mybrain.superkassa.desktop.ui.analytics.kkmGroups
import kz.mybrain.superkassa.desktop.ui.analytics.sieved
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Как выглядит и сколько стоит карта сети из двух тысяч касс.
 *
 * Разметку проверяет [AnalyticsMapCrowdTest], а вид — только человек:
 * на карте страны должны стоять кружки с числами, крупнее там, где касс
 * больше, красные там, где есть заблокированная, и над всем этим — итог
 * по видимому куску карты.
 *
 * Время сборки кадра пишется рядом со снимком: карта, которая думает
 * полсекунды на каждый сдвиг, на показе выглядит сломанной, и заметить
 * это надо здесь, а не на экране вице-министра.
 */
class AnalyticsMapCrowdShots {

    /** Вся сеть на карте страны: кружки с числами и итог над ними. */
    @Test
    fun `две тысячи касс по стране`() {
        val laid = spread()
        val model = Look.model()
        model.centre(laid.placed)
        shoot("an-map-crowd-country", model, laid)
    }

    /** Приближение к Алматы: кружки распались, число в каждом меньше. */
    @Test
    fun `приближение разбивает кружки`() {
        val laid = spread()
        val model = Look.model()
        model.centre(laid.placed)
        model.map.centreOn(ALMATY_LATITUDE, ALMATY_LONGITUDE, CITY_ZOOM)
        shoot("an-map-crowd-city", model, laid)
    }

    /** Отбор по заблокированным: итог в углу говорит, сколько осталось от сети. */
    @Test
    fun `отбор сужает сеть и говорит об этом`() {
        val whole = spread()
        val model = Look.model()
        model.centre(whole.placed)
        model.sieve = MapSieve(marks = setOf(KkmMark.Blocked))
        shoot("an-map-crowd-sieved", model, sieved(whole, model.sieve), whole)
    }

    /** Выбранная касса: её кружок залит, поднят и виден среди сотни соседей. */
    @Test
    fun `выбранная касса видна среди соседей`() {
        val laid = spread()
        val model = Look.model()
        model.centre(laid.placed)
        model.map.centreOn(ALMATY_LATITUDE, ALMATY_LONGITUDE, CITY_ZOOM)
        val groups = kkmGroups(laid.placed, model.map.zoom)
        val near = groups.minByOrNull { hypotenuse(it) } ?: groups.first()
        model.open(near)
        shoot("an-map-crowd-chosen", model, laid)
    }

    /**
     * Сеть показа как её отдаёт кабинет: почти одни черновики.
     *
     * На боевых данных из 3294 касс 3288 — черновики, четыре на учёте,
     * две сняты. Прежде каждый кружок такой сети был красным: черновик
     * считался неблагополучным наравне с отказом КГД.
     */
    @Test
    fun `сеть почти из одних черновиков`() {
        val laid = Placement(asKgdSees(crowd()), emptyList())
        val model = Look.model()
        model.centre(laid.placed)
        shoot("an-map-crowd-drafts", model, laid)
    }

    /** Те же кассы с состояниями учёта в той же пропорции, что в кабинете показа. */
    private fun asKgdSees(placed: List<PlacedKkm>): List<PlacedKkm> = placed.mapIndexed { at, row ->
        val status = when {
            at < ON_RECORD -> "REGISTERED"
            at < ON_RECORD + STRUCK -> "DEREGISTERED"
            else -> "DRAFT"
        }
        row.copy(kkm = row.kkm.copy(status = status))
    }

    /**
     * Снимок и замер.
     *
     * Первый кадр считается отдельно от установившихся: в нём собирается
     * весь состав раздела, а владелец после этого видит кадры сдвига,
     * и медленным для него будет именно установившийся.
     */
    private fun shoot(name: String, model: AnalyticsMapModel, laid: Placement, whole: Placement = laid) {
        val groups = kkmGroups(laid.placed, model.map.zoom)
        RenderProbe(WIDE, HIGH) {
            MapLook(model, laid, groups, crowdView(whole.placed), whole.placed.size)
        }.use { probe ->
            val first = measureTimeMillis { repeat(SETTLE) { probe.frame() } }
            val steady = measureTimeMillis { repeat(FRAMES) { probe.frame() } } / FRAMES
            Look.shot(name, probe.frame())
            File("/tmp/$name.txt").writeText(report(name, laid, groups, first, steady))
            assertTrue(steady < SLOW, "кадр карты с ${laid.placed.size} кассами собирается $steady мс")
        }
    }

    private fun report(name: String, laid: Placement, groups: List<KkmGroup>, first: Long, steady: Long): String =
        listOf(
            "снимок: $name",
            "касс на карте: ${laid.placed.size}",
            "кружков: ${groups.size}, в самом крупном: ${groups.biggest()}",
            "установка ($SETTLE кадров): $first мс",
            "установившийся кадр: $steady мс"
        ).joinToString("\n", postfix = "\n")

    /** Насколько место далеко от Алматы: по этому выбирается касса в середине окна. */
    private fun hypotenuse(group: KkmGroup): Double {
        val down = group.latitude - ALMATY_LATITUDE
        val across = group.longitude - ALMATY_LONGITUDE
        return down * down + across * across
    }

    private fun spread(): Placement = Placement(crowd(), emptyList())

    private companion object {
        /** Сколько касс сети стоит на учёте и сколько снято — как в кабинете показа. */
        const val ON_RECORD = 4
        const val STRUCK = 2

        const val ALMATY_LATITUDE = 43.238949
        const val ALMATY_LONGITUDE = 76.889709
        const val CITY_ZOOM = 9

        /** Кадров на установку: полотно называет свой размер после первого. */
        const val SETTLE = 24

        /** По скольким кадрам считается установившееся время. */
        const val FRAMES = 20

        /** Кадр дольше этого владелец читает как задержку. */
        const val SLOW = 120

        const val WIDE = 1180
        const val HIGH = 820
    }
}
