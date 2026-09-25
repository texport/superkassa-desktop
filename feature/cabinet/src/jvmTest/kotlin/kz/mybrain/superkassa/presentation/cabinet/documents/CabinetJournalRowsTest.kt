package kz.mybrain.superkassa.presentation.cabinet.documents

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceipt
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetShift
import kz.mybrain.superkassa.presentation.common.document.JournalDelivery
import kz.mybrain.superkassa.presentation.common.document.journalTypesIn
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Документы кабинета — теми же строками журнала, что и документы кассы.
 *
 * Показ, поиск и отбор журнала написаны один раз, а кабинет приводит
 * к ним свои чеки и смены: состояние доставки он называет своими кодами,
 * а сумму и время отдаёт своими типами.
 */
class CabinetJournalRowsTest {

    private val cabinet = textsOf(Language.Ru).cabinet

    @Test
    fun `кабинет причины отказа не отдаёт, и придумывать её строка не станет`() {
        val receipt = CabinetReceipt(transactionId = "t-2", deliveryStatus = "DELIVERY_ERROR")

        assertNull(receiptRow(receipt, cabinet).entry.refusal)
    }

    @Test
    fun `чек кабинета становится такой же строкой журнала`() {
        val receipt = CabinetReceipt(
            transactionId = "t-1",
            receiptNumber = "12",
            shiftNumber = 3,
            operationType = "SALE",
            total = Decimal.parse("4500.84"),
            createdAt = "2026-09-07T19:02:59Z",
            deliveryStatus = "ONLINE_OK",
            kgdMark = "KGD-77"
        )

        val entry = receiptRow(receipt, cabinet).entry

        assertEquals(cabinet.operationSale, entry.type)
        assertEquals("12", entry.number)
        assertEquals(12L, entry.numberOrder)
        assertEquals(0, Decimal.parse("4500.84").compareTo(requireNotNull(entry.amountOrder)))
        assertEquals(JournalDelivery.Delivered, entry.delivery)
        assertEquals(3L, entry.shiftNo)
        assertEquals("KGD-77", entry.sign)
        assertEquals(1_788_807_779_000, entry.at, "время кабинета сортируется числом, а не строкой")
    }

    @Test
    fun `кабинет различает четыре вида чека, и покупка не названа продажей`() {
        assertEquals(cabinet.operationSale, documentTitle("SALE", cabinet))
        assertEquals(cabinet.operationReturn, documentTitle("RETURN", cabinet))
        assertEquals(cabinet.operationPurchase, documentTitle("BUY", cabinet))
        assertEquals(cabinet.operationPurchaseReturn, documentTitle("BUY_RETURN", cabinet))
        listOf("BUY", "BUY_RETURN").forEach { code ->
            assertTrue(
                !documentTitle(code, cabinet).contains(cabinet.operationSale),
                "покупка выдана за продажу: $code"
            )
        }
    }

    @Test
    fun `покупка стоит в отборе журнала своей плашкой, а не в плашке продажи`() {
        val rows = listOf("SALE", "BUY", "BUY_RETURN").map { code ->
            receiptRow(CabinetReceipt(transactionId = "t-$code", operationType = code), cabinet).entry
        }

        val types = journalTypesIn(rows)

        assertEquals(3, types.size, "три вида чека — три плашки отбора")
        assertEquals(cabinet.operationPurchase, types.single { it.code == "BUY" }.title)
        assertEquals(cabinet.operationPurchaseReturn, types.single { it.code == "BUY_RETURN" }.title)
    }

    @Test
    fun `состояния кабинета и узла сходятся в одних словах`() {
        assertEquals(JournalDelivery.Delivered, cabinetDelivery("ONLINE_OK"))
        assertEquals(JournalDelivery.Delivered, cabinetDelivery("DELIVERED"))
        assertEquals(JournalDelivery.Refused, cabinetDelivery("DELIVERY_ERROR"))
        assertEquals(JournalDelivery.Refused, cabinetDelivery("REJECTED"))
        assertEquals(JournalDelivery.Queued, cabinetDelivery("OFFLINE_QUEUED"))
        assertNull(cabinetDelivery(" "))
    }

    @Test
    fun `смена кабинета говорит о себе своим состоянием, а не доставкой`() {
        val closed = shiftRow(CabinetShift(shiftNumber = 8, state = "CLOSED"), cabinet).entry
        val open = shiftRow(CabinetShift(shiftNumber = 9, state = "OPEN"), cabinet).entry

        assertNull(closed.delivery, "смена в ОФД не доставляется — она открыта или закрыта")
        assertEquals(cabinet.statuses.shiftClosed, closed.state?.title)
        assertTrue(closed.state?.done == true)
        assertTrue(open.state?.done == false)
        assertTrue(!open.printable, "выписки по незакрытой смене нет: её итоги ещё не сошлись")
    }
}
