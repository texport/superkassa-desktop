package kz.mybrain.superkassa.presentation.settings.tax

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель налогов кассы окна. */
@Composable
fun taxSettingsViewModel(app: AppContainer): TaxSettingsViewModel = viewModel { taxSettingsModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun taxSettingsModel(app: AppContainer): TaxSettingsViewModel =
    TaxSettingsViewModel(TaxCases(app.kassa, app.signIn), app.talk)
