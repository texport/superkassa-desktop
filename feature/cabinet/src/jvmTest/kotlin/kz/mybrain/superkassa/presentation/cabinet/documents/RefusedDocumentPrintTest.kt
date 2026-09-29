package kz.mybrain.superkassa.presentation.cabinet.documents

import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetCashMovement
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceipt
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReport
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * У документа, отвергнутого БФД, печатной формы нет.
 *
 * Фискальным он не стал: его нет ни в БФД, ни в отчётности. Печатная
 * форма при этом выглядит как настоящий чек — с номером, признаком
 * и QR-кодом, — и покупатель принимает её за подтверждение покупки.
 *
 * Журнал кассы так и делает со своими документами. Журнал кабинета —
 * та же таблица и те же кнопки — считал печатным любой документ,
 * и отвергнутый чек открывался и уходил на принтер наравне с принятым.
 */
class RefusedDocumentPrintTest {

    private val texts = textsOf(Language.Ru).cabinet

    @Test
    fun `чек, отклонённый или не полученный КГД, не открывается печатной формой`() {
        listOf("REJECTED", "FAILED").forEach { code ->
            val refused = CabinetReceipt(transactionId = "t-$code", deliveryStatus = code)
            assertFalse(receiptRow(refused, texts).entry.printable, "чек под вопросом у КГД печатается: $code")
        }
    }

    @Test
    fun `Z-отчёт, отклонённый КГД, тоже не печатается, а движение денег печатается всегда`() {
        val report = CabinetReport(transactionId = "r-1", type = "Z", deliveryStatus = "REJECTED")
        val movement = CabinetCashMovement(transactionId = "m-1", type = "DEPOSIT", sendStatus = "FAILED")

        assertFalse(reportRow(report, texts).entry.printable, "отклонённый Z-отчёт выдан за фискальный")
        assertTrue(movementRow(movement, texts).entry.printable, "движение денег в КГД не уходит")
    }

    /**
     * Чек в пути печатается: он фискальный, просто ещё не доставлен.
     * Ошибка передачи службе — тоже: её повторят, и отнимать у чека
     * печатную форму из-за временного сбоя значило бы оставить
     * покупателя без чека на плохой связи.
     */
    @Test
    fun `принятый, ждущий и чек с ошибкой передачи печатаются`() {
        val delivered = CabinetReceipt(transactionId = "t-2", deliveryStatus = "DELIVERED")
        val queued = CabinetReceipt(transactionId = "t-3", sendStatus = "ACCEPTED")
        val retried = CabinetReceipt(transactionId = "t-4", sendStatus = "FAILED")

        listOf(delivered, queued, retried).forEach { receipt ->
            assertTrue(receiptRow(receipt, texts).entry.printable, receipt.transactionId)
        }
    }
}
