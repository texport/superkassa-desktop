package kz.mybrain.superkassa.presentation.users

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.ObserveSignIn
import kz.mybrain.superkassa.domain.users.usecase.ChangeCashierPin
import kz.mybrain.superkassa.domain.users.usecase.CreateCashier
import kz.mybrain.superkassa.domain.users.usecase.ReadCashiers
import kz.mybrain.superkassa.domain.users.usecase.ReadRoleNames
import kz.mybrain.superkassa.domain.users.usecase.RemoveCashier

/**
 * Сценарии кассиров: всё, что модель делает с кассой, — только через них.
 *
 * Смена пина и удаление берут сам держатель входа: сменивший пин себе
 * продолжает работу новым, удаливший себя выходит из кассы.
 */
class UsersCases(kassa: Kassa, signIn: SignIn) {
    val observe = ObserveSignIn(signIn)
    val readCashiers = ReadCashiers(kassa, signIn)
    val readRoleNames = ReadRoleNames(kassa)
    val createCashier = CreateCashier(kassa, signIn)
    val changeCashierPin = ChangeCashierPin(kassa, signIn)
    val removeCashier = RemoveCashier(kassa, signIn)
}
