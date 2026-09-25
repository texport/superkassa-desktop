package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import io.github.texport.superkassa.testing.api.kassa.VatMode
import kz.kazakhtelecom.proto.v203.DomainTypeEnum
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainInput
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.kassa.tiyn
import kz.mybrain.superkassa.presentation.common.message.Message
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Отрасль, смешанный НДС и поиск по штрихкоду на настоящем ядре:
 * реквизиты отрасли доходят до БФД, а без них чек не уходит.
 */
class SaleDomainCoreTest {
    private val desk = CoreDesk()

    @AfterTest
    fun close() = desk.close()

    @Test
    fun `такси без номера машины и тарифа — кнопка молчит, в БФД ничего не ушло`() {
        val kassa = desk.seated()
        desk.memory.domains[kassa.kkmId] = DomainKind.Taxi.code
        val model = desk.sale()
        model.add("Поездка", "1500")

        model.issue()

        assertEquals(SaleBlock.DomainFields, model.state.value.block)
        assertTrue(desk.bfd.countedTickets().isEmpty())
    }

    @Test
    fun `такси с реквизитами — номер машины и тариф доходят до БФД`() {
        val kassa = desk.seated()
        desk.memory.domains[kassa.kkmId] = DomainKind.Taxi.code
        val model = desk.sale()
        model.add("Поездка", "1500")
        model.form.domain(DomainInput(carNumber = "123ABC02", currentFee = "150"))

        model.issue()

        assertIs<Message.Done>(desk.said, desk.saidText)
        val domain = assertNotNull(desk.bfd.countedTickets().single().domain)
        assertEquals(DomainTypeEnum.DOMAIN_TAXI, domain.type)
        assertEquals("123ABC02", domain.taxi?.car_number)
        assertEquals(FEE, domain.taxi?.current_fee.tiyn())
    }

    @Test
    fun `смешанный режим — у каждой позиции своя ставка, налога чека нет`() {
        desk.seated(VatMode.Mixed(VatGroup.VAT_16))
        val model = desk.sale()
        model.add("Хлеб", "1120")

        model.issue()

        assertIs<Message.Done>(desk.said, desk.saidText)
        val ticket = desk.bfd.countedTickets().single()
        assertTrue(ticket.taxes.isEmpty(), "налог ушёл налогом чека: ${ticket.taxes}")
        assertEquals(listOf(PERCENT_16), ticket.items.single().commodity?.taxes?.map { it.percent })
    }

    @Test
    fun `штрихкод, которого справочник не знает, — причина словами, в чек ничего не встало`() {
        desk.seated()
        val model = desk.sale()
        model.entry.typeBarcode("4870200000019")

        model.entry.search()

        assertTrue(model.state.value.basket.positions.isEmpty())
        assertNotNull(model.state.value.search.problem, "поиск кончился молча")
        assertTrue(!model.state.value.search.searching, "поиск повис")
    }

    private companion object {
        const val FEE = 15_000L
        const val PERCENT_16 = 16_000
    }
}
