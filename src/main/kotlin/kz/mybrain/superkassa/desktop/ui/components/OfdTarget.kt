package kz.mybrain.superkassa.desktop.ui.components

/**
 * Куда касса будет слать чеки: один из ОФД справочника узла и контур.
 *
 * Адреса серверов ОФД узел знает сам — по провайдеру и контуру. Владелец
 * хост и порт не вводит: присланный клиентом адрес узел не принимает.
 */
data class OfdTarget(
    val provider: String = "",
    val environment: String = ""
) {
    /** Всё нужное для заведения кассы заполнено. */
    val complete: Boolean get() = provider.isNotBlank() && environment.isNotBlank()
}
