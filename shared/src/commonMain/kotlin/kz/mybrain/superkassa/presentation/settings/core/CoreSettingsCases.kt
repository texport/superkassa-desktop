package kz.mybrain.superkassa.presentation.settings.core

import kz.mybrain.superkassa.domain.settings.port.CoreSettingsStore
import kz.mybrain.superkassa.domain.settings.usecase.ReadCoreSettings
import kz.mybrain.superkassa.domain.settings.usecase.SaveCoreSettings

/** Сценарии настроек кассы на этой машине. */
class CoreSettingsCases(store: CoreSettingsStore) {
    val read = ReadCoreSettings(store)
    val save = SaveCoreSettings(store)
}
