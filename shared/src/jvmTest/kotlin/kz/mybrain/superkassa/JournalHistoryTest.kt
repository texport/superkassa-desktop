package kz.mybrain.superkassa

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.data.node.Dictionary
import kz.mybrain.superkassa.data.node.DictionaryEntry
import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.data.node.ServerClient
import kz.mybrain.superkassa.kassa.DashboardScene
import kz.mybrain.superkassa.presentation.dashboard.documentAmount
import kz.mybrain.superkassa.presentation.history.dayRange
import kz.mybrain.superkassa.presentation.history.documentTypeTitle
import kz.mybrain.superkassa.presentation.history.documentTypesIn
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.strings.stringsOf
import kz.mybrain.superkassa.presentation.theme.Glyphs
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Журнал: границы дня и отбор по типу документа.
 *
 * День берётся местный: кассир ищет свои вчерашние чеки, а не отрезок
 * суток, сдвинутый на часовой пояс сервера.
 */
class JournalHistoryTest {

    private fun document(type: String?) = Document(id = "d-$type", docType = type)

    @Test
    fun `у отчёта и открытия смены на главной стоит прочерк, а не ноль`() {
        // Журнал за срок ставит на их месте прочерк, а список документов
        // смены рисовал «0,00 ₸» — кассир читал это как «не продано ничего».
        val report = Document(id = "d-x", docType = "X_REPORT", totalAmount = 0)
        val opened = Document(id = "d-o", docType = "SHIFT_OPEN", totalAmount = 0)
        val sale = Document(id = "d-s", docType = "SALE", totalAmount = 120_000)

        assertEquals(Glyphs.DASH, documentAmount(DashboardScene.core(report)))
        assertEquals(Glyphs.DASH, documentAmount(DashboardScene.core(opened)))
        assertEquals("1${Glyphs.NBSP}200,00${Glyphs.NBSP}₸", documentAmount(DashboardScene.core(sale)))
    }

    private fun session(): Session {
        val http = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })
        // Отдельный каталог: смена языка пишет файл рядом с настройками,
        // и в общем временном каталоге это задело бы чужие настройки.
        val directory = Files.createTempDirectory("journal").toFile()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
    }

    @Test
    fun `день кончается началом следующего, а не суткой назад от сейчас`() {
        // Пояс задан смещением, а не именем: часовые пояса Казахстана
        // уже переносили, и тест о границах дня не должен падать от этого.
        val range = dayRange(LocalDate.of(2026, 8, 31), ZoneOffset.ofHours(5))

        assertEquals(Instant.parse("2026-08-30T19:00:00Z").toEpochMilli(), range.fromMillis)
        assertEquals(Instant.parse("2026-08-31T19:00:00Z").toEpochMilli(), range.toMillis)
    }

    @Test
    fun `типы идут в порядке справочника, а незнакомый — в конец`() {
        val order = listOf("SHIFT_OPEN", "SALE", "RETURN", "CASH_IN")
        val documents = listOf(
            document("CASH_IN"),
            document("CHECK"),
            document("SALE"),
            document("SALE"),
            document("SHIFT_OPEN"),
            document(null)
        )

        assertEquals(
            listOf("SHIFT_OPEN", "SALE", "CASH_IN", "CHECK"),
            documentTypesIn(documents, order),
            "снятый с учёта тип не теряется, но и вперёд справочника не лезет"
        )
    }

    @Test
    fun `название типа берётся у узла, а снятый с учёта CHECK показан словом`() {
        val session = session()
        session.switchLanguage(Language.Ru)
        session.dictionaries[Dictionary.DocumentTypes] = listOf(
            DictionaryEntry("SALE", mapOf("ru" to "Продажа", "kk" to "Сатылым", "en" to "Sale"))
        )
        val texts = stringsOf(Language.Ru)

        assertEquals("Продажа", documentTypeTitle(session, texts, "SALE"))
        assertEquals(
            texts.enums.docCheck,
            documentTypeTitle(session, texts, "CHECK"),
            "кассир читает чек, а не протокол"
        )
        assertEquals("—", documentTypeTitle(session, texts, null))
    }
}
