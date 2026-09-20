package kz.mybrain.superkassa.desktop.app.log

import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.readSetting
import kz.mybrain.superkassa.desktop.app.writeSetting
import java.io.File

/**
 * Что рабочее место помнит о журнале: уровень записи и режим отладки.
 *
 * По файлу на значение — как и остальные настройки рабочего места.
 * Лежит там же, где выбранная касса и язык: обслуживание ищет журнал
 * в одной папке с настройками, а не по всему диску.
 *
 * По умолчанию уровень обычный: тела запросов и ответов не пишутся,
 * пока владелец не включит отладочный.
 */
class LogSettings(private val home: File = Preferences.defaultFile().parentFile) {

    /** Порог записи. */
    var level: LogLevel
        get() = LogLevel.byCode(readSetting(levelFile))
        set(value) = writeSetting(levelFile, value.code)

    /** Включён ли режим отладки: при нём открыто окно журнала. */
    var debugMode: Boolean
        get() = readSetting(debugFile) == ON
        set(value) = writeSetting(debugFile, if (value) ON else null)

    /** Папка файлов журнала — рядом с настройками рабочего места. */
    val directory: File get() = File(home, "log")

    private val levelFile get() = File(home, "log-level")

    private val debugFile get() = File(home, "debug")

    private companion object {

        /** Отметка включённого режима: файла с другим содержимым не бывает. */
        const val ON = "on"
    }
}
