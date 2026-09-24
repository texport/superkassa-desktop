package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.port.PrintChoices

/** В каком виде сохранять форму: PDF — покупателю, HTML — в бухгалтерию, картинка — как на экране. */
class ChoosePrintKind(private val choices: PrintChoices) {

    operator fun invoke(kind: PrintKind) = choices.chooseKind(kind)
}
