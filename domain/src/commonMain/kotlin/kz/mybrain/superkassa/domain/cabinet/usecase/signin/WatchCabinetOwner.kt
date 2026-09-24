package kz.mybrain.superkassa.domain.cabinet.usecase.signin

import kotlinx.coroutines.flow.Flow
import kz.mybrain.superkassa.domain.cabinet.model.CabinetOwner
import kz.mybrain.superkassa.domain.cabinet.port.CabinetAccount

/** Кто вошёл в кабинет — и когда вошедшего не стало: вышел или истёк доступ. */
class WatchCabinetOwner(private val account: CabinetAccount) {
    operator fun invoke(): Flow<CabinetOwner?> = account.owner
}
