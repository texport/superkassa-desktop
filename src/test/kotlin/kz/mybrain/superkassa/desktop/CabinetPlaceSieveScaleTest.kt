package kz.mybrain.superkassa.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceOrder
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceSieve
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceRow
import kz.mybrain.superkassa.desktop.ui.cabinet.placeRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Отбор и порядок на хозяйстве владельца: две тысячи точек, пять тысяч касс.
 *
 * Состав взят с показа, а не придуман: 2004 точки, 4971 касса, из них
 * шесть на учёте, две снятые с учёта, остальные — черновики. Проверять
 * это на самом кабинете нельзя — там боевые данные владельца, поэтому
 * список собирается здесь и рисуется сценой без окна.
 *
 * Мерится не красота картинки, а два свойства: отбор и порядок считаются
 * разом, а не на каждый кадр, и сборка колонки не растёт с длиной списка.
 */
class CabinetPlaceSieveScaleTest {

    private val places = (1..PLACES).map {
        RetailPlace(
            id = "p$it",
            name = "Торговая точка $it",
            address = "Алматы, проспект Абая $it",
            addressKz = "Алматы, Абай даңғылы $it",
            cashRegisterCount = (it % PER_PLACE + 1).toLong()
        )
    }

    /**
     * Кассы так, как их отдаёт кабинет владельца: почти все черновики.
     *
     * Это и есть трудный случай: отбор по учёту обязан найти шесть касс
     * на учёте среди пяти тысяч, а не сдаться на однородном списке.
     */
    private val registers = (1..REGISTERS).map {
        CabinetRegister(
            id = "r$it",
            kkmId = it,
            internalName = "Касса $it",
            status = when {
                it <= ON_RECORD -> "REGISTERED"
                it <= ON_RECORD + DEREGISTERED -> "DEREGISTERED"
                else -> "DRAFT"
            },
            registrationNumber = "%012d".format(it.toLong()),
            retailPlaceId = "p${it % PLACES + 1}"
        )
    }

    @Composable
    private fun Column(sieve: PlaceSieve) {
        PlacesLook(
            places = places,
            registers = registers,
            open = "p1",
            sieve = sieve
        )
    }

    /** Сколько миллисекунд занимает собрать строки: отбор, порядок и всё вместе. */
    private fun rowsMillis(sieve: PlaceSieve): Long {
        repeat(WARMUP) { placeRows(places, registers, "p1", sieve) }
        val started = System.nanoTime()
        repeat(RUNS) { placeRows(places, registers, "p1", sieve) }
        return (System.nanoTime() - started) / MILLION / RUNS
    }

    @Test
    fun `отбор и порядок на пяти тысячах касс считаются за единицы миллисекунд`() {
        val plain = rowsMillis(PlaceSieve())
        val record = rowsMillis(PlaceSieve(record = KkmRecord.OnRecord))
        val byRecord = rowsMillis(PlaceSieve(order = PlaceOrder.Record, descending = true))
        val everything = rowsMillis(
            PlaceSieve(needle = "точка 1", record = KkmRecord.Entered, order = PlaceOrder.Address)
        )
        println(
            "строки $PLACES точек и $REGISTERS касс: без отбора $plain мс, по учёту $record мс, " +
                "порядок по состоянию $byRecord мс, всё вместе $everything мс"
        )
        listOf(plain, record, byRecord, everything).forEach {
            assertTrue(it < ROWS_BUDGET, "сборка строк заняла $it мс")
        }
    }

    @Test
    fun `колонка с отбором рисуется быстро и не зависит от длины списка`() {
        renderMillis { Column(PlaceSieve()) }
        val plain = renderMillis { Column(PlaceSieve()) }
        val sieved = renderMillis { Column(PlaceSieve(record = KkmRecord.OnRecord)) }
        val ordered = renderMillis { Column(PlaceSieve(order = PlaceOrder.Record)) }
        println("колонка: без отбора $plain мс, по учёту $sieved мс, порядок по состоянию $ordered мс")
        listOf(plain, sieved, ordered).forEach {
            assertTrue(it < RENDER_BUDGET, "колонка рисуется $it мс")
        }
    }

    /** Отбор по учёту находит шесть касс на учёте среди пяти тысяч черновиков. */
    @Test
    fun `отбор находит единицы на учёте среди пяти тысяч`() {
        val found = placeRows(places, registers, null, PlaceSieve(record = KkmRecord.OnRecord))
        assertEquals(ON_RECORD, found.count { it is PlaceRow.Point }, "точек с кассой на учёте не столько")
        val struck = placeRows(places, registers, null, PlaceSieve(record = KkmRecord.Deregistered))
        assertEquals(DEREGISTERED, struck.count { it is PlaceRow.Point }, "точек со снятой кассой не столько")
    }

    /** Список остаётся ленивым: отобранное прокручивается, а не собирается целиком. */
    @Test
    fun `отобранная колонка прокручивается`() {
        RenderProbe(WINDOW_WIDE, WINDOW_HIGH) { Column(PlaceSieve(record = KkmRecord.Entered)) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val before = probe.frame()
            probe.wheel(at = Offset(WHEEL_X, WHEEL_Y), ticks = WHEEL_TICKS)
            assertTrue(probe.changedFrom(before), "картинка отобранного списка не изменилась после прокрутки")
        }
    }

    private companion object {
        /** Столько точек и касс у владельца показа: измерено запросами к кабинету. */
        const val PLACES = 2004
        const val REGISTERS = 4971

        /** Столько касс у него на учёте и столько снято с учёта. */
        const val ON_RECORD = 6
        const val DEREGISTERED = 2

        /** По сколько касс приходится на точку в разбросе. */
        const val PER_PLACE = 4

        const val WARMUP = 3
        const val RUNS = 5
        const val MILLION = 1_000_000

        /** Сколько отводится на сборку строк и на сборку колонки целиком. */
        const val ROWS_BUDGET = 150L
        const val RENDER_BUDGET = 1500L

        const val WINDOW_WIDE = 1000
        const val WINDOW_HIGH = 700
        const val SETTLE = 20
        const val WHEEL_X = 200f
        const val WHEEL_Y = 400f
        const val WHEEL_TICKS = 6f
    }
}
