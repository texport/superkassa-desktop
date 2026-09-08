package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.history.Shift
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Прошлые смены: Z-отчёт есть только у закрытой.
 *
 * Кнопка печати у незакрытой смены дала бы кассиру отказ узла вместо
 * бумаги — а он в этот момент ищет отчёт, а не разбирается с кодами.
 */
class JournalShiftTest {

    @Test
    fun `ответ узла о сменах разбирается целиком`() {
        val body = """
            [{"id":"s-1","kkmId":"k-1","shiftNo":12,"status":"CLOSED","openedAt":1700000000000,
              "closedAt":1700030000000,"openDocumentId":"d-open","closeDocumentId":"d-close"}]
        """.trimIndent()

        val shifts = ServerClient.lenientJson.decodeFromString<List<Shift>>(body)

        assertEquals(1, shifts.size)
        assertEquals(12, shifts.single().shiftNo)
        assertEquals("d-close", shifts.single().zReportId)
    }

    @Test
    fun `у открытой смены Z-отчёта нет`() {
        val open = Shift(id = "s-2", shiftNo = 13, status = "OPEN", openedAt = 1700000000000)

        assertTrue(!open.isClosed)
        assertNull(open.zReportId)
    }

    @Test
    fun `закрытая смена без документа закрытия отчёта тоже не обещает`() {
        val broken = Shift(id = "s-3", shiftNo = 14, status = "CLOSED", closeDocumentId = null)

        assertTrue(broken.isClosed)
        assertNull(broken.zReportId)
    }
}
