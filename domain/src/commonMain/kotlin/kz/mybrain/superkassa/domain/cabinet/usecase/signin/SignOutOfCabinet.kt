package kz.mybrain.superkassa.domain.cabinet.usecase.signin

import kz.mybrain.superkassa.domain.cabinet.port.CabinetAccount

/** Выход из кабинета: доступ отзывается и в кабинете, и здесь. */
class SignOutOfCabinet(private val account: CabinetAccount) {
    suspend operator fun invoke() = account.signOut()
}
