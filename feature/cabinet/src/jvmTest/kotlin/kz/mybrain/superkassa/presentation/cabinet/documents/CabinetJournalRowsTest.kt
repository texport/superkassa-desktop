package kz.mybrain.superkassa.presentation.cabinet.documents

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetCashMovement
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceipt
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReport
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
        val receipt = CabinetReceipt(transactionId = "t-2", deliveryStatus = "REJECTED")

        assertNull(receiptRow(receipt, cabinet).entry.refusal)
    }

    @Test
    fun `чек кабинета становится такой же строкой журнала`() {
        val receipt = CabinetReceipt(
            transactionId = "t-1",
            receiptNumber = "4138775047",
            shiftNumber = 3,
            operationType = "SALE",
            total = Decimal.parse("4500.84"),
            createdAt = "2026-09-07T19:02:59Z",
            deliveryStatus = "DELIVERED",
            kgdMark = "KGD-77"
        )

        val entry = receiptRow(receipt, cabinet).entry

        assertEquals(cabinet.documents.operationSale, entry.type)
        assertEquals(Glyphs.DASH, entry.number, "номера по счётчику кассы в списке кабинета нет")
        assertNull(entry.numberOrder)
        assertEquals(0, Decimal.parse("4500.84").compareTo(requireNotNull(entry.amountOrder)))
        assertEquals(JournalDelivery.Delivered, entry.delivery)
        assertEquals(3L, entry.shiftNo)
        assertEquals("4138775047", entry.sign, "номер чека кабинета — фискальный признак, как в журнале кассы")
        assertEquals(1_788_807_779_000, entry.at, "время кабинета сортируется числом, а не строкой")
    }

    @Test
    fun `кабинет различает четыре вида чека, и покупка не названа продажей`() {
        assertEquals(cabinet.documents.operationSale, documentTitle("SALE", cabinet))
        assertEquals(cabinet.documents.operationReturn, documentTitle("RETURN", cabinet))
        assertEquals(cabinet.documents.operationPurchase, documentTitle("BUY", cabinet))
        assertEquals(cabinet.documents.operationPurchaseReturn, documentTitle("BUY_RETURN", cabinet))
        listOf("BUY", "BUY_RETURN").forEach { code ->
            assertTrue(
                !documentTitle(code, cabinet).contains(cabinet.documents.operationSale),
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
        assertEquals(cabinet.documents.operationPurchase, types.single { it.code == "BUY" }.title)
        assertEquals(cabinet.documents.operationPurchaseReturn, types.single { it.code == "BUY_RETURN" }.title)
    }

    /**
     * Столбец «Состояние» — доставка в КГД, а не одно «Принят» на всех.
     *
     * Каждая строка правила: слова строки журнала и группа для цвета,
     * отбора и печати.
     */
    @Test
    fun `чек называет своё состояние в КГД по итогу, а без итога — по передаче`() {
        val docs = cabinet.documents
        val cases = listOf(
            Triple("DELIVERED", "ACCEPTED", docs.kgdAccepted to JournalDelivery.Delivered),
            Triple("REJECTED", "ACCEPTED", docs.kgdRejected to JournalDelivery.Refused),
            Triple("FAILED", "ACCEPTED", docs.kgdFailed to JournalDelivery.Refused),
            Triple("SENT", "ACCEPTED", docs.kgdSent to JournalDelivery.Queued),
            Triple(null, "IN_PROGRESS", docs.kgdTransferring to JournalDelivery.Queued),
            Triple(null, "FAILED", docs.kgdTransferFailed to JournalDelivery.Refused),
            Triple(null, "ACCEPTED", docs.kgdAwaiting to JournalDelivery.Queued),
            Triple(null, null, docs.kgdAwaiting to JournalDelivery.Queued)
        )
        cases.forEach { (delivery, send, expected) ->
            val receipt = CabinetReceipt(
                transactionId = "t",
                operationType = "SALE",
                deliveryStatus = delivery,
                sendStatus = send
            )
            val entry = receiptRow(receipt, cabinet).entry
            assertEquals(expected.first, entry.deliveryWords, "$delivery / $send")
            assertEquals(expected.second, entry.delivery, "$delivery / $send")
        }
    }

    @Test
    fun `в КГД уходят чеки и Z-отчёт, а X-отчёт и движение денег — никогда`() {
        val docs = cabinet.documents
        val x = reportRow(CabinetReport(transactionId = "x", type = "X", deliveryStatus = "DELIVERED"), cabinet).entry
        val z = reportRow(CabinetReport(transactionId = "z", type = "Z", deliveryStatus = "DELIVERED"), cabinet).entry
        val moves = listOf("DEPOSIT", "WITHDRAWAL").map { type ->
            val movement = CabinetCashMovement(transactionId = type, type = type, sendStatus = "IN_PROGRESS")
            movementRow(movement, cabinet).entry
        }

        assertEquals(docs.kgdNotSent, x.deliveryWords)
        assertEquals(JournalDelivery.Internal, x.delivery)
        assertEquals(docs.kgdAccepted, z.deliveryWords)
        moves.forEach { assertEquals(docs.kgdNotSent, it.deliveryWords) }
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
