package kz.mybrain.superkassa.presentation.settings.core

import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore
import kz.mybrain.superkassa.domain.settings.usecase.core.ReadCoreSettings
import kz.mybrain.superkassa.domain.settings.usecase.core.SaveDeliveryChannels

/** Сценарии каналов доставки чека. */
class DeliveryCases(store: CoreSettingsStore) {
    val read = ReadCoreSettings(store)
    val save = SaveDeliveryChannels(store)
}
