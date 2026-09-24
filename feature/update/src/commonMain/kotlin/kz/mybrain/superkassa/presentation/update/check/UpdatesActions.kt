package kz.mybrain.superkassa.presentation.update.check

import kz.mybrain.superkassa.domain.update.model.AvailableUpdate

/**
 * Что кассир делает с обновлениями.
 *
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому.
 */
interface UpdatesActions {
    fun check() = Unit

    fun switchAutomatic(on: Boolean) = Unit

    fun install(update: AvailableUpdate) = Unit
}
