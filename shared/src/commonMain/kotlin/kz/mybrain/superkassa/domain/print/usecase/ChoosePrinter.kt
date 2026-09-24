package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.print.port.PrintChoices

/**
 * Принтер кассы.
 *
 * Держится за кассой: за одним компьютером их бывает две, и чековая лента
 * у каждой своя. Выбор действует сразу — отдельной кнопки у него нет.
 */
class ChoosePrinter(private val choices: PrintChoices) {

    /** @param name имя принтера; `null` — системный по умолчанию. */
    operator fun invoke(kkmId: String, name: String?) = choices.choosePrinter(kkmId, name)
}
