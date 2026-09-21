package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.ui.analytics.KkmMark
import kz.mybrain.superkassa.desktop.ui.analytics.MapSieve
import kz.mybrain.superkassa.desktop.ui.analytics.sieved
import kotlin.test.Test

/**
 * Снимки отбора над картой.
 *
 * Отбор сужает и карту, и список рядом, и на снимке проверяется именно
 * это согласие: если под отбор не подошла ни одна касса, на месте карты
 * обязано стоять объяснение про отбор, а не про источник положения.
 */
class AnalyticsSieveShots {

    /** Ничего не подошло: объяснение должно говорить про отбор. */
    @Test
    fun `под отбор не подошла ни одна касса`() =
        look("an-sieve-nothing", MapSieve(needle = "ничего такого нет"))

    /** Поиск по номеру КГД: владелец помнит кассу по заявлению. */
    @Test
    fun `поиск по номеру КГД`() = look("an-sieve-kgd", MapSieve(needle = "004500002"))

    /** Отбор по торговой точке: на карте остаётся одна точка. */
    @Test
    fun `отбор по торговой точке`() = look("an-sieve-place", MapSieve(place = "p-2"))

    /** Все плашки нажаты разом: ряд не должен разъехаться, а карта — обмануть. */
    @Test
    fun `все плашки нажаты разом`() =
        look("an-sieve-all-marks", MapSieve(needle = "Касса", place = "p-1", marks = KkmMark.entries.toSet()))

    /** Сброс отбора: на карте снова всё хозяйство. */
    @Test
    fun `отбор сброшен`() = look("an-sieve-cleared", MapSieve())

    /**
     * Снимок раздела с заданным отбором.
     *
     * Плашки торговых точек считаются по всему ответу кабинета, а не
     * по отобранному: иначе нажатая точка исчезала бы из своего же списка.
     */
    private fun look(name: String, sieve: MapSieve) {
        val view = Look.view(fleet(), listOf(Look.kkm(9, placeId = "p-3", place = "Склад", address = null)))
        val model = Look.model()
        model.sieve = sieve
        val whole = laidOut(view, points())
        model.centre(whole.placed)
        val laid = sieved(whole, sieve)
        val groups = groupsOf(laid, model.map.zoom)
        RenderProbe(WIDE, HIGH) { MapLook(model, laid, groups, view) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot(name, probe.frame())
            }
    }

    /** Хозяйство из трёх точек: по нему видно, что отбор оставил. */
    private fun fleet(): List<AnalyticsKkm> = listOf(
        Look.kkm(1, place = "Магазин на Абая", placeId = "p-1", address = ABAI),
        Look.kkm(2, place = "Магазин на Абая", placeId = "p-1", address = ABAI, shiftOpen = true),
        Look.kkm(3, place = "Магазин на Сатпаева", placeId = "p-2", address = SATPAEV),
        Look.kkm(4, place = "Магазин на Сатпаева", placeId = "p-2", address = SATPAEV, blocked = true),
        Look.kkm(5, place = "Павильон у рынка", placeId = "p-3", address = MARKET, status = "DEREGISTERED")
    )

    private fun points(): Map<String, Pair<Double, Double>?> = mapOf(
        ABAI to (Look.LATITUDE to Look.LONGITUDE),
        SATPAEV to (Look.LATITUDE + Look.APART to Look.LONGITUDE + Look.APART),
        MARKET to (Look.LATITUDE - Look.APART to Look.LONGITUDE + Look.APART)
    )

    private companion object {
        const val ABAI = "г. Алматы, пр. Абая, 10"
        const val SATPAEV = "г. Алматы, ул. Сатпаева, 22"
        const val MARKET = "г. Алматы, ул. Жандосова, 4"

        const val SETTLE = 24
        const val WIDE = 1180
        const val HIGH = 820
    }
}
