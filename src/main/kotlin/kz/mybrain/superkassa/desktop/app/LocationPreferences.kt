package kz.mybrain.superkassa.desktop.app

import java.io.File

/**
 * Разрешил ли владелец определять место по адресу подключения.
 *
 * Отдельный предмет от остальных настроек: это не выбор удобства,
 * а согласие отдать наружу адрес подключения. Спрашивается один раз
 * и запоминается — так же, как это делает браузер; решение принимает
 * владелец, а не приложение.
 *
 * Пусто — не спрашивали ещё; отказ хранится наравне с согласием,
 * чтобы не спрашивать снова при каждом открытии карты.
 */
class LocationPreferences(private val directory: File?) {

    var allowed: Boolean?
        get() = when (readSetting(locationFile)) {
            ALLOWED -> true
            DENIED -> false
            else -> null
        }
        set(value) = writeSetting(locationFile, value?.let { if (it) ALLOWED else DENIED })

    private val locationFile = File(directory, "location")

    companion object {
        /** Ответ владельца на вопрос об определении места. */
        const val ALLOWED = "allowed"
        const val DENIED = "denied"
    }
}
