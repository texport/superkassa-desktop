package kz.mybrain.superkassa.presentation.settings.kkm

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.settings.port.SettingsPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель самой кассы в настройках окна. */
@Composable
internal fun kkmSettingsViewModel(services: WindowServices, ports: SettingsPorts): KkmSettingsViewModel =
    viewModel { kkmSettingsModel(services, ports) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun kkmSettingsModel(services: WindowServices, ports: SettingsPorts): KkmSettingsViewModel = KkmSettingsViewModel(
    KkmCases(services.kassa, services.signIn, ports.workplace, services.memory, services.talk.journal),
    services.talk
)
