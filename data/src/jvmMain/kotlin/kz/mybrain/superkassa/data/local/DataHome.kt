package kz.mybrain.superkassa.data.local

import kz.mybrain.superkassa.domain.version.model.BuildVersion
import java.io.File

/**
 * Каталог данных рабочего места: настройки, журнал и касса в процессе.
 *
 * Выбирается один раз и для всех: переменной окружения [VARIABLE] или
 * свойством [PROPERTY]. Без них выпуск живёт в `~/.superkassa`, а сборка
 * разработчика — в `~/.superkassa-dev`: запуск из исходников не должен
 * трогать кассу, которая на этой же машине работает по-настоящему, —
 * её токен, пины кассиров и смены.
 */
object DataHome {

    /** Переменная окружения с каталогом данных. */
    const val VARIABLE = "SUPERKASSA_HOME"

    /** Свойство машины Java с каталогом данных. */
    const val PROPERTY = "superkassa.home"

    /** Каталог данных этого запуска. */
    fun directory(): File = resolve(
        variable = System.getenv(VARIABLE),
        property = System.getProperty(PROPERTY),
        version = BuildVersion.NAME,
        userHome = System.getProperty("user.home")
    )

    /** Каталог кассы в процессе: база, настройки ядра и замок владельца. */
    fun kassa(): File = File(directory(), KASSA)

    /**
     * Какой каталог взять: названный явно, иначе по виду сборки.
     *
     * Переменная сильнее свойства: её задают снаружи запуска — службой,
     * ярлыком, — а свойство вшито в сам запуск.
     */
    internal fun resolve(variable: String?, property: String?, version: String, userHome: String): File {
        val named = variable?.takeIf { it.isNotBlank() } ?: property?.takeIf { it.isNotBlank() }
        if (named != null) return File(named)
        val folder = if (version.endsWith(DEVELOPER_SUFFIX)) DEVELOPER_FOLDER else RELEASE_FOLDER
        return File(userHome, folder)
    }

    private const val DEVELOPER_SUFFIX = "-dev"
    private const val RELEASE_FOLDER = ".superkassa"
    private const val DEVELOPER_FOLDER = ".superkassa-dev"
    private const val KASSA = "kassa"
}
