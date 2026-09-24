package kz.mybrain.superkassa.domain.cabinet.usecase.signin

import kz.mybrain.superkassa.domain.cabinet.port.CabinetAccount

/**
 * Вход владельца в кабинет по ЭЦП: кабинет выдаёт задачу, владелец её подписывает.
 * Длится, пока подписывающий ждёт подпись.
 */
class SignInToCabinet(private val account: CabinetAccount) {
    suspend operator fun invoke() = account.signIn()
}
