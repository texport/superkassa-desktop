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
import kz.mybrain.superkassa.desktop.server.cabinet.ReceiptSearch
import kz.mybrain.superkassa.desktop.server.cabinet.cashMovement
import kz.mybrain.superkassa.desktop.server.cabinet.receipt
import kz.mybrain.superkassa.desktop.server.cabinet.receipts
import kz.mybrain.superkassa.desktop.server.cabinet.report
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

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
               "items":[{"name":"Кофе","quantity":3.0,"price":150.55,"sum":435.84}],
               "payments":[{"type":"CASH","sum":435.84}],
               "taxes":[{"type":"VAT","percent":16,"sum":60.12}],
               "amounts":{"total":435.84,"taken":500.00,"change":64.16}}"""
        )
        val receipt = runBlocking { client.receipt("token", "kkm", "t-1") }
        assertEquals("Кофе", receipt.items.single().name)
        assertEquals(0, BigDecimal("150.55").compareTo(receipt.items.single().price))
        assertEquals("CASH", receipt.payments.single().type)
        assertEquals(16, receipt.taxes.single().percent)
        assertEquals(0, BigDecimal("64.16").compareTo(receipt.amounts?.change))
        assertEquals("Администратор", receipt.operator?.name)
    }

    @Test
    fun `отчёт несёт смену и её границы`() {
        val client = clientReturning(
            """{"transactionId":"z-1","type":"Z","shiftNumber":3,"total":1000.00,
               "shiftOpenedAt":"2026-09-07T09:00:00Z","shiftClosedAt":"2026-09-07T21:00:00Z",
               "deliveryStatus":"ONLINE_OK"}"""
        )
        val report = runBlocking { client.report("token", "kkm", "z-1") }
        assertEquals("Z", report.type)
        assertEquals(3, report.shiftNumber)
        assertEquals("2026-09-07T21:00:00Z", report.shiftClosedAt)
    }

    @Test
    fun `движение денег несёт сумму и кассира`() {
        val client = clientReturning(
            """{"transactionId":"m-1","type":"DEPOSIT","amount":5000.00,"shiftNumber":3,
               "operator":{"code":1,"name":"Айгүл Серікова"}}"""
        )
        val movement = runBlocking { client.cashMovement("token", "kkm", "m-1") }
        assertEquals("DEPOSIT", movement.type)
        assertEquals(0, BigDecimal("5000.00").compareTo(movement.amount))
        assertEquals("Айгүл Серікова", movement.operator?.name)
    }
}
