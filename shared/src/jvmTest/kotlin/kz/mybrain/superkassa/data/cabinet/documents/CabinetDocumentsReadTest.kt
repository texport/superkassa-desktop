package kz.mybrain.superkassa.data.cabinet.documents

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.CabinetWire
import kz.mybrain.superkassa.domain.cabinet.model.documents.ReceiptSearch
import kz.mybrain.superkassa.domain.cabinet.port.CabinetDocuments
import kz.mybrain.superkassa.replying
import kz.mybrain.superkassa.signedPorts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Фискальные документы кабинета: разбор ответов.
 *
 * Ответы ниже сняты с контракта кабинета. Проверяется то, из-за чего
 * раздел показал бы неверное: суммы приходят десятичной записью и обязаны
 * дойти до тиына, а состав чека — списком, а не строкой.
 */
class CabinetDocumentsReadTest {

    private fun clientReturning(body: String): CabinetDocuments =
        CabinetWire(http = replying(body)).signedPorts().documents

    @Test
    fun `чек приходит страницей с суммой до тиына`() {
        val client = clientReturning(
            """{"page":0,"size":50,"totalElements":1,"items":[
               {"transactionId":"t-1","receiptNumber":"12","shiftNumber":3,"operationType":"SALE",
                "total":435.84,"createdAt":"2026-09-07T19:02:59Z","deliveryStatus":"ONLINE_OK"}]}"""
        )
        val page = runBlocking { client.receipts("kkm", ReceiptSearch()) }
        assertEquals(1, page.items.size)
        assertEquals(Decimal.parse("435.84"), page.items.single().total)
    }

    @Test
    fun `состав чека разбирается со всеми частями`() {
        val client = clientReturning(
            """{"transactionId":"t-1","receiptNumber":"12","operationType":"SALE","total":435.84,
               "operator":{"code":1,"name":"Администратор"},
               "items":[{"positionNumber":1,"name":"Кофе","quantity":3.0,"price":150.55,"amount":435.84,
                         "taxPercent":16,"taxAmount":60.12}],
               "taxTotal":60.12,"cashTotal":435.84,"cardTotal":0,"protocolDocumentId":"3846668294"}"""
        )
        val receipt = runBlocking { client.receipt("kkm", "t-1") }
        assertEquals("Кофе", receipt.items.single().name)
        assertEquals(Decimal.parse("150.55"), receipt.items.single().price)
        assertEquals("CASH", receipt.payments.single().type)
        assertEquals(16, receipt.items.single().taxes.single().percent)
        assertEquals(Decimal.parse("60.12"), receipt.taxes.single().sum)
        assertEquals("3846668294", receipt.kkmDocumentNumber)
        assertEquals(Decimal.parse("435.84"), receipt.amounts?.total)
        assertEquals("Администратор", receipt.operator?.name)
    }

    @Test
    fun `отчёт несёт смену и её границы`() {
        val client = clientReturning(
            """{"transactionId":"z-1","reportType":"Z","shiftNumber":3,"saleTotal":1000.00,
               "returnTotal":100.00,"cashBalance":900.00,"receiptsCount":7,"deliveryStatus":"ONLINE_OK"}"""
        )
        val report = runBlocking { client.report("kkm", "z-1") }
        assertEquals("Z", report.type)
        assertEquals(3, report.shiftNumber)
        assertEquals(7, report.receiptsCount)
        // Наличные отчёта — остаток ящика, а не оплаченное наличными:
        // подписью «Наличные в кассе» стояло второе, и карточка показывала
        // не ту сумму.
        assertEquals(Decimal.parse("900.00"), report.cashBalance)
    }

    /**
     * Прежде проверка требовала у движения кассира. Кабинет его по движению
     * не отдаёт — и не отдавал: поле стояло в модели впустую, а карточка
     * рисовала «Пробит —» с прочерком. Проверяется то, что приходит.
     */
    @Test
    fun `движение денег несёт сумму, смену и состояние передачи`() {
        val client = clientReturning(
            """{"transactionId":"m-1","movementType":"DEPOSIT","amount":5000.00,"shiftNumber":3,
               "protocolDocumentId":"69","sendStatus":"ACCEPTED"}"""
        )
        val movement = runBlocking { client.movement("kkm", "m-1") }
        assertEquals("DEPOSIT", movement.type)
        assertEquals(Decimal.parse("5000.00"), movement.amount)
        assertEquals(3, movement.shiftNumber)
        assertEquals("69", movement.protocolDocumentId)
        assertEquals("ACCEPTED", movement.sendStatus)
    }

    @Test
    fun `пакет протокола доходит до узла и объектом, и строкой`() {
        val asObject = clientReturning(
            """{"transactionId":"t-1","payload":{"request":{"command":"COMMAND_TICKET"},"response":{}}}"""
        )
        val asText = clientReturning(
            """{"transactionId":"z-1","payload":"{\"request\":{\"command\":\"COMMAND_REPORT\"}}"}"""
        )
        val movement = clientReturning("""{"transactionId":"m-1"}""")

        val receipt = runBlocking { asObject.receipt("kkm", "t-1") }
        val report = runBlocking { asText.report("kkm", "z-1") }

        assertEquals(
            """{"request":{"command":"COMMAND_TICKET"},"response":{}}""",
            receipt.packet
        )
        assertEquals("""{"request":{"command":"COMMAND_REPORT"}}""", report.packet)
        assertNull(runBlocking { movement.movement("kkm", "m-1") }.packet)
    }
}
