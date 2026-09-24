package kz.mybrain.superkassa.presentation.print.target

import kz.mybrain.superkassa.domain.print.port.PrintChoices
import kz.mybrain.superkassa.domain.print.port.PrintOut
import kz.mybrain.superkassa.domain.print.usecase.ChooseCopies
import kz.mybrain.superkassa.domain.print.usecase.ChoosePrintKind
import kz.mybrain.superkassa.domain.print.usecase.ChoosePrinter
import kz.mybrain.superkassa.domain.print.usecase.ReadPrintTarget
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn

/** Сценарии принтера кассы: чей принтер показан, что на машине и что выбрано. */
class PrintTargetCases(signIn: SignIn, printOut: PrintOut, choices: PrintChoices) {
    val observe = ObserveSignIn(signIn)
    val read = ReadPrintTarget(printOut, choices)
    val printer = ChoosePrinter(choices)
    val copies = ChooseCopies(choices)
    val kind = ChoosePrintKind(choices)
}
