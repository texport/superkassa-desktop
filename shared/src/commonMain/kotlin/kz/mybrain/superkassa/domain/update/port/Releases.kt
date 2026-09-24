package kz.mybrain.superkassa.domain.update.port

import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kotlin.time.Instant

/**
 * Служба выпусков кассы.
 *
 * Сама ничего не ставит: касса — фискальный инструмент, и менять её
 * посреди смены без ведома кассира нельзя. Касса скачивает установщик,
 * сверяет его с выпуском и открывает — ставит кассир.
 */
interface Releases {

    /** Свежий выпуск с установщиком под эту систему; отказ сети — ответ, а не исключение. */
    suspend fun latest(): ReleaseAnswer

    /**
     * Скачивает установщик и сверяет его сумму с объявленной выпуском.
     *
     * Не совпавший файл удаляется здесь же: он не должен остаться на диске
     * там, где его можно открыть по ошибке.
     */
    suspend fun fetch(installer: Installer): Fetched

    /** Открывает скачанный и сверенный установщик; `false` — система не открыла. */
    fun openFile(file: String): Boolean

    /** Открывает страницу выпуска в браузере; `false` — система не открыла. */
    fun openPage(url: String): Boolean
}

/**
 * Что рабочее место помнит о проверке выпусков.
 *
 * По умолчанию касса проверяет выпуски сама; отказаться можно — на месте
 * без выхода в интернет проверка только пишет отказы в журнал.
 */
interface UpdateMemory {

    /** Проверять ли выпуски самой. */
    var automatic: Boolean

    /**
     * Когда выпуски проверялись в последний раз.
     *
     * Хранится, а не живёт в памяти: касса перезапускается каждое утро,
     * и без записи проверка шла бы при каждом запуске, а не раз в сутки.
     */
    var lastChecked: Instant?
}
