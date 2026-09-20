package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.ExchangeAddress
import kz.mybrain.superkassa.desktop.ui.analytics.exchangeRegisters
import kz.mybrain.superkassa.desktop.ui.analytics.exchangeRows
import kz.mybrain.superkassa.desktop.ui.analytics.kkmTitle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Отбор и поиск по адресам обмена.
 *
 * Список у сети — сотни строк, и без поиска он не читается. Проверяется
 * то, из-за чего владелец не нашёл бы свою кассу: поиск идёт по всему,
 * что видно в строке, а не по одному адресу, и отбор по кассе работает
 * вместе с поиском, а не вместо него.
 */
class AnalyticsExchangeTest {

    private val rows = listOf(
        row("c1", 2000302, "KGD-2000302", "Касса у входа", "Магазин на Абая", "212.154.10.7"),
        row("c1", 2000302, "KGD-2000302", "Касса у входа", "Магазин на Абая", "95.59.40.1"),
        row("c2", 2000303, "KGD-2000303", "Касса в зале", "Склад на Сейфуллина", "212.154.10.7")
    )

    private fun row(
        id: String,
        kkmId: Int,
        registrationNumber: String,
        internalName: String,
        place: String,
        address: String
    ) = ExchangeAddress(
        cashRegisterId = id,
        kkmId = kkmId,
        registrationNumber = registrationNumber,
        internalName = internalName,
        retailPlaceName = place,
        address = address,
        firstSeen = "2026-09-01T06:00:00Z",
        lastSeen = "2026-09-19T08:14:00Z"
    )

    @Test
    fun `без поиска и отбора список доходит целиком и в прежнем порядке`() {
        val found = exchangeRows(rows, query = "", register = null)
        assertEquals(rows, found)
    }

    @Test
    fun `поиск идёт по самому адресу`() {
        val found = exchangeRows(rows, query = "95.59", register = null)
        assertEquals(listOf("95.59.40.1"), found.map { it.address })
    }

    @Test
    fun `поиск находит кассу по своему названию, номеру КГД и номеру машины`() {
        assertEquals(2, exchangeRows(rows, query = "у входа", register = null).size)
        assertEquals(1, exchangeRows(rows, query = "kgd-2000303", register = null).size)
        assertEquals(2, exchangeRows(rows, query = "2000302", register = null).size)
    }

    @Test
    fun `поиск находит кассу по торговой точке и не различает регистра`() {
        assertEquals(1, exchangeRows(rows, query = "СКЛАД", register = null).size)
        assertEquals(2, exchangeRows(rows, query = "магазин на абая", register = null).size)
    }

    @Test
    fun `отбор по кассе и поиск работают вместе`() {
        val found = exchangeRows(rows, query = "212.154", register = "c1")
        assertEquals(1, found.size)
        assertEquals("c1", found.single().cashRegisterId)
    }

    @Test
    fun `не нашлось — пустой список, а не весь список обратно`() {
        assertTrue(exchangeRows(rows, query = "10.0.0.1", register = null).isEmpty())
    }

    @Test
    fun `пробелы вокруг искомого не мешают найти`() {
        assertEquals(1, exchangeRows(rows, query = "  95.59.40.1  ", register = null).size)
    }

    @Test
    fun `в отборе каждая касса встречается один раз`() {
        val registers = exchangeRegisters(rows)
        assertEquals(listOf("c1", "c2"), registers.map { it.cashRegisterId })
        assertEquals("Касса у входа", kkmTitle(registers.first()))
    }

    @Test
    fun `касса без своего названия зовётся номером КГД, а без него — номером машины`() {
        val byNumber = rows.first().copy(internalName = null)
        assertEquals("KGD-2000302", kkmTitle(byNumber))
        assertEquals("№ 2000302", kkmTitle(byNumber.copy(registrationNumber = null)))
    }
}
