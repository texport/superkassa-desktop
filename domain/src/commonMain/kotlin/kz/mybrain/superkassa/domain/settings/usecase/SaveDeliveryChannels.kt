package kz.mybrain.superkassa.domain.settings.usecase

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.answering
import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.domain.settings.model.DeliveryRules
import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore

/**
 * Каналы доставки чека: включены ли, кому шлют, адреса служб и ключи SMS,
 * Telegram, WhatsApp и почты.
 *
 * Настройки кассы — одна запись на всё рабочее место, и сроки обмена с БФД
 * меняют в ней же. Поэтому набранное ложится на запись, прочитанную
 * в момент сохранения, а не на ту, что была на экране: иначе сохранение
 * доставки возвращало бы прежние сроки, сохранённые минуту назад.
 *
 * Правку решает касса: в режиме сервера и под запретом владельца она
 * отказывает кодом `SETTINGS_FROZEN`. Адреса и ключи касса берёт из настроек
 * на каждую доставку, а какие каналы включены и кому они шлют, — при запуске:
 * это действует после перезапуска кассы.
 */
class SaveDeliveryChannels(private val store: CoreSettingsStore) {

    /**
     * @param typed набранное по полям; поля, которых не касались, не передаются.
     * @param switched включённые и выключенные каналы; нетронутые не передаются.
     */
    suspend operator fun invoke(
        typed: Map<DeliveryField, String>,
        switched: Map<DeliveryChannel, Boolean>
    ): Answer<CoreSettings> = answering {
        val now = store.read()
        store.save(now.copy(delivery = DeliveryRules.applied(now.delivery, typed, switched)))
    }
}
