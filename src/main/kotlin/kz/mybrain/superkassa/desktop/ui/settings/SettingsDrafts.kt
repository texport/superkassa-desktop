package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.runtime.mutableStateMapOf

/**
 * Набранное в настройках, за что ещё не нажали «Сохранить».
 *
 * Экран настроек уходит из состава вместе с разделом, и набранное в его
 * полях исчезало вместе с ним: владелец набирал адрес узла, отвлекался
 * на журнал и возвращался к прежнему адресу, ничего об этом не узнав.
 * Своё поле помнить об этом не может — его самого на экране уже нет.
 *
 * Живёт в памяти и только до закрытия кассы: на диск попадает то,
 * что сохранили кнопкой, а черновик — это ещё не настройка.
 *
 * Заведено рядом с [EscapeCloses][kz.mybrain.superkassa.desktop.ui.components.EscapeCloses]
 * и по тому же поводу: состояние принадлежит окну, а не тому, что в нём
 * сейчас нарисовано.
 */
object SettingsDrafts {

    private val typed = mutableStateMapOf<String, String>()

    /** Что показывать в поле: набранное, если оно есть, иначе сохранённое. */
    fun of(field: String, saved: String): String = typed[field] ?: saved

    /** Владелец набрал в поле: держим до сохранения или до закрытия кассы. */
    fun type(field: String, value: String) {
        typed[field] = value
    }

    /** Сохранено: черновик больше не нужен, поле берёт значение из настроек. */
    fun forget(field: String) {
        typed.remove(field)
    }

    /** Поля, у которых бывает несохранённое: имя черновика — имя настройки. */
    object Field {
        const val NODE_ADDRESS = "node-address"
        const val CABINET_ADDRESS = "cabinet-address"
        const val MAP_TILES = "map-tiles"
        const val MAP_SEARCH = "map-search"
        const val MAP_REVERSE = "map-reverse"
        const val MAP_LOCATION = "map-location"
    }
}
