package kz.mybrain.superkassa.presentation.settings.tax

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/** Модель налогов кассы окна. */
@Composable
fun taxSettingsViewModel(services: WindowServices): TaxSettingsViewModel = viewModel { taxSettingsModel(services) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun taxSettingsModel(services: WindowServices): TaxSettingsViewModel =
    TaxSettingsViewModel(TaxCases(services.kassa, services.signIn), services.talk)
