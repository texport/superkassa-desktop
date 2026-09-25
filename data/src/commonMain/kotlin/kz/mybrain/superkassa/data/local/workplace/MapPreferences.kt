package kz.mybrain.superkassa.data.local.workplace

import kotlinx.io.files.Path

/**
 * Чьими службами рисуется карта на этом рабочем месте.
 *
 * Пустое значение означает общедоступную службу сообщества: её правила
 * запрещают массовую выкачку, и выпускать на неё всех владельцев нельзя.
 * Замена — настройка, а не перевыпуск приложения.
 */
class MapPreferences(private val directory: Path) {

    /** Откуда берутся плитки карты. */
    var tiles: String?
        get() = readSetting(tilesFile)
        set(value) = writeSetting(tilesFile, value)

    /** Чем ищется адрес торговой точки. */
    var search: String?
        get() = readSetting(searchFile)
        set(value) = writeSetting(searchFile, value)

    /** Чем узнаётся место под поставленной на карте меткой. */
    var reverse: String?
        get() = readSetting(reverseFile)
        set(value) = writeSetting(reverseFile, value)

    /** Чем определяется место по адресу подключения. */
    var location: String?
        get() = readSetting(locationFile)
        set(value) = writeSetting(locationFile, value)

    private val tilesFile = Path(directory, "map-tiles")

    private val searchFile = Path(directory, "map-search")

    private val reverseFile = Path(directory, "map-reverse")

    private val locationFile = Path(directory, "map-location")
}
