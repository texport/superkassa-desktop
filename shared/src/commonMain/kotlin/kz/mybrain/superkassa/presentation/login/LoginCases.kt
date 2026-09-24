package kz.mybrain.superkassa.presentation.login

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn
import kz.mybrain.superkassa.domain.signin.usecase.ReadKkmChoice
import kz.mybrain.superkassa.domain.signin.usecase.SignInCashier
import kz.mybrain.superkassa.domain.signin.usecase.SignOut
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/** Сценарии входа: список касс, вход кассира и его уход. */
class LoginCases(kassa: Kassa, signIn: SignIn, memory: WorkplaceMemory, journal: Journal) {
    val observe = ObserveSignIn(signIn)
    val readKkms = ReadKkmChoice(kassa, memory)
    val signInCashier = SignInCashier(kassa, signIn, memory, journal)
    val signOut = SignOut(signIn, journal)
}
