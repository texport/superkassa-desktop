package kz.mybrain.superkassa.domain.kassa.port

import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings

/** Настройки доставки, заданные проверкой; по умолчанию доставка не настраивалась. */
class FixedDeliverySetup(private val settings: DeliverySettings? = null) : DeliverySetup {
    override suspend fun read(): DeliverySettings? = settings
}
