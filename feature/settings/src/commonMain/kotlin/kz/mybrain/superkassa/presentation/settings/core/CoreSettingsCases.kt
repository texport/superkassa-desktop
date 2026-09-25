package kz.mybrain.superkassa.presentation.settings.core

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore
import kz.mybrain.superkassa.domain.settings.usecase.core.ReadCoreSettings
import kz.mybrain.superkassa.domain.settings.usecase.core.ReadKassaFacts
import kz.mybrain.superkassa.domain.settings.usecase.core.SaveCoreSettings

/** Сценарии настроек кассы на этой машине и сведений о ней. */
class CoreSettingsCases(store: CoreSettingsStore, kassa: Kassa) {
    val read = ReadCoreSettings(store)
    val save = SaveCoreSettings(store)
    val facts = ReadKassaFacts(store, kassa)
}
