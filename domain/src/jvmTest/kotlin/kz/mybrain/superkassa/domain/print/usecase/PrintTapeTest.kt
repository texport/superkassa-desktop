package kz.mybrain.superkassa.domain.print.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingResponse
import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.domain.print.port.FakePrintOut
import kz.mybrain.superkassa.domain.print.port.MemoryPrintChoices
import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Печать ленты без экрана: принтер, ширина, копии и честный ответ о принтере. */
class PrintTapeTest {

    private val out = FakePrintOut()
    private val choices = MemoryPrintChoices(copies = 2)
    private val print = PrintTape(out, choices)
    private val tape = byteArrayOf(1, 2, 3)

    @Test
    fun `лента уходит на выбранный принтер шириной кассы и выбранным числом копий`(): Unit = runTest {
        choices.choosePrinter("kkm-1", "Чековый у кассы")
        val kkm = CoreScene.kkm().copy(branding = ReceiptBrandingResponse(paperWidthMm = 58))

        assertEquals(PrintTape.Result.Sent, print(kkm, tape))
        val job = out.printed.single()
        assertEquals(listOf<Any?>("Чековый у кассы", 58, 2), listOf(job.printer, job.widthMm, job.copies))
        assertContentEquals(tape, job.tape)
    }

    @Test
    fun `без заданной ширины лента печатается по ширине страницы`(): Unit = runTest {
        print(CoreScene.kkm(), tape)

        assertEquals(0, out.printed.single().widthMm)
        assertNull(out.printed.single().printer, "без выбора печатает системный принтер")
    }

    @Test
    fun `принтер не принял задание — так и сказано`(): Unit = runTest {
        out.accepts = false

        assertEquals(PrintTape.Result.Refused, print(CoreScene.kkm(), tape))
    }

    @Test
    fun `на машине без принтера задание не отправляется`(): Unit = runTest {
        out.names = emptyList()

        assertEquals(PrintTape.Result.NoPrinter, print(CoreScene.kkm(), tape))
        assertTrue(out.printed.isEmpty())
    }

    /**
     * Чековый принтер отключили или удалили из системы. Прежде лента молча
     * уходила на системный по умолчанию — конторский, на лист A4, — а кассиру
     * говорилось «напечатано».
     */
    @Test
    fun `выбранного принтера нет в системе — задание не уходит на чужой`(): Unit = runTest {
        choices.choosePrinter("kkm-1", "Чековый у кассы")
        out.names = listOf("Конторский A4")

        assertEquals(PrintTape.Result.PrinterGone, print(CoreScene.kkm(), tape))
        assertTrue(out.printed.isEmpty(), "лента ушла на другой принтер")
    }
}
