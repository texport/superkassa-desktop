package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.port.PrintChoices

/** В каком виде сохранять форму в файл: выбор машины, а не экрана. */
class ReadFileKind(private val choices: PrintChoices) {

    operator fun invoke(): PrintKind = choices.kind()
}
