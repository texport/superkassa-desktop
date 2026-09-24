package kz.mybrain.superkassa.presentation.debug.log

import kz.mybrain.superkassa.domain.debug.model.LogLevel

/**
 * Что владелец делает с журналом: в окне отладки и в карточке настроек.
 *
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому.
 */
interface LogActions {
    fun filter(level: LogLevel) = Unit

    fun search(query: String) = Unit

    fun clear() = Unit

    fun save() = Unit

    fun chooseLevel(level: LogLevel) = Unit

    fun switchDebugMode(on: Boolean) = Unit
}
