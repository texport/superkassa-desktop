package kz.mybrain.superkassa.data.kassa.settings

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.presentation.api.SettingsApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore

/**
 * Настройки кассы, поднятой в процессе приложения.
 *
 * Фасад настроек блокирующий — он читает и пишет файл, — поэтому вызов
 * уходит в [io], как и у [EmbeddedKassa].
 *
 * @param directory каталог данных, на котором точка сборки подняла кассу.
 */
class EmbeddedSettings(
    private val api: SettingsApi,
    override val directory: String? = null,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : CoreSettingsStore {

    override suspend fun read(): CoreSettings = withContext(io) { api.getSettings() }

    override suspend fun save(settings: CoreSettings): CoreSettings = withContext(io) { api.updateSettings(settings) }
}
