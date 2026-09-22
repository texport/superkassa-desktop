package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetShift
import kz.mybrain.superkassa.desktop.server.cabinet.DocumentsOverview
import kz.mybrain.superkassa.desktop.server.cabinet.ReceiptSearch
import kz.mybrain.superkassa.desktop.server.cabinet.cashMovement
import kz.mybrain.superkassa.desktop.server.cabinet.receipt
import kz.mybrain.superkassa.desktop.server.cabinet.receipts
import kz.mybrain.superkassa.desktop.server.cabinet.report
import kz.mybrain.superkassa.desktop.ui.cabinet.DocumentKind
import kz.mybrain.superkassa.desktop.ui.cabinet.documentsEmpty
import kz.mybrain.superkassa.desktop.ui.cabinet.shiftRow
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriod
import kz.mybrain.superkassa.desktop.ui.history.JournalSpan
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Фискальные документы кабинета: разбор ответов.
 *
 * Ответы ниже сняты с контракта кабинета. Проверяется то, из-за чего
 * раздел показал бы неверное: суммы приходят десятичной записью и обязаны
 * дойти до тиына, а состав чека — списком, а не строкой.
 */
class CabinetDocumentsTest {

    private fun clientReturning(body: String): CabinetClient {
        val engine = MockEngine {
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
    }

    @Test
    fun `чек приходит страницей с суммой до тиына`() {
        val client = clientReturning(
            """{"page":0,"size":50,"totalElements":1,"items":[
               {"transactionId":"t-1","receiptNumber":"12","shiftNumber":3,"operationType":"SALE",
                "total":435.84,"createdAt":"2026-09-07T19:02:59Z","deliveryStatus":"ONLINE_OK"}]}"""
        )
        val page = runBlocking { client.receipts("token", "kkm", ReceiptSearch()) }
        assertEquals(1, page.items.size)
        assertEquals(0, BigDecimal("435.84").compareTo(page.items.single().total))
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
        val receipt = runBlocking { client.receipt("token", "kkm", "t-1") }
        assertEquals("Кофе", receipt.items.single().name)
        assertEquals(0, BigDecimal("150.55").compareTo(receipt.items.single().price))
        assertEquals("CASH", receipt.payments.single().type)
        assertEquals(16, receipt.items.single().taxes.single().percent)
        assertEquals(0, BigDecimal("60.12").compareTo(receipt.taxes.single().sum))
        assertEquals("3846668294", receipt.kkmDocumentNumber)
        assertEquals(0, BigDecimal("435.84").compareTo(receipt.amounts?.total))
        assertEquals("Администратор", receipt.operator?.name)
    }

    @Test
    fun `отчёт несёт смену и её границы`() {
        val client = clientReturning(
            """{"transactionId":"z-1","reportType":"Z","shiftNumber":3,"saleTotal":1000.00,
               "returnTotal":100.00,"cashBalance":900.00,"receiptsCount":7,"deliveryStatus":"ONLINE_OK"}"""
        )
        val report = runBlocking { client.report("token", "kkm", "z-1") }
        assertEquals("Z", report.type)
        assertEquals(3, report.shiftNumber)
        assertEquals(7, report.receiptsCount)
        // Наличные отчёта — остаток ящика, а не оплаченное наличными:
        // подписью «Наличные в кассе» стояло второе, и карточка показывала
        // не ту сумму.
        assertEquals(0, BigDecimal("900.00").compareTo(report.cashBalance))
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
        val movement = runBlocking { client.cashMovement("token", "kkm", "m-1") }
        assertEquals("DEPOSIT", movement.type)
        assertEquals(0, BigDecimal("5000.00").compareTo(movement.amount))
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

        val receipt = runBlocking { asObject.receipt("token", "kkm", "t-1") }
        val report = runBlocking { asText.report("token", "kkm", "z-1") }

        assertEquals(
            """{"request":{"command":"COMMAND_TICKET"},"response":{}}""",
            receipt.packet
        )
        assertEquals("""{"request":{"command":"COMMAND_REPORT"}}""", report.packet)
        assertNull(runBlocking { movement.cashMovement("token", "kkm", "m-1") }.packet)
    }

    @Test
    fun `у смены печатать нечего — её Z-отчёт стоит своей строкой`() {
        val shift = shiftRow(
            CabinetShift(shiftNumber = 3, state = "CLOSED", saleTotal = BigDecimal("100.00")),
            cabinetTexts(Language.Ru)
        )

        assertFalse(shift.entry.printable)
    }

    /**
     * Выручка смены — продажи за вычетом возвратов.
     *
     * Прежде и строка журнала, и подпись «Выручка» брали продажи как есть:
     * у смены 12 кассы 260940000021 стояло 3 570 ₸ при возвратах 1 900 ₸,
     * и сами возвраты были перечислены строкой ниже в той же карточке.
     */
    @Test
    fun `выручка смены уменьшена на возвраты`() {
        val shift = CabinetShift(
            shiftNumber = 12,
            state = "CLOSED",
            receiptsCount = 10,
            saleTotal = BigDecimal("3570.00"),
            returnTotal = BigDecimal("1900.00"),
            buyTotal = BigDecimal("400.00"),
            cashBalance = BigDecimal("6070.00")
        )

        assertEquals(0, BigDecimal("1670.00").compareTo(shift.total))
        assertEquals(0, BigDecimal("1670.00").compareTo(shift.totals?.revenue))
        // Продажи и покупка остаются собой: их владелец сверяет с лентой.
        assertEquals(0, BigDecimal("3570.00").compareTo(shift.totals?.salesSum))
        assertEquals(0, BigDecimal("400.00").compareTo(shift.totals?.purchasesSum))
    }

    /**
     * Пустой список за срок не объявляет кассу пустой.
     *
     * Над списком стоят счётчики за всё время. У кассы со ста чеками
     * и выбранной неделей экран говорил «Здесь появится то, что БФД
     * приняла от этой кассы» — рядом с собственной сотней.
     */
    @Test
    fun `пусто за срок и пусто вовсе названы по-разному`() {
        val texts = cabinetTexts(Language.Ru)
        val hundred = DocumentsOverview(cashRegisterId = "c-1", receiptsCount = 100)
        val week = JournalPeriod.of(JournalSpan.Week)

        assertEquals(
            texts.documentsNoneInPeriod,
            documentsEmpty(DocumentKind.Receipts, week, hundred, texts).title,
            "пустая неделя объявила кассу без чеков"
        )
        assertEquals(
            texts.documentsEmpty,
            documentsEmpty(DocumentKind.Receipts, week, DocumentsOverview(cashRegisterId = "c-1"), texts).title,
            "у кассы без чеков вовсе предложено сменить срок"
        )
        assertEquals(
            texts.documentsEmpty,
            documentsEmpty(DocumentKind.Receipts, JournalPeriod.of(JournalSpan.All), hundred, texts).title,
            "за всё время предложено сменить срок"
        )
        // У смен срока нет вовсе: кабинет их по дате не отдаёт.
        assertEquals(
            texts.documentsEmpty,
            documentsEmpty(DocumentKind.Shifts, week, hundred.copy(shiftsCount = 5), texts).title,
            "смены, которых кабинет по сроку не отдаёт, предложено искать сроком"
        )
    }
}
