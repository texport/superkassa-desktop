package kz.mybrain.superkassa.presentation.print.target

import kz.mybrain.superkassa.domain.print.model.PrintKind

/** Что владелец меняет в принтере кассы. Пустые действия — для снимков вида. */
interface PrintTargetActions {
    fun choosePrinter(name: String?) = Unit

    fun chooseCopies(copies: Int) = Unit

    fun chooseKind(kind: PrintKind) = Unit
}
