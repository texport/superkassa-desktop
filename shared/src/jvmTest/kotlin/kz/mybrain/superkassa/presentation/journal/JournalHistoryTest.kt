package kz.mybrain.superkassa.presentation.journal

import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kotlinx.datetime.LocalDate
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kz.mybrain.superkassa.presentation.common.period.dayRange
import kz.mybrain.superkassa.presentation.journal.documents.documentTypeTitle
import kz.mybrain.superkassa.presentation.journal.documents.documentTypesIn
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Журнал: границы дня и отбор по типу документа.
 *
 * День берётся местный: кассир ищет свои вчерашние чеки, а не отрезок
 * суток, сдвинутый на часовой пояс сервера.
 */
class JournalHistoryTest {

    @Test
    fun `день кончается началом следующего, а не суткой назад от сейчас`() {
        // Пояс задан смещением, а не именем: часовые пояса Казахстана
        // уже переносили, и тест о границах дня не должен падать от этого.
        val range = dayRange(LocalDate(2026, 8, 31), UtcOffset(hours = 5).asTimeZone())

        assertEquals(Instant.parse("2026-08-30T19:00:00Z").toEpochMilli(), range.fromMillis)
        assertEquals(Instant.parse("2026-08-31T19:00:00Z").toEpochMilli(), range.toMillis)
    }

    @Test
    fun `типы идут в порядке справочника, а незнакомый — в конец`() {
        val order = listOf("SHIFT_OPEN", "SALE", "RETURN", "CASH_IN")
        val codes = listOf("CASH_IN", "CHECK", "SALE", "SALE", "SHIFT_OPEN")

        assertEquals(
            listOf("SHIFT_OPEN", "SALE", "CASH_IN", "CHECK"),
            documentTypesIn(codes, order),
            "снятый с учёта тип не теряется, но и вперёд справочника не лезет"
        )
    }

    @Test
    fun `название типа берётся у кассы, а снятый с учёта CHECK показан словом`() {
        val names = mapOf("SALE" to TrilingualMessageResponse("Продажа", "Сатылым", "Sale"))
        val texts = textsOf(Language.Ru).common

        assertEquals("Продажа", documentTypeTitle("SALE", names, Language.Ru, texts.enums))
        assertEquals("Сатылым", documentTypeTitle("SALE", names, Language.Kk, texts.enums))
        assertEquals(
            texts.enums.docCheck,
            documentTypeTitle("CHECK", names, Language.Ru, texts.enums),
            "кассир читает чек, а не протокол"
        )
        assertEquals("—", documentTypeTitle(null, names, Language.Ru, texts.enums))
    }
}
