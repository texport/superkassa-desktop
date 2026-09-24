package kz.mybrain.superkassa.presentation.print.preview

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.print.port.PrintChoices
import kz.mybrain.superkassa.domain.print.port.PrintOut
import kz.mybrain.superkassa.domain.print.usecase.AsksPin
import kz.mybrain.superkassa.domain.print.usecase.DrawDocument
import kz.mybrain.superkassa.domain.print.usecase.FindDrawer
import kz.mybrain.superkassa.domain.print.usecase.KeepDocument
import kz.mybrain.superkassa.domain.print.usecase.NameDrawer
import kz.mybrain.superkassa.domain.print.usecase.PrintTape
import kz.mybrain.superkassa.domain.print.usecase.ReadFileKind
import kz.mybrain.superkassa.domain.print.usecase.RenderDocument
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Сценарии печати: кто рисует, чьим пином, как рисует, куда уходит форма.
 *
 * Рисовальщик ищется и без вошедшего кассира — владелец открывает кабинет
 * с экрана входа, — поэтому поиску нужен сам держатель входа, а рисованию
 * — только касса и пин того, кто за ней сидит.
 */
internal class PrintCases(
    kassa: Kassa,
    signIn: SignIn,
    printOut: PrintOut,
    choices: PrintChoices,
    memory: WorkplaceMemory
) {
    val observe = ObserveSignIn(signIn)
    val findDrawer = FindDrawer(kassa, signIn)
    val asksPin = AsksPin(signIn)
    val nameDrawer = NameDrawer(memory)
    val draw = DrawDocument(kassa, printOut, signIn)
    val render = RenderDocument(kassa, signIn)
    val print = PrintTape(printOut, choices)
    val keep = KeepDocument(printOut)
    val kind = ReadFileKind(choices)
}
