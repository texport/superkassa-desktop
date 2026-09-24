package kz.mybrain.superkassa.integrations.bfdcabinet

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.DocumentPeriod
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.ReceiptSearch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Документы касс: суммы до тиына, состав чека, пакет протокола и отбор по периоду. */
class CabinetDocumentsTest {

    @Test
    fun receiptPageKeepsSumToTiyn() = runTest {
        val page = CabinetFake.always(
            """{"page":0,"size":50,"totalElements":1,"items":[
               {"transactionId":"t-1","receiptNumber":"12","shiftNumber":3,"operationType":"SALE",
                "total":435.84,"createdAt":"2026-09-07T19:02:59Z","deliveryStatus":"ONLINE_OK"}]}"""
        ).cabinet().documents.receipts("kkm", ReceiptSearch())

        assertEquals(43_584L, page.items.single().total?.tiyn())
    }

    @Test
    fun receiptIsReadWithAllParts() = runTest {
        val receipt = CabinetFake.always(
            """{"transactionId":"t-1","receiptNumber":"12","shiftNumber":3,
               "operationType":"SALE","total":435.84,"taxTotal":60.12,"cashTotal":435.84,"cardTotal":0,
               "createdAt":"2026-09-07T19:02:59Z","registrationNumber":"000000010001",
               "operator":{"code":1,"name":"Администратор"},
               "items":[{"positionNumber":1,"name":"Кофе молотый «Эфиопия Иргачеффе» 250 г","quantity":3.0,
                         "price":150.55,"amount":435.84,"taxPercent":16,"taxAmount":60.12}],
               "deliveryStatus":"ONLINE_OK","kgdMark":"210000000001","kgdMarkAt":"2026-09-07T19:03:01Z",
               "protocolDocumentId":"3846668294"}"""
        ).cabinet().documents.receipt("kkm", "t-1")
        val item = receipt.items.single()

        assertEquals("3.0", item.quantity?.plain)
        assertEquals(15_055L, item.price?.tiyn())
        assertEquals(43_584L, item.sum?.tiyn())
        assertEquals(CabinetDecimal.of("16"), item.taxPercent)
        assertEquals(0L, receipt.cardTotal?.tiyn())
        assertEquals("3846668294", receipt.kkmDocumentNumber)
        assertEquals("210000000001", receipt.kgdMark)
        assertEquals("Администратор", receipt.operator?.name)
    }

    @Test
    fun reportAndMovementCarryShiftAndCash() = runTest {
        val report = CabinetFake.always(
            """{"transactionId":"z-1","reportType":"Z","shiftNumber":3,"saleTotal":1000.00,
               "returnTotal":100.00,"cashBalance":900.00,"receiptsCount":7,"deliveryStatus":"ONLINE_OK"}"""
        ).cabinet().documents.report("kkm", "z-1")
        val movement = CabinetFake.always(
            """{"transactionId":"m-1","movementType":"DEPOSIT","amount":5000.00,"shiftNumber":3,
               "protocolDocumentId":"69","sendStatus":"ACCEPTED"}"""
        ).cabinet().documents.movement("kkm", "m-1")

        assertEquals("Z", report.type)
        assertEquals(7, report.receiptsCount)
        assertEquals(90_000L, report.cashBalance?.tiyn())
        assertEquals("DEPOSIT", movement.type)
        assertEquals(500_000L, movement.amount?.tiyn())
        assertEquals("69", movement.protocolDocumentId)
    }

    /** Кабинет хранит пакет `jsonb` и отдаёт его то объектом, то строкой. */
    @Test
    fun protocolPacketComesAsObjectOrString() = runTest {
        val ticket = """{"request":{"command":"COMMAND_TICKET"},"response":{}}"""
        val report = """{"request":{"command":"COMMAND_REPORT"}}"""
        val asObject = CabinetFake.always("""{"transactionId":"t-1","payload":$ticket}""").cabinet()
        val quoted = Json.encodeToString(report)
        val asText = CabinetFake.always("""{"transactionId":"z-1","payload":$quoted}""").cabinet()
        val without = CabinetFake.always("""{"transactionId":"m-1"}""").cabinet()

        assertEquals(ticket, asObject.documents.receipt("k", "t-1").packet)
        assertEquals(report, asText.documents.report("k", "z-1").packet)
        assertNull(without.documents.movement("k", "m-1").packet)
    }

    @Test
    fun periodGoesAsParametersForReportsAndMovements() = runTest {
        val fake = CabinetFake.always("""{"page":0,"size":50,"totalElements":0,"items":[]}""")
        val documents = fake.cabinet().documents
        val period = DocumentPeriod(from = "2026-09-01T00:00:00Z", to = "2026-09-07T23:59:59Z")

        documents.reports("r 1", page = 2, period = period)
        documents.movements("r-1", period = DocumentPeriod(from = "2026-09-01T00:00:00Z"))
        documents.shifts("r-1")

        val reports = fake.asked[0].url
        assertEquals("/api/cash-registers/r%201/reports", reports.encodedPath)
        assertEquals("2", reports.parameters["page"])
        assertEquals("2026-09-01T00:00:00Z", reports.parameters["dateFrom"])
        assertEquals("2026-09-07T23:59:59Z", reports.parameters["dateTo"])
        assertNull(fake.asked[1].url.parameters["dateTo"], "одна граница потянула за собой вторую")
        assertEquals("/api/cash-registers/r-1/shifts?page=0&size=50", fake.asked[2].target())
    }

    /** У чеков границы уходят телом отбора; умолчания в тело не пишутся — кабинет подставит свои. */
    @Test
    fun receiptSearchGoesInBody() = runTest {
        val fake = CabinetFake.always("""{"page":0,"size":50,"totalElements":0,"items":[]}""")
        val search = ReceiptSearch(dateFrom = "2026-09-01T00:00:00Z", sumFrom = CabinetDecimal.of("100.50"))
        fake.cabinet().documents.receipts("r-1", search)

        val body = fake.asked.single().text()
        assertEquals("/api/cash-registers/r-1/receipts/search", fake.asked.single().url.encodedPath)
        assertTrue(body.contains(""""dateFrom":"2026-09-01T00:00:00Z""""), body)
        assertTrue(body.contains(""""sumFrom":100.50"""), body)
        assertTrue(!body.contains("dateTo"), body)
    }
}
