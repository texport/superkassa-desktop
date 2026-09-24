package kz.mybrain.superkassa.domain.print.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.model.PrintRoute
import kz.mybrain.superkassa.domain.print.model.Printed
import kz.mybrain.superkassa.domain.print.port.PrintChoices
import kz.mybrain.superkassa.domain.print.port.PrintOut

/**
 * Печать готовой ленты на принтер кассы.
 *
 * Принтер задан в настройках, а пока не задан — системный по умолчанию:
 * кассир не выбирает принтер на каждый чек. Лента печатается шириной,
 * заданной у кассы, а ноль ширины — по ширине страницы.
 */
class PrintTape(private val printOut: PrintOut, private val choices: PrintChoices) {

    /** Чем кончилась печать. */
    enum class Result {
        Sent,

        /** Принтер задания не принял. */
        Refused,

        /**
         * Принтера на машине нет вовсе: рабочее место ставят раньше, чем
         * подключают чековый, и «не принял задание» было бы неправдой.
         */
        NoPrinter,

        /**
         * Принтера, выбранного для кассы, в системе больше нет: его отключили
         * или удалили. Лента не уходит на системный по умолчанию — это
         * конторский принтер, и чек на листе A4 кассир принял бы за сбой
         * чекового, а не за свою настройку.
         */
        PrinterGone,

        /** Кассир закрыл системный диалог печати: он передумал. */
        Cancelled
    }

    /**
     * Есть ли куда печатать вообще.
     *
     * Спрашивается до рисования: рисовать форму ради отказа «принтера нет»
     * незачем — на Android рисование идёт через WebView и занимает секунды.
     */
    suspend fun hasPrinter(): Boolean = printOut.route == PrintRoute.SystemDialog || printOut.printers().isNotEmpty()

    /** В каком виде форму рисовать для печати: ленту картинкой или документ для системного диалога. */
    val kind: PrintKind get() = printOut.route.kind

    /**
     * @param tape форма в виде [kind].
     *
     * Системный диалог выбирает принтер и копии сам: принтера кассы и копий
     * у него не спрашивают.
     */
    suspend operator fun invoke(kkm: KkmResponse, tape: ByteArray): Result {
        val width = kkm.branding?.paperWidthMm ?: 0
        if (printOut.route == PrintRoute.SystemDialog) return resultOf(printOut.print(tape, null, width, 1))
        val printers = printOut.printers()
        val printer = choices.printer(kkm.kkmId)
        return when {
            printers.isEmpty() -> Result.NoPrinter
            printer != null && printer !in printers -> Result.PrinterGone
            else -> resultOf(printOut.print(tape, printer, width, choices.copies))
        }
    }

    private fun resultOf(printed: Printed): Result = when (printed) {
        Printed.Sent -> Result.Sent
        Printed.Refused -> Result.Refused
        Printed.Cancelled -> Result.Cancelled
    }
}
