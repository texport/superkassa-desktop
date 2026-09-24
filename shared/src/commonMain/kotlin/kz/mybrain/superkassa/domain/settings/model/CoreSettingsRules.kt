package kz.mybrain.superkassa.domain.settings.model

import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings

/** Касса работает сервером: её настройки меняют там, а не на рабочем месте. */
val CoreSettings.server: Boolean get() = mode == CoreMode.SERVER

/**
 * Правка закрыта: владелец запретил её в файле настроек кассы или касса
 * работает сервером. Закрытое видно до нажатия, а не отказом после него.
 */
val CoreSettings.frozen: Boolean get() = !allowChanges || server

/** Настройки с новыми сроками обмена с БФД; прочее — как было. */
fun CoreSettings.withBfdTimes(timeoutSeconds: Long, reconnectSeconds: Long): CoreSettings =
    copy(ofdTimeoutSeconds = timeoutSeconds, ofdReconnectIntervalSeconds = reconnectSeconds)

/** Секунды из набранного: целое число больше нуля, иначе `null`. */
fun secondsOf(text: String): Long? = text.trim().toLongOrNull()?.takeIf { it > 0 }
