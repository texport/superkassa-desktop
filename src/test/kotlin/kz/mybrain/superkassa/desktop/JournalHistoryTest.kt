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
import kz.mybrain.superkassa.desktop.ui.history.dayRange
import kz.mybrain.superkassa.desktop.ui.history.documentTypeTitle
import kz.mybrain.superkassa.desktop.ui.history.documentTypesIn
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
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
