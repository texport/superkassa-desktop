package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.print.port.PrintChoices

/** Сколько копий печатать: второй экземпляр нужен там, где чек подшивают. */
class ChooseCopies(private val choices: PrintChoices) {

    /** @return сколько копий записано: больше [PrintChoices.MAX_COPIES] чек не печатают. */
    operator fun invoke(copies: Int): Int {
        choices.copies = copies
        return choices.copies
    }
}
