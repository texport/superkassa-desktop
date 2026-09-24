package kz.mybrain.superkassa.data.eds

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.data.log.LogJournal
import kz.mybrain.superkassa.data.log.LogLevel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Обмен с NCALayer в журнале: ход обмена виден, а подписи и содержимого нет. */
class EdsJournalTest {

    private val payload = "cGF5bG9hZA=="

    /**
     * Обмен виден в журнале на обычном уровне.
     *
     * Прежде между запросом задачи у кабинета и отказом в журнале не было
     * ни одной строки: чем кончился обмен, нельзя было понять ни владельцу,
     * ни поддержке. Подписи и содержимого для подписи в журнале при этом
     * быть не должно — владелец пересылает его целиком.
     */
    @Test
    fun `обмен записан в журнал, а подпись и содержимое в него не попали`() {
        val was = AppLog.journal
        AppLog.journal = LogJournal(level = LogLevel.Info)
        try {
            NcaFake { NcaReply.Frames(listOf(SIGNED)) }.use { fake ->
                runBlocking { NcaSigner(fake.address).sign(payload) }
            }
            val lines = AppLog.entries.map { it.text }

            assertTrue(lines.any { it.contains("handshake done") }, "$lines")
            assertTrue(lines.any { it.contains("$BASICS/sign") }, "модуль и метод: $lines")
            assertTrue(lines.any { it.contains("fields: status body") }, "состав кадра: $lines")
            assertTrue(lines.any { it.contains("answer received") }, "исход обмена: $lines")
            assertFalse(lines.any { it.contains("MIIC-signature") }, "подпись в журнале: $lines")
            assertFalse(lines.any { it.contains(payload) }, "содержимое для подписи в журнале: $lines")
        } finally {
            AppLog.journal = was
        }
    }

    private companion object {
        const val BASICS = "kz.gov.pki.knca.basics"
        const val SIGNED = """{"status":true,"body":{"result":["MIIC-signature"]}}"""
    }
}
