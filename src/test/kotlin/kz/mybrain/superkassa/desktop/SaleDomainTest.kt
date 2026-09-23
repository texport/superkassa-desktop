package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.DomainParking
import kz.mybrain.superkassa.desktop.server.ReceiptDomain
import kz.mybrain.superkassa.desktop.ui.sale.DomainField
import kz.mybrain.superkassa.desktop.ui.sale.DomainInput
import kz.mybrain.superkassa.desktop.ui.sale.DomainKind
import kz.mybrain.superkassa.desktop.ui.sale.SaleBlock
import kz.mybrain.superkassa.desktop.ui.sale.SaleState
import kz.mybrain.superkassa.desktop.ui.sale.blockOf
import kz.mybrain.superkassa.desktop.ui.strings.paymentTextsRu
import kz.mybrain.superkassa.desktop.ui.strings.saleTextsRu
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Отраслевые реквизиты чека.
 *
 * Узел такой чек пропускает — отвергает его уже БФД, когда исправлять
 * нечего. Поэтому проверка обязана быть здесь, обязана называть поле
 * и обязана спрашивать только то, что нужно отрасли этой кассы.
 */
class SaleDomainTest {

    @Test
    fun `у кассы в торговле отраслевых полей нет вовсе`() {
        assertTrue(DomainKind.Trading.fields.isEmpty(), "кассир магазина не заполняет ничего")
        assertTrue(DomainInput().complete(DomainKind.Trading))
        assertNull(DomainInput().missing(DomainKind.Trading))
    }

    @Test
    fun `услуги и гостиницы требуют лицевой счёт`() {
        listOf(DomainKind.Services, DomainKind.Hotels).forEach { kind ->
            assertEquals(listOf(DomainField.AccountNumber), kind.fields, kind.name)
            assertEquals(DomainField.AccountNumber, DomainInput().missing(kind), kind.name)
            assertNull(DomainInput(accountNumber = "A-1").missing(kind), kind.name)
        }
    }

    @Test
    fun `нефтепродукты требуют номер карты`() {
        assertEquals(DomainField.CardNumber, DomainInput().missing(DomainKind.GasOil))
        assertNull(DomainInput(cardNumber = "5555").missing(DomainKind.GasOil))
    }

    @Test
    fun `такси требует номер машины, потом тариф`() {
        val taxi = DomainKind.Taxi
        assertEquals(DomainField.CarNumber, DomainInput().missing(taxi))
        assertEquals(DomainField.Fee, DomainInput(carNumber = "777ABC").missing(taxi))
        assertEquals(
            DomainField.Fee,
            DomainInput(carNumber = "777ABC", currentFee = "Городской").missing(taxi),
            "тариф словом — это не тариф"
        )
        assertNull(DomainInput(carNumber = "777ABC", currentFee = "120.50").missing(taxi))
    }

    @Test
    fun `стоянка требует время въезда и выезда`() {
        val parking = DomainKind.Parking
        assertEquals(DomainField.ParkingFrom, DomainInput().missing(parking))
        assertEquals(DomainField.ParkingTo, DomainInput(parkingFrom = "8:15").missing(parking))
        assertEquals(
            DomainField.ParkingTo,
            DomainInput(parkingFrom = "8:15", parkingTo = "четверть одиннадцатого").missing(parking),
            "время словами — это не время"
        )
        assertNull(DomainInput(parkingFrom = "8:15", parkingTo = "10:15").missing(parking))
    }

    /**
     * Двух подблоков разом протокол не допускает: `Domain` несёт вид
     * отрасли и ровно один подблок под него.
     */
    @Test
    fun `в чек уходит вид отрасли и ровно один подблок`() {
        DomainKind.entries.forEach { kind ->
            val domain = FILLED.toDomain(kind)
            assertEquals(kind.code, domain.type, kind.name)
            val blocks = subBlocks(domain)
            assertTrue(blocks <= 1, "${kind.name}: подблоков $blocks")
            if (kind == DomainKind.Trading) {
                assertEquals(0, blocks, "торговля уходит без подблоков")
            } else {
                assertEquals(1, blocks, "${kind.name}: подблока нет")
            }
        }
    }

    @Test
    fun `реквизиты такси уходят тем, чем их набрал кассир`() {
        val taxi = assertNotNull(
            DomainInput(carNumber = " 777ABC ", isOrder = true, currentFee = "350.00")
                .toDomain(DomainKind.Taxi).taxi
        )
        assertEquals("777ABC", taxi.carNumber)
        assertTrue(taxi.isOrder)
        assertEquals(0, taxi.currentFee.compareTo(BigDecimal("350.00")))
    }

    @Test
    fun `часы и минуты стоянки становятся временем въезда и выезда`() {
        val day = DomainInput(parkingFrom = "8:15", parkingTo = "10:15")
            .toDomain(DomainKind.Parking).parking
        assertEquals(2L, hours(assertNotNull(day)))
        // Стоянка через полночь: въезд с вечера, выезд утром. Иначе въезд
        // оказывался позже выезда, и такой чек БФД не примет.
        val night = DomainInput(parkingFrom = "23:30", parkingTo = "0:30")
            .toDomain(DomainKind.Parking).parking
        assertEquals(1L, hours(assertNotNull(night)))
    }

    @Test
    fun `незаполненный реквизит не даёт пробить чек и назван поимённо`() {
        val state = SaleState(missingDomainField = DomainField.CarNumber)
        assertEquals(SaleBlock.DomainFields, blockOf(state))
        val words = SaleBlock.DomainFields.reason(saleTextsRu, paymentTextsRu, DomainField.CarNumber)
        assertTrue(words.contains(saleTextsRu.carNumber), "причина не называет поле: $words")
        // Числовому полю сказано, что от него нужно число: «Заполните: Тариф»
        // над полем со словом «Городской» — загадка, а не причина.
        val fee = SaleBlock.DomainFields.reason(saleTextsRu, paymentTextsRu, DomainField.Fee)
        assertTrue(fee.contains(saleTextsRu.fee), "причина не называет тариф: $fee")
        assertTrue(fee != words)
    }

    /** Возврат несёт вид отрасли кассы, но не выдуманные реквизиты поездки. */
    @Test
    fun `возврат несёт вид отрасли без подблоков`() {
        val plain = DomainKind.Taxi.plain
        assertEquals("DOMAIN_TAXI", plain.type)
        assertEquals(0, subBlocks(plain))
    }

    private fun subBlocks(domain: ReceiptDomain): Int =
        listOfNotNull(domain.services, domain.gasOil, domain.taxi, domain.parking).size

    private fun hours(parking: DomainParking): Long =
        (parking.endTimeMillis - parking.beginTimeMillis) / MILLIS_IN_HOUR

    private companion object {
        const val MILLIS_IN_HOUR = 3_600_000L

        /** Все поля разом: подблок обязан выбираться отраслью, а не набранным. */
        val FILLED = DomainInput(
            accountNumber = "ACC-1024",
            cardNumber = "CARD-77",
            carNumber = "777ABC",
            isOrder = true,
            currentFee = "350",
            parkingFrom = "8:15",
            parkingTo = "10:15"
        )
    }
}
