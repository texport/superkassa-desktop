package kz.mybrain.superkassa.presentation.settings.ofd

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель связи кассы с БФД окна. */
@Composable
fun ofdSettingsViewModel(app: AppContainer): OfdSettingsViewModel = viewModel { ofdSettingsModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun ofdSettingsModel(app: AppContainer): OfdSettingsViewModel =
    OfdSettingsViewModel(OfdCases(app.kassa, app.signIn), app.talk)
