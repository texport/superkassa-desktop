package kz.mybrain.superkassa.presentation.settings.kkm

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель самой кассы в настройках окна. */
@Composable
fun kkmSettingsViewModel(app: AppContainer): KkmSettingsViewModel = viewModel { kkmSettingsModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun kkmSettingsModel(app: AppContainer): KkmSettingsViewModel = KkmSettingsViewModel(
    KkmCases(app.kassa, app.signIn, app.areas.settings.workplace, app.memory, app.journal),
    app.talk
)
