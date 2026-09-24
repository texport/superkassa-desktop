package kz.mybrain.superkassa.domain.print.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
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
        PrinterGone
    }

    /**
     * Есть ли куда печатать вообще.
     *
     * Спрашивается до рисования: рисовать форму ради отказа «принтера нет»
     * незачем — на Android рисование идёт через WebView и занимает секунды.
     */
    suspend fun hasPrinter(): Boolean = printOut.printers().isNotEmpty()

    suspend operator fun invoke(kkm: KkmResponse, tape: ByteArray): Result {
        val printers = printOut.printers()
        val printer = choices.printer(kkm.kkmId)
        return when {
            printers.isEmpty() -> Result.NoPrinter
            printer != null && printer !in printers -> Result.PrinterGone
            printOut.print(tape, printer, kkm.branding?.paperWidthMm ?: 0, choices.copies) -> Result.Sent
            else -> Result.Refused
        }
    }
}
