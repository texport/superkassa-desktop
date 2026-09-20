package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.log.HIDDEN
import kz.mybrain.superkassa.desktop.app.log.LogFile
import kz.mybrain.superkassa.desktop.app.log.LogJournal
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.app.log.LogSource
import kz.mybrain.superkassa.desktop.app.log.hideSecrets
import kz.mybrain.superkassa.desktop.app.log.matching
import java.io.File
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Журнал приложения.
 *
 * Проверяется то, из-за чего журнал стал бы вреден: тайное в файле,
 * который владелец пересылает в поддержку; файл, растущий до конца диска;
 * и отбор, показывающий не то, что выбрано.
 */
class AppLogTest {

    private val moment = LocalDateTime.of(2026, 9, 19, 12, 30, 15)

    private fun journal(level: LogLevel = LogLevel.Debug, file: LogFile? = null) =
        LogJournal(file = file, level = level, clock = { moment })

    /**
     * Пин даёт право на фискальные команды, токен — на обмен с ОФД,
     * подпись равна подписанному заявлению. В пересланном файле любое
     * из них означает выданный чужому доступ.
     */
    @Test
    fun `пин, токен и подпись не попадают в журнал`() {
        val log = journal()
        log.record(
            source = LogSource.Node,
            level = LogLevel.Info,
            text = "POST /api/v1/tickets",
            body = """{"pin":"1234","ofdToken":"9f2c","signature":"MIIFxAYJ","name":"Нан"}"""
        )

        val written = log.entries.single().body.orEmpty()
        assertFalse(written.contains("1234"), "пин кассира: $written")
        assertFalse(written.contains("9f2c"), "токен кассы: $written")
        assertFalse(written.contains("MIIFxAYJ"), "подпись: $written")
        assertTrue(written.contains("Нан"), "товар обязан остаться: $written")
        assertTrue(written.contains(HIDDEN), "поле должно остаться видимым, скрыто только значение")
    }

    /**
     * Вырезается и из самого текста записи: путь обращения и сообщение
     * об отказе собираются где угодно, и пароль ЭЦП попадал бы в них
     * мимо тела.
     */
    @Test
    fun `пароль ЭЦП, доступ и данные покупателя вырезаются из текста`() {
        val hidden = hideSecrets(
            """POST /sign password=qwerty123, Authorization: Bearer eyJhbGciOi, """ +
                """{"customerIin":"901231300123","phone":"+77011234567","total":1500}"""
        )

        assertFalse(hidden.contains("qwerty123"), hidden)
        assertFalse(hidden.contains("eyJhbGciOi"), hidden)
        assertFalse(hidden.contains("901231300123"), hidden)
        assertFalse(hidden.contains("77011234567"), hidden)
        assertTrue(hidden.contains("1500"), "сумма чека нужна при разборе: $hidden")
    }

    /**
     * Тело — только на отладочном уровне: на обычной работе оно раздувает
     * файл килобайтами на каждый чек, а при разборе отказа нужно целиком.
     */
    @Test
    fun `тело запроса пишется только на отладочном уровне`() {
        val ordinary = journal(level = LogLevel.Info)
        ordinary.record(LogSource.Node, LogLevel.Info, "POST /tickets", body = """{"total":1500}""")
        assertNull(ordinary.entries.single().body, "на обычном уровне тела в журнале нет")

        val debug = journal(level = LogLevel.Debug)
        debug.record(LogSource.Node, LogLevel.Info, "POST /tickets", body = """{"total":1500}""")
        assertTrue(debug.entries.single().body.orEmpty().contains("1500"))
    }

    /** Запись ниже выбранного порога не пишется вовсе — ни в память, ни в файл. */
    @Test
    fun `запись ниже выбранного уровня не заводится`() {
        val log = journal(level = LogLevel.Warning)

        log.record(LogSource.Node, LogLevel.Info, "GET /kkm -> 200")
        log.record(LogSource.Node, LogLevel.Failure, "GET /kkm — нет ответа")

        assertEquals(1, log.entries.size)
        assertEquals(LogLevel.Failure, log.entries.single().level)
    }

    /** Отбор в окне: уровень не ниже выбранного и совпадение с набранным. */
    @Test
    fun `отбор оставляет уровень не ниже выбранного и совпавшее со строкой`() {
        val log = journal()
        log.record(LogSource.Node, LogLevel.Debug, "GET /dictionaries -> 200")
        log.record(LogSource.Node, LogLevel.Info, "POST /tickets -> 200")
        log.record(LogSource.Cabinet, LogLevel.Warning, "POST /registers -> 409")
        log.record(LogSource.Node, LogLevel.Failure, "POST /tickets — нет ответа")

        assertEquals(4, log.entries.matching(LogLevel.Debug, "").size)
        assertEquals(3, log.entries.matching(LogLevel.Info, "").size)
        assertEquals(2, log.entries.matching(LogLevel.Warning, "").size)
        assertEquals(2, log.entries.matching(LogLevel.Debug, "tickets").size)
        assertEquals(1, log.entries.matching(LogLevel.Failure, "tickets").size)
        assertEquals(1, log.entries.matching(LogLevel.Debug, "cabinet").size)
        assertEquals(0, log.entries.matching(LogLevel.Debug, "не бывало такого").size)
    }

    /**
     * Касса работает годами, журнал пишется каждую смену: без ограничения
     * он однажды занял бы диск целиком, и заметил бы это кассир посреди
     * продажи.
     */
    @Test
    fun `файл журнала не растёт бесконечно`() {
        val directory = File.createTempFile("log", "").also { it.delete() }
        directory.mkdirs()
        val file = LogFile(directory, maxBytes = MAX_BYTES, keep = KEEP)

        repeat(TIMES) { at -> file.append("2026-09-19 12:30:15.000 INFO node GET /kkm -> 200 ($at)") }

        assertTrue(file.current.length() <= MAX_BYTES, "текущий файл: ${file.current.length()} байт")
        assertEquals(KEEP + 1, file.files().size, "текущий файл и ${KEEP} прошлых")
        assertEquals(KEEP + 1, directory.listFiles().orEmpty().size, "лишних файлов в папке нет")
        val total = file.files().sumOf { it.length() }
        assertTrue(total <= MAX_BYTES * (KEEP + 1), "весь журнал: $total байт")
        directory.deleteRecursively()
    }

    /** Записанное в файл читается строкой: время, уровень, источник, текст. */
    @Test
    fun `строка файла начинается со времени, уровня и источника`() {
        val directory = File.createTempFile("log-line", "").also { it.delete() }
        directory.mkdirs()
        val file = LogFile(directory)

        journal(file = file).record(LogSource.Cabinet, LogLevel.Warning, "POST /registers -> 409")

        val line = file.current.readText().trim()
        assertEquals("2026-09-19 12:30:15.000 WARNING cabinet POST /registers -> 409", line)
        directory.deleteRecursively()
    }

    private companion object {
        const val MAX_BYTES = 300L
        const val KEEP = 2
        const val TIMES = 200
    }
}
