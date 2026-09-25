package kz.mybrain.superkassa.data.cabinet.signin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.data.cabinet.cabinetCall
import kz.mybrain.superkassa.domain.cabinet.model.CabinetCompany
import kz.mybrain.superkassa.domain.cabinet.model.CabinetOwner
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUser
import kz.mybrain.superkassa.domain.cabinet.port.CabinetAccount
import kz.mybrain.superkassa.integrations.bfdcabinet.signin.AccountApi
import kz.mybrain.superkassa.integrations.bfdcabinet.signin.CabinetOwner as BfdOwner

/**
 * Вход владельца — модулем кабинета.
 *
 * Доступ, выданный кабинетом, живёт в модуле и сюда не приходит: порт
 * знает только, кто вошёл.
 *
 * Вход и выход идут вне главного потока: по пути модуль пишет журнал
 * обмена в файл, разбирает ответы и ждёт подписывающего. На главном
 * потоке каждое нажатие «Войти» задерживало кадр, а частые нажатия
 * складывались в видимые рывки.
 */
internal class RemoteAccount(private val account: AccountApi) : CabinetAccount {
    override val owner: Flow<CabinetOwner?> = account.owner.map { it?.owner() }

    override val address: String get() = account.address

    override suspend fun signIn() {
        withContext(Dispatchers.IO) { cabinetCall { account.signIn() } }
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) { cabinetCall { account.signOut() } }
}

/** Вошедший и его компания — так, как их знает предметная область. */
private fun BfdOwner.owner() = CabinetOwner(
    user = CabinetUser(user.id, user.iin, user.fullName),
    company = CabinetCompany(company.id, company.bin, company.name)
)
