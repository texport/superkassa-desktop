package kz.mybrain.superkassa.presentation.shift.dashboard

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.domain.shift.model.zReportId
import kz.mybrain.superkassa.presentation.common.print.PrintActions
import kz.mybrain.superkassa.presentation.common.print.PrintFileName

/**
 * Что кассир может сделать на главном экране.
 *
 * Экран получает действия готовыми и сам ничего не решает: кнопка зовёт
 * действие, модель делает работу. Действия по умолчанию пустые — для
 * снимков вида, где нажимать некому.
 */
interface DashboardActions {
    fun refresh() = Unit

    fun openShift() = Unit

    /** Z-отчёт: экран спрашивает до вызова, он не отменяется. */
    fun closeShift() = Unit

    fun xReport() = Unit

    fun checkLink() = Unit

    fun sendQueued() = Unit

    /** Показать печатную форму документа на экране. */
    fun preview(document: FiscalDocumentResponse) = Unit

    /** Отправить печатную форму на принтер рабочего места. */
    fun print(document: FiscalDocumentResponse) = Unit

    /** Показать Z-отчёт закрытой смены. */
    fun zReport(shift: ShiftResponse) = Unit
}

/** Действия экрана, выполняемые этой моделью; печать — у окна. */
internal fun DashboardViewModel.actions(print: PrintActions): DashboardActions {
    val model = this
    return object : DashboardActions {
        override fun refresh() = model.refresh()

        override fun openShift() = model.openShift()

        override fun closeShift() = model.closeShift()

        override fun xReport() = model.xReport()

        override fun checkLink() = model.checkLink()

        override fun sendQueued() = model.sendQueued()

        override fun preview(document: FiscalDocumentResponse) = print.preview(document)

        override fun print(document: FiscalDocumentResponse) = print.print(document.id)

        override fun zReport(shift: ShiftResponse) =
            print.preview(shift.zReportId, PrintFileName.zReport(shift.shiftNo))
    }
}
