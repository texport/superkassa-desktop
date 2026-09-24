package kz.mybrain.superkassa.data.eds

import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.data.log.LogLevel
import kz.mybrain.superkassa.data.log.LogSource
import kz.mybrain.superkassa.integrations.ncalayer.NcaJournal

/**
 * Обмен с NCALayer — в журнал приложения, источником подписи.
 *
 * Разбирают его с чужой машины, без отладочного режима, поэтому уровень
 * обычный, а сорванный шаг — предупреждением. Строки модуля уже без
 * подписи, содержимого и сертификата, а помеху называют её именем.
 */
internal fun signatureJournal(): NcaJournal = NcaJournal { line, failure ->
    AppLog.record(LogSource.Signature, if (failure == null) LogLevel.Info else LogLevel.Warning, line)
}
