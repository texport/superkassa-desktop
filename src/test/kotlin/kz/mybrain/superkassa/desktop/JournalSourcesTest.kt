package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReceipt
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetShift
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetDelivery
import kz.mybrain.superkassa.desktop.ui.cabinet.documentTitle
import kz.mybrain.superkassa.desktop.ui.cabinet.receiptRow
import kz.mybrain.superkassa.desktop.ui.cabinet.shiftRow
import kz.mybrain.superkassa.desktop.ui.history.JournalDelivery
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriod
import kz.mybrain.superkassa.desktop.ui.history.JournalQuery
import kz.mybrain.superkassa.desktop.ui.history.JournalSpan
import kz.mybrain.superkassa.desktop.ui.history.journalEntriesOf
import kz.mybrain.superkassa.desktop.ui.history.journalTypesIn
import kz.mybrain.superkassa.desktop.ui.history.select
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Два источника одного журнала: узел и кабинет.
 *
 * Показ, поиск и отбор написаны один раз, а источники приводят к ним свои
 * записи. Проверяется то, из-за чего экраны разошлись бы: состояние
 * доставки узел и кабинет называют разными кодами, а сумму и время они
 * отдают разными типами.
 */
class JournalSourcesTest {

    private val texts = stringsOf(Language.Ru)
    private val cabinet = cabinetTexts(Language.Ru)

    private fun session(): Session {
        val http = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })
        val directory = Files.createTempDirectory("journal-sources").toFile()
        val session = Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
        session.switchLanguage(Language.Ru)
        session.dictionaries[Dictionary.DocumentTypes] = listOf(
            DictionaryEntry("SALE", mapOf("ru" to "Продажа", "kk" to "Сатылым", "en" to "Sale"))
        )
        return session
    }

    @Test
    fun `документ узла становится строкой журнала со суммой до тиына`() {
        val document = Document(
            id = "d-1",
            docNo = 12,
            docType = "SALE",
            ofdStatus = "SENT",
            fiscalSign = "AB12CD34",
            totalAmount = 450_084,
            createdAt = 1_788_807_779_000,
            shiftNo = 3
        )

        val entry = journalEntriesOf(session(), texts, listOf(document)).single()

        assertEquals("Продажа", entry.type)
        assertEquals("12", entry.number)
        assertEquals(0, BigDecimal("4500.84").compareTo(entry.amountOrder))
        assertEquals("AB12CD34", entry.sign)
        assertEquals(3L, entry.shiftNo)
        assertEquals(JournalDelivery.Delivered, entry.delivery)
    }

    @Test
    fun `автономный чек узла назван доставленным позже, а отклонённый — отказом`() {
        val autonomous = Document(id = "a", ofdStatus = "SENT", isAutonomous = true, autonomousSign = "AUT1")
        val refused = Document(id = "r", ofdStatus = "FAILED")
        val queued = Document(id = "q", ofdStatus = "PENDING")
        val opened = Document(id = "o", docType = "SHIFT_OPEN", ofdStatus = "SENT")
        val strange = Document(id = "s", ofdStatus = "WHAT_IS_THIS")

        val states = journalEntriesOf(session(), texts, listOf(autonomous, refused, queued, opened, strange))
            .associate { it.key to it.delivery }

        assertEquals(JournalDelivery.Resent, states["a"])
        assertEquals(JournalDelivery.Refused, states["r"])
        assertEquals(JournalDelivery.Queued, states["q"])
        assertEquals(JournalDelivery.Internal, states["o"], "открытие смены в ОФД не уходит вовсе")
        assertNull(states["s"], "незнакомый код состоянием не становится: кодов на экране быть не должно")
        assertEquals("AUT1", journalEntriesOf(session(), texts, listOf(autonomous)).single().sign)
    }

    @Test
    fun `отклонённый документ не печатается`() {
        val refused = Document(id = "r", ofdStatus = "FAILED")
        assertTrue(!journalEntriesOf(session(), texts, listOf(refused)).single().printable)
    }

    @Test
    fun `чек кабинета становится такой же строкой журнала`() {
        val receipt = CabinetReceipt(
            transactionId = "t-1",
            receiptNumber = "12",
            shiftNumber = 3,
            operationType = "SALE",
            total = BigDecimal("4500.84"),
            createdAt = "2026-09-07T19:02:59Z",
            deliveryStatus = "ONLINE_OK",
            kgdMark = "KGD-77"
        )

        val entry = receiptRow(receipt, cabinet).entry

        assertEquals(cabinet.operationSale, entry.type)
        assertEquals("12", entry.number)
        assertEquals(12L, entry.numberOrder)
        assertEquals(0, BigDecimal("4500.84").compareTo(entry.amountOrder))
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
        assertEquals(cabinet.shiftClosed, closed.state?.title)
        assertTrue(closed.state?.done == true)
        assertTrue(open.state?.done == false)
        assertTrue(!open.printable, "выписки по незакрытой смене нет: её итоги ещё не сошлись")
    }

    @Test
    fun `один и тот же чек находится и у узла, и в кабинете одним поиском`() {
        val document = Document(
            id = "d-1",
            docNo = 12,
            docType = "SALE",
            ofdStatus = "SENT",
            totalAmount = 450_084,
            createdAt = 1_788_807_779_000,
            shiftNo = 3
        )
        val receipt = CabinetReceipt(
            transactionId = "t-1",
            receiptNumber = "12",
            shiftNumber = 3,
            operationType = "SALE",
            total = BigDecimal("4500.84"),
            createdAt = "2026-09-07T19:02:59Z",
            deliveryStatus = "ONLINE_OK"
        )
        val query = JournalQuery(search = "4500.84", shiftNo = 3)

        assertEquals(1, journalEntriesOf(session(), texts, listOf(document)).select(query).size)
        assertEquals(1, listOf(receiptRow(receipt, cabinet).entry).select(query).size)
    }

    @Test
    fun `день срока — местные сутки, а не отрезок от сейчас`() {
        val day = JournalPeriod.of(JournalSpan.Day, LocalDate.of(2026, 9, 7))
        val window = requireNotNull(day.range)

        assertEquals(LocalDate.of(2026, 9, 7), window.from)
        assertEquals(LocalDate.of(2026, 9, 7), window.to)
        assertTrue(window.oneDay)
        assertEquals(1L, window.days)
    }

    @Test
    fun `окно перелистывается на свою длину`() {
        val week = JournalPeriod.of(JournalSpan.Week, LocalDate.of(2026, 9, 14))
        val earlier = requireNotNull(week.shiftedBy(-1).range)

        assertEquals(LocalDate.of(2026, 9, 1), earlier.from)
        assertEquals(LocalDate.of(2026, 9, 7), earlier.to)
        assertEquals(7L, earlier.days)
    }
}
