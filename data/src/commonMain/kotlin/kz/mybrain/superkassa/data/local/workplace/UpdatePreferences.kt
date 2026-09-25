package kz.mybrain.superkassa.data.local.workplace

import kotlinx.io.files.Path
import kz.mybrain.superkassa.domain.update.port.UpdateMemory
import kotlin.time.Instant

/**
 * Что рабочее место помнит о проверке обновлений.
 *
 * Отдельный предмет: к фискальной работе это не относится, а меняется
 * реже прочего. По умолчанию касса проверяет выпуски сама — владелец
 * узнаёт о новой версии из угла окна, а не из звонка поддержки.
 * Отказаться можно: на рабочем месте без выхода в интернет проверка
 * только пишет отказы в журнал.
 */
class UpdatePreferences(private val directory: Path) : UpdateMemory {

    /** Проверять ли выпуски самой; файла нет — проверять. */
    override var automatic: Boolean
        get() = readSetting(automaticFile) != OFF
        set(value) = writeSetting(automaticFile, if (value) null else OFF)

    /**
     * Когда выпуски проверялись в последний раз.
     *
     * Хранится, а не живёт в памяти: касса перезапускается каждое утро,
     * и без записи проверка шла бы при каждом запуске, а не раз в сутки.
     */
    override var lastChecked: Instant?
        get() = readSetting(checkedFile)?.let { runCatching { Instant.parse(it) }.getOrNull() }
        set(value) = writeSetting(checkedFile, value?.toString())

    private val automaticFile = Path(directory, "updates")

    private val checkedFile = Path(directory, "updates-checked")

    companion object {
        /** Отметка выключенной проверки: файла с другим содержимым не бывает. */
        const val OFF = "off"
    }
}
