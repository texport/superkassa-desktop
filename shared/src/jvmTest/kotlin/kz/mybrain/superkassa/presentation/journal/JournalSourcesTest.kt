package kz.mybrain.superkassa.presentation.journal

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceipt
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetShift
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.cabinet.documents.cabinetDelivery
import kz.mybrain.superkassa.presentation.cabinet.documents.documentTitle
import kz.mybrain.superkassa.presentation.cabinet.documents.receiptRow
import kz.mybrain.superkassa.presentation.cabinet.documents.shiftRow
import kz.mybrain.superkassa.presentation.common.document.JournalDelivery
import kz.mybrain.superkassa.presentation.common.document.JournalQuery
import kz.mybrain.superkassa.presentation.common.document.journalTypesIn
import kz.mybrain.superkassa.presentation.common.document.select
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kz.mybrain.superkassa.presentation.journal.documents.journalEntriesOf
import kz.mybrain.superkassa.refusal
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Два источника одного журнала: касса и кабинет.
 *
 * Показ, поиск и отбор написаны один раз, а источники приводят к ним свои
 * записи. Проверяется то, из-за чего экраны разошлись бы: состояние
 * доставки касса и кабинет называют разными кодами, а сумму и время они
 * отдают разными типами.
 */
class JournalSourcesTest {

    private val texts = textsOf(Language.Ru).common
    private val cabinet = textsOf(Language.Ru).cabinet

    private val names = mapOf("SALE" to TrilingualMessageResponse("Продажа", "Сатылым", "Sale"))

    /** Строки журнала кассы на русском. */
    private fun entries(vararg documents: FiscalDocumentResponse) =
        journalEntriesOf(texts, Language.Ru, names, documents.toList())

    /** Документ кассы: вид, состояние доставки и то, что проверке важно. */
    private fun document(
        id: String,
        status: String? = "SENT",
        type: String = "SALE",
        amount: Long? = null
    ) = CoreScene.document(id, type = type, amount = amount, status = status)

    @Test
    fun `документ кассы становится строкой журнала со суммой до тиына`() {
        val document = document("d-1", amount = 450_084)
            .copy(docNo = 12, fiscalSign = "AB12CD34", createdAt = 1_788_807_779_000, shiftNo = 3)

        val entry = entries(document).single()

        assertEquals("Продажа", entry.type)
        assertEquals("12", entry.number)
        assertEquals(0, Decimal.parse("4500.84").compareTo(requireNotNull(entry.amountOrder)))
        assertEquals("AB12CD34", entry.sign)
        assertEquals(3L, entry.shiftNo)
        assertEquals(JournalDelivery.Delivered, entry.delivery)
    }

    @Test
    fun `автономный чек кассы назван доставленным позже, а отклонённый — отказом`() {
        val autonomous = document("a").copy(isAutonomous = true, autonomousSign = "AUT1")
        val refused = document("r", status = "FAILED")
        val queued = document("q", status = "PENDING")
        val opened = document("o", type = "SHIFT_OPEN")
        val strange = document("s", status = "WHAT_IS_THIS")

        val states = entries(autonomous, refused, queued, opened, strange).associate { it.key to it.delivery }

        assertEquals(JournalDelivery.Resent, states["a"])
        assertEquals(JournalDelivery.Refused, states["r"])
        assertEquals(JournalDelivery.Queued, states["q"])
        assertEquals(JournalDelivery.Internal, states["o"], "открытие смены в ОФД не уходит вовсе")
        assertEquals(
            JournalDelivery.Unknown,
            states["s"],
            "незнакомый код обязан дойти до строки словами: протокольных кодов на экране быть не должно"
        )
        assertEquals("AUT1", entries(autonomous).single().sign)
    }

    @Test
    fun `отклонённый документ не печатается`() {
        assertTrue(!entries(document("r", status = "FAILED")).single().printable)
    }

    /**
     * У отклонённого документа в журнале нет ни просмотра, ни печати,
     * и кассир видел одну красную плашку. Причина у кассы есть — она
     * доходит до строки и до поиска по ней.
     */
    @Test
    fun `отклонённый документ кассы называет причину отказа словами кассира`() {
        val refused = document("r", status = "FAILED").copy(ofdErrorCode = 17, ofdErrorText = "Same taxpayer")

        val entry = entries(refused).single()

        assertEquals(
            textsOf(Language.Ru).journal.ofdRefusal.words(17) + Glyphs.SEPARATOR + "${texts.common.refusalCode} 17",
            entry.refusal
        )
        assertTrue(entry.searchable.contains("17"), "по причине отказа строка обязана находиться")
    }

    @Test
    fun `незнакомый код отказа доходит пояснением БФД, а знакомого текста хватает своего`() {
        val strange = document("s", status = "FAILED").copy(ofdErrorCode = 777, ofdErrorText = "Odd refusal")
        val quiet = document("q")

        val rows = entries(strange, quiet).associateBy { it.key }

        assertEquals("Odd refusal${Glyphs.SEPARATOR}${texts.common.refusalCode} 777", rows["s"]?.refusal)
        assertNull(rows["q"]?.refusal, "у принятого документа причины отказа нет")
    }

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

    @Test
    fun `один и тот же чек находится и у кассы, и в кабинете одним поиском`() {
        val document = document("d-1", amount = 450_084).copy(docNo = 12, createdAt = 1_788_807_779_000, shiftNo = 3)
        val receipt = CabinetReceipt(
            transactionId = "t-1",
            receiptNumber = "12",
            shiftNumber = 3,
            operationType = "SALE",
            total = Decimal.parse("4500.84"),
            createdAt = "2026-09-07T19:02:59Z",
            deliveryStatus = "ONLINE_OK"
        )
        val query = JournalQuery(search = "4500.84", shiftNo = 3)

        assertEquals(1, entries(document).select(query).size)
        assertEquals(1, listOf(receiptRow(receipt, cabinet).entry).select(query).size)
    }

    @Test
    fun `день срока — местные сутки, а не отрезок от сейчас`() {
        val day = JournalPeriod.of(JournalSpan.Day, LocalDate(2026, 9, 7))
        val window = requireNotNull(day.range)

        assertEquals(LocalDate(2026, 9, 7), window.from)
        assertEquals(LocalDate(2026, 9, 7), window.to)
        assertTrue(window.oneDay)
        assertEquals(1L, window.days)
    }

    @Test
    fun `окно перелистывается на свою длину`() {
        val week = JournalPeriod.of(JournalSpan.Week, LocalDate(2026, 9, 14))
        val earlier = requireNotNull(week.shiftedBy(-1).range)

        assertEquals(LocalDate(2026, 9, 1), earlier.from)
        assertEquals(LocalDate(2026, 9, 7), earlier.to)
        assertEquals(7L, earlier.days)
    }
}
