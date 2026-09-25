package kz.mybrain.superkassa.presentation.settings.kkm

/**
 * Что владелец делает с самой кассой в настройках.
 *
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому.
 */
interface KkmSettingsActions {
    fun typeName(text: String) = Unit

    fun saveName() = Unit

    fun resetName() = Unit

    fun switchKkm() = Unit

    fun switchProgramming() = Unit

    fun askDecommission() = Unit

    fun cancelDecommission() = Unit

    fun decommission() = Unit
}
