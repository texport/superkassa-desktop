package kz.mybrain.superkassa.presentation.journal

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.common.document.JournalDelivery
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kz.mybrain.superkassa.presentation.journal.documents.journalEntriesOf
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Документы кассы — строками общего журнала, и срок журнала.
 *
 * Показ, поиск и отбор журнала написаны один раз, а касса приводит к ним
 * свои документы: состояние доставки она называет своими кодами, а сумму
 * и время отдаёт своими типами. Те же строки из кабинета проверяет
 * кабинет, общий поиск по обоим источникам — каркас.
 */
class JournalSourcesTest {

    private val texts = textsOf(Language.Ru).common
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
