package kz.mybrain.superkassa.presentation.settings.core

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.settings.port.SettingsPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель настроек кассы на этой машине. */
@Composable
internal fun coreSettingsViewModel(services: WindowServices, ports: SettingsPorts): CoreSettingsViewModel =
    viewModel { coreSettingsModel(services, ports) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun coreSettingsModel(services: WindowServices, ports: SettingsPorts): CoreSettingsViewModel =
    CoreSettingsViewModel(CoreSettingsCases(ports.coreSettings, services.kassa), services.talk)
