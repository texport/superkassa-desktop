package kz.mybrain.superkassa.presentation.setup

import kz.mybrain.superkassa.presentation.common.cabinet.CabinetCalls

/**
 * Обращения там, где кабинета нет: на ручном пути мастер к кабинету
 * не обращается, и чужой занятости, которую нужно показать, нет.
 */
internal object WithoutCabinet : CabinetCalls {
    override suspend fun <T> run(action: String, block: suspend () -> T): T? = block()
}
