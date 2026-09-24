package kz.mybrain.superkassa.domain.print.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingResponse
import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.model.PrintRoute
import kz.mybrain.superkassa.domain.print.model.Printed
import kz.mybrain.superkassa.domain.print.port.PrintOut
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.settings.FakePrintOut
import kz.mybrain.superkassa.presentation.settings.MemoryPrintChoices
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Печать системным диалогом — как на Android.
 *
 * Принтеров по имени машина не видит, и это не «принтера нет»: принтер,
 * копии и «Сохранить как PDF» выбирают в диалоге. Форма уходит туда
 * документом PDF, а закрытый диалог — не отказ.
 */
class SystemDialogPrintTest {
    private val printer = FakePrintOut(names = emptyList())
    private val out = object : PrintOut by printer {
        override val route = PrintRoute.SystemDialog
    }
    private val choices = MemoryPrintChoices(copies = 3).apply { choosePrinter("kkm-1", "Чековый у кассы") }
    private val print = PrintTape(out, choices)
    private val pdf = byteArrayOf(0x25, 0x50, 0x44, 0x46)

    @Test
    fun `печатать есть куда и без принтеров по имени, и форма рисуется документом`(): Unit = runTest {
        assertTrue(print.hasPrinter(), "системный диалог принят за отсутствие принтера")
        assertEquals(PrintKind.Pdf, print.kind)
    }

    @Test
    fun `в диалог уходит документ шириной кассы, без принтера кассы и копий`(): Unit = runTest {
        val kkm = CoreScene.kkm().copy(branding = ReceiptBrandingResponse(paperWidthMm = 58))

        assertEquals(PrintTape.Result.Sent, print(kkm, pdf))

        val job = printer.printed.single()
        assertNull(job.printer, "принтер кассы навязан системному диалогу")
        assertEquals(listOf(58, 1), listOf(job.widthMm, job.copies))
    }

    @Test
    fun `закрытый диалог — не отказ, а передумавший кассир`(): Unit = runTest {
        val cancelling = object : PrintOut by out {
            override suspend fun print(tape: ByteArray, printer: String?, widthMm: Int, copies: Int) = Printed.Cancelled
        }

        assertEquals(PrintTape.Result.Cancelled, PrintTape(cancelling, choices)(CoreScene.kkm(), pdf))
    }

    @Test
    fun `настольная машина печатает лентой, как прежде`() {
        assertEquals(PrintKind.Png, PrintTape(printer, choices).kind)
    }
}
