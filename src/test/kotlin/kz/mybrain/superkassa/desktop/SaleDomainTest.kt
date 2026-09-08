package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.sale.DomainField
import kz.mybrain.superkassa.desktop.ui.sale.DomainInput
import kz.mybrain.superkassa.desktop.ui.sale.DomainKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Отраслевые реквизиты чека.
 *
 * Узел такой чек пропускает — отвергает его уже ОФД, когда исправлять
 * нечего. Поэтому проверка обязана быть здесь и обязана называть поле.
 */
class SaleDomainTest {

    @Test
    fun `торговля ничего не требует`() {
        assertNull(DomainInput().missing)
        assertTrue(DomainInput().complete)
    }

    @Test
    fun `услуги и гостиницы требуют номер счёта`() {
        listOf(DomainKind.Services, DomainKind.Hotels).forEach { kind ->
            val empty = DomainInput(kind = kind)
            assertEquals(DomainField.AccountNumber, empty.missing, kind.name)
            assertNull(empty.copy(accountNumber = "A-1").missing, kind.name)
        }
    }

    @Test
    fun `заправка требует номер карты`() {
        val input = DomainInput(kind = DomainKind.GasOil)
        assertEquals(DomainField.CardNumber, input.missing)
        assertNull(input.copy(cardNumber = "5555").missing)
    }

    @Test
    fun `такси требует номер машины, потом тариф`() {
        val input = DomainInput(kind = DomainKind.Taxi)
        assertEquals(DomainField.CarNumber, input.missing)
        assertEquals(DomainField.Fee, input.copy(carNumber = "777ABC").missing)
        assertNull(input.copy(carNumber = "777ABC", currentFee = "120.50").missing)
    }

    @Test
    fun `стоянка требует положительное число часов`() {
        val input = DomainInput(kind = DomainKind.Parking)
        assertNull(input.missing)
        assertEquals(DomainField.ParkingHours, input.copy(parkingHours = "0").missing)
        assertEquals(DomainField.ParkingHours, input.copy(parkingHours = "").missing)
    }

    @Test
    fun `в чек уходит ровно один подблок`() {
        val taxi = DomainInput(kind = DomainKind.Taxi, carNumber = "777", currentFee = "10").toDomain()
        assertNotNull(taxi)
        assertEquals("DOMAIN_TAXI", taxi.type)
        assertNotNull(taxi.taxi)
        assertNull(taxi.services)
        assertNull(taxi.gasOil)
        assertNull(taxi.parking)
    }

    @Test
    fun `стоянка превращается во время въезда и выезда`() {
        val parking = assertNotNull(
            DomainInput(kind = DomainKind.Parking, parkingHours = "3").toDomain()?.parking
        )
        val hours = (parking.endTimeMillis - parking.beginTimeMillis) / MILLIS_IN_HOUR
        assertEquals(3, hours)
    }

    @Test
    fun `торговля уходит без подблоков`() {
        val trading = assertNotNull(DomainInput().toDomain())
        assertEquals("DOMAIN_TRADING", trading.type)
        assertNull(trading.services)
        assertNull(trading.taxi)
    }
}

private const val MILLIS_IN_HOUR = 3_600_000L
