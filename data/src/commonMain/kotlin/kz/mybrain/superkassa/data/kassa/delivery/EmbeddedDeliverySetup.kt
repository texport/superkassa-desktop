package kz.mybrain.superkassa.data.kassa.delivery

import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.presentation.api.SettingsApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.domain.kassa.port.DeliverySetup

/**
 * Настройки доставки чека кассы, поднятой в процессе приложения.
 *
 * Фасад настроек блокирующий — он читает файл, — поэтому вызов уходит
 * в [io], как и у кассы.
 */
class EmbeddedDeliverySetup(
    private val api: SettingsApi,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : DeliverySetup {

    override suspend fun read(): DeliverySettings? = withContext(io) { api.getSettings().delivery }
}
