package kz.mybrain.superkassa.data.log

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kz.mybrain.superkassa.KeptFiles
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource
import kotlin.test.Test
import kotlin.test.assertEquals

/** Журнал рабочего места как книга окна отладки: потоком, без Compose. */
class AppLogBookTest {

    private val moment = LocalDateTime(2026, 9, 19, 12, 30, 15)

    private fun journal(level: LogLevel) = LogJournal(level = level, clock = { moment })

    /**
     * Книга журнала следит за записями потоком, без Compose.
     *
     * Слой данных держал строки в состоянии Compose: окно отладки видело их
     * только внутри композиции, а проверке без отрисовки было не на что смотреть.
     */
    @Test
    fun `книга видит новую запись и подменённый журнал без отрисовки`() = runBlocking {
        val was = AppLog.journal
        try {
            AppLog.journal = journal(level = LogLevel.Info)
            AppLog.record(LogSource.App, LogLevel.Info, "first")
            assertEquals(listOf("first"), AppLogBook(KeptFiles()).state.first().entries.map { it.text })

            AppLog.journal = journal(level = LogLevel.Warning)
            assertEquals(LogLevel.Warning, AppLogBook(KeptFiles()).state.first().level)
        } finally {
            AppLog.journal = was
        }
    }

    @Test
    fun `в памяти остаются последние строки`() {
        val log = LogJournal(capacity = 2, level = LogLevel.Info, clock = { moment })
        listOf("a", "b", "c").forEach { log.record(LogSource.App, LogLevel.Info, it) }
        assertEquals(listOf("b", "c"), log.lines.value.map { it.text })
    }

    /** Показанные строки уходят в файл, место которому выбирает владелец, — строками журнала. */
    @Test
    fun `сохранённый журнал — те же строки, что в окне`() = runBlocking {
        val files = KeptFiles()
        val log = journal(level = LogLevel.Info)
        log.record(LogSource.Cabinet, LogLevel.Warning, "POST /registers -> 409")

        AppLogBook(files).save(log.entries, "")

        val saved = files.saved.getValue("superkassa-log.txt").decodeToString()
        assertEquals("2026-09-19 12:30:15.000 WARNING cabinet POST /registers -> 409", saved)
    }
}
