package kz.mybrain.superkassa.data.log

import kz.mybrain.superkassa.domain.journal.Journal

/**
 * Журнал экранов, записанный в общий журнал рабочего места.
 *
 * Тот же файл и то же окно отладки, что у обмена с узлом и кабинетом:
 * разбор отказа не должен складывать источники по времени руками.
 *
 * @param source чьи это записи; касса в процессе пишет как касса.
 */
class AppJournal(private val source: LogSource = LogSource.Machine) : Journal {

    override fun info(text: String) = AppLog.record(source, LogLevel.Info, text)

    override fun warn(text: String) = AppLog.record(source, LogLevel.Warning, text)

    override fun failure(text: String) = AppLog.record(source, LogLevel.Failure, text)
}
