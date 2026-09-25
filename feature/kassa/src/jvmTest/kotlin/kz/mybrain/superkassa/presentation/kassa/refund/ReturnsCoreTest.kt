package kz.mybrain.superkassa.presentation.kassa.refund

import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import io.github.texport.superkassa.testing.api.kassa.VatMode
import kotlinx.coroutines.Dispatchers
import kz.kazakhtelecom.proto.v203.OperationTypeEnum
import kz.kazakhtelecom.proto.v203.TicketRequest
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LosingKassa
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.kassa.tiyn
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.kassa.sale.add
import kz.mybrain.superkassa.presentation.kassa.sale.sale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Возврат на настоящем ядре: основание ищется среди чеков дня, возвращается
 * отметками строк или суммой; итог сверяется с ядром и с тем, что получил
 * БФД — операция, основание, строки, суммы и налог.
 */
class ReturnsCoreTest {
    private val desk = CoreDesk()

    @AfterTest
    fun close() = desk.close()

    /** Экран возврата с выбранным чеком-основанием — последним проданным. */
    private fun chosen(
        model: ReturnsViewModel = returnsModel(desk.services, desk.kassaPorts)
    ): ReturnsViewModel {
        model.visit()
        model.choose(model.state.value.candidates.first())
        assertTrue(model.state.value.refund?.itemsRead == true, "строки основания не прочитаны")
        return model
    }

    /** Оформляет возврат и отдаёт то, что учёл БФД. */
    private fun ReturnsViewModel.refunded(): TicketRequest {
        refund.refund()
        refund.confirm()
        assertIs<Message.Done>(desk.said, "возврат не проведён: ${desk.saidText}")
        assertNull(state.value.refund, "проведённый возврат остался выбранным")
        return desk.bfd.countedTickets().last().also {
            assertEquals(OperationTypeEnum.OPERATION_SELL_RETURN, it.operation)
        }
    }

    @Test
    fun `чек дня находится по номеру, и отмеченная строка возвращается своей строкой`() {
        desk.seated()
        desk.sale().run {
            add("Хлеб", "450")
            add("Кумыс", "600")
            issue()
        }
        val model = returnsModel(desk.services, desk.kassaPorts).also { it.visit() }
        val number = assertNotNull(model.state.value.candidates.single().printedDocumentNumber)
        model.number(number.toString())
        val picked = chosen(model)
        picked.refund.toggle(1)

        val ticket = picked.refunded()

        assertEquals(listOf("Кумыс"), ticket.items.map { it.commodity?.name })
        assertEquals(KUMYS, ticket.amounts.total.tiyn())
        assertEquals(LOAF + KUMYS, ticket.parent_ticket?.parent_ticket_total.tiyn())
    }

    @Test
    fun `возврат суммой — одна строка на набранную сумму`() {
        desk.seated().sell("500.00", "3")
        val model = chosen()
        model.refund.enter("300")

        val ticket = model.refunded()

        assertEquals(1, ticket.items.size)
        assertEquals(THREE_HUNDRED, ticket.amounts.total.tiyn())
    }

    @Test
    fun `скидка на чек в основании — отметка строки возвращает оплаченное, а не цену`() {
        desk.seated().sellWithDiscount("1000.00", "100.00")
        val model = chosen()
        model.refund.toggle(0)

        val ticket = model.refunded()

        assertEquals(NINE_HUNDRED, ticket.amounts.total.tiyn(), "покупателю вернули больше, чем он заплатил")
    }

    @Test
    fun `НДС основания — строка возврата уходит с той же ставкой`() {
        desk.seated(VatMode.Payer(VatGroup.VAT_16)).sellWithItemVat(VatGroup.VAT_16)
        val model = chosen()
        model.refund.toggle(0)

        val ticket = model.refunded()

        assertEquals(listOf(PERCENT_16), ticket.items.single().commodity?.taxes?.map { it.percent })
    }

    /**
     * Возврат частью чека: повтор полного возврата тем же ключом ядро
     * отвергает «уже возвращено всё» — проверка остатка у него стоит раньше
     * поиска по ключу (дефект ядра, в отчёте). Второго возврата нет и там.
     */
    @Test
    fun `ответ на возврат потерян — повтор тем же ключом не проводит второй возврат`() {
        val kassa = desk.seated().also { it.sell("500.00", "3") }
        val losing = LosingKassa(EmbeddedKassa(desk.bench.api, Dispatchers.Unconfined))
        val services = CoreScene.services(losing, desk.signIn, desk.notices)
        val model = chosen(returnsModel(services, KassaPorts(FixedDeliverySetup())))
        model.refund.enter("300")

        model.refund.refund()

        model.refund.confirm()
        assertIs<Message.NoAnswer>(desk.said, desk.saidText)
        model.refund.refund()
        model.refund.confirm()

        assertIs<Message.Done>(desk.said, desk.saidText)
        val returns = kassa.api.listFiscalDocumentsByPeriod(kassa.kkmId, 0, Long.MAX_VALUE, PAGE, 0, kassa.adminPin)
            .filter { it.docType == "RETURN" }
        assertEquals(1, returns.size, "в ядре два возврата на одну попытку")
        assertEquals(1, desk.bfd.countedTickets().count { it.operation == OperationTypeEnum.OPERATION_SELL_RETURN })
    }

    private companion object {
        const val LOAF = 45_000L
        const val KUMYS = 60_000L
        const val THREE_HUNDRED = 30_000L
        const val NINE_HUNDRED = 90_000L
        const val PERCENT_16 = 16_000
        const val PAGE = 500
    }
}
