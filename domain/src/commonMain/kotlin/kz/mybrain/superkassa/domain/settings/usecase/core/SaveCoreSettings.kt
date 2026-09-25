package kz.mybrain.superkassa.domain.settings.usecase.core

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.answering
import kz.mybrain.superkassa.domain.settings.model.withBfdTimes
import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore

/**
 * Сроки обмена с БФД: сколько ждать ответа и когда пробовать связь снова.
 *
 * Сроки ложатся на запись, прочитанную в момент сохранения: доставку чека
 * меняют в той же записи, и прежняя запись с экрана вернула бы её назад.
 *
 * Правку решает касса: в режиме сервера и под запретом владельца она
 * отказывает кодом `SETTINGS_FROZEN`. Новые значения действуют со следующего
 * запуска кассы.
 */
class SaveCoreSettings(private val store: CoreSettingsStore) {

    suspend operator fun invoke(timeoutSeconds: Long, reconnectSeconds: Long): Answer<CoreSettings> =
        answering { store.save(store.read().withBfdTimes(timeoutSeconds, reconnectSeconds)) }
}
