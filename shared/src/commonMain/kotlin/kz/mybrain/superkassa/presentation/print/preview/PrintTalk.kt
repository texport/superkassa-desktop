package kz.mybrain.superkassa.presentation.print.preview

import kz.mybrain.superkassa.domain.print.model.Kept
import kz.mybrain.superkassa.domain.print.usecase.PrintTape
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kz.mybrain.superkassa.presentation.strings.print.printTexts

/**
 * Итог печати словами кассира.
 *
 * Не ушедшее на принтер — отказ, а не итог: «на машине нет принтера»,
 * показанное как сделанное, кассир принимал за напечатанный чек.
 * Закрытый системный диалог — не отказ: кассир передумал сам.
 */
internal fun Talk.printed(result: PrintTape.Result, action: String) {
    val texts = stringsOf(language()).preview
    when (result) {
        PrintTape.Result.Sent -> done(texts.printSent, action)
        PrintTape.Result.Refused -> refuse(action, texts.printFailed, PRINTER_REFUSED)
        PrintTape.Result.NoPrinter -> refuse(action, texts.printerMissing, NO_PRINTER)
        PrintTape.Result.PrinterGone -> refuse(action, printTexts(language()).printerGone, PRINTER_GONE)
        PrintTape.Result.Cancelled -> Unit
    }
}

/** Итог сохранения формы: передумавшему владельцу говорить не о чем, а «некуда» — сказать. */
internal fun Talk.kept(kept: Kept, action: String) {
    when (kept) {
        is Kept.Saved -> done("${stringsOf(language()).preview.saved}: ${kept.name}", action)
        Kept.Cancelled -> Unit
        Kept.Unavailable -> refuse(action, printTexts(language()).keepUnavailable, NO_FILES)
    }
}

/** Отказ этой машины: кассиру — словами, журналу — кодом. */
private fun Talk.refuse(action: String, words: String, code: String) {
    journal.warn("$action: refused $code")
    say(action, Message.Refusal(words, code))
}

/** Коды отказов печати — для журнала и строки, не для кассира. */
private const val PRINTER_REFUSED = "PRINTER_REFUSED"
private const val NO_PRINTER = "NO_PRINTER"
private const val PRINTER_GONE = "PRINTER_GONE"
private const val NO_FILES = "NO_FILES"
