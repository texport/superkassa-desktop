package kz.mybrain.superkassa.domain.settings.port

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings

/**
 * Настройки самой кассы в процессе: сроки обмена с БФД, доставка, протокол.
 *
 * Это не настройки одной кассы — налоги и оформление чека живут у кассы
 * и меняются её фасадом, — а настройки ядра целиком: одни на все кассы
 * рабочего места. Пина они не спрашивают, правку их решает ядро: отказ
 * приходит его исключением `SettingsFrozenException`.
 */
interface CoreSettingsStore {

    /**
     * Каталог данных кассы: база, настройки ядра, замок владельца; `null` —
     * платформа его не назвала. Поддержка ищет там базу и журнал ядра.
     */
    val directory: String?

    /** Действующие настройки. */
    suspend fun read(): CoreSettings

    /**
     * Сохраняет настройки целиком; действуют они со следующего запуска кассы.
     *
     * @return сохранённые настройки.
     */
    suspend fun save(settings: CoreSettings): CoreSettings
}
