package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCashMovement
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReceipt
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReport
import kz.mybrain.superkassa.desktop.ui.cabinet.movementRow
import kz.mybrain.superkassa.desktop.ui.cabinet.receiptRow
import kz.mybrain.superkassa.desktop.ui.cabinet.reportRow
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
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

    private val texts = cabinetTexts(Language.Ru)

    @Test
    fun `отвергнутый чек кабинета не открывается печатной формой`() {
        val refused = CabinetReceipt(transactionId = "t-1", deliveryStatus = "DELIVERY_ERROR")

        assertFalse(receiptRow(refused, texts).entry.printable, "отвергнутый чек выдан за фискальный")
    }

    @Test
    fun `отвергнутый отчёт и отвергнутое движение денег тоже не печатаются`() {
        val report = CabinetReport(transactionId = "r-1", sendStatus = "SEND_FAIL")
        val movement = CabinetCashMovement(transactionId = "m-1", sendStatus = "REJECTED")

        assertFalse(reportRow(report, texts).entry.printable, "отвергнутый отчёт выдан за фискальный")
        assertFalse(movementRow(movement, texts).entry.printable, "отвергнутое движение выдано за фискальное")
    }

    /**
     * Чек, пробитый без связи, печатается: он фискальный, просто ещё
     * не доставлен. Смешать его с отказом значило бы отнять печатную
     * форму у половины смены на плохой связи.
     */
    @Test
    fun `принятый и ждущий отправки чеки печатаются по-прежнему`() {
        val delivered = CabinetReceipt(transactionId = "t-2", deliveryStatus = "ONLINE_OK")
        val queued = CabinetReceipt(transactionId = "t-3", sendStatus = "OFFLINE_QUEUED")

        assertTrue(receiptRow(delivered, texts).entry.printable)
        assertTrue(receiptRow(queued, texts).entry.printable)
    }

    /** Та же мера у своих документов узла: обе таблицы решают это одинаково. */
    @Test
    fun `свой отвергнутый документ печатным не считается`() {
        assertFalse(Document(id = "d-1", ofdStatus = "FAILED").printable)
        assertTrue(Document(id = "d-2", ofdStatus = "SENT").printable)
    }
}
