package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.PrintFileName
import kz.mybrain.superkassa.desktop.server.Document
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Имя файла печатной формы.
 *
 * В окно сохранения подставлялся идентификатор документа — тридцать шесть
 * знаков с дефисами. Такой файл не найти через неделю и не показать
 * в КГД: имя обязано называть вид документа и его номер.
 */
class PrintFileNameTest {

    @Test
    fun `чек продажи назван видом, сменой и фискальным признаком`() {
        val name = PrintFileName.of(
            Document(id = "069fa609-b2ba", docType = "SALE", shiftNo = 9, fiscalSign = "3286317477")
        )

        assertEquals("receipt-sale-shift-9-3286317477", name)
    }

    @Test
    fun `автономный документ назван своим признаком`() {
        val name = PrintFileName.of(
            Document(id = "x", docType = "BUY_RETURN", shiftNo = 4, autonomousSign = "1789000000000")
        )

        assertEquals("receipt-buy-return-shift-4-1789000000000", name)
    }

    @Test
    fun `отчёт без признака назван видом и сменой`() {
        assertEquals("z-report-shift-7", PrintFileName.of(Document(id = "x", docType = "Z_REPORT", shiftNo = 7)))
        assertEquals("x-report-shift-7", PrintFileName.of(Document(id = "x", docType = "X_REPORT", shiftNo = 7)))
    }

    @Test
    fun `документ кабинета назван видом и номером из строки журнала`() {
        assertEquals(
            "receipt-buy-shift-9-3286317477",
            PrintFileName.of(typeCode = "BUY", number = "3286317477", shiftNo = 9)
        )
    }

    @Test
    fun `незнакомый вид не роняет имя`() {
        assertEquals("document", PrintFileName.of(Document(id = "x", docType = "ЧТО-ТО")))
        assertEquals("document", PrintFileName.of(typeCode = null, number = null, shiftNo = null))
    }
}
