package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.settings.port.SettingsPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель настроек машины окна. */
@Composable
internal fun workplaceSettingsViewModel(services: WindowServices, ports: SettingsPorts): WorkplaceSettingsViewModel =
    viewModel { workplaceSettingsModel(services, ports) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
internal fun workplaceSettingsModel(services: WindowServices, ports: SettingsPorts): WorkplaceSettingsViewModel =
    WorkplaceSettingsViewModel(WorkplaceCases(services.signIn, ports.workplace, services.memory))
