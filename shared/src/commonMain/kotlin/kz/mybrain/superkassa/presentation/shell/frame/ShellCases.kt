package kz.mybrain.superkassa.presentation.shell.frame

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn
import kz.mybrain.superkassa.domain.signin.usecase.RefreshKkm
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.domain.workplace.usecase.ReadLocalName

/** Сценарии каркаса окна: кто за кассой, как касса зовётся здесь и перечитать её. */
class ShellCases(kassa: Kassa, signIn: SignIn, memory: WorkplaceMemory) {
    val observe = ObserveSignIn(signIn)
    val localName = ReadLocalName(memory)
    val refresh = RefreshKkm(kassa, signIn)
}
