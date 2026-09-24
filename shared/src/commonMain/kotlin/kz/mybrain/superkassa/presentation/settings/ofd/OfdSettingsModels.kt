package kz.mybrain.superkassa.presentation.settings.ofd

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель связи кассы с БФД окна. */
@Composable
fun ofdSettingsViewModel(services: WindowServices): OfdSettingsViewModel = viewModel { ofdSettingsModel(services) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun ofdSettingsModel(services: WindowServices): OfdSettingsViewModel =
    OfdSettingsViewModel(OfdCases(services.kassa, services.signIn), services.talk)
