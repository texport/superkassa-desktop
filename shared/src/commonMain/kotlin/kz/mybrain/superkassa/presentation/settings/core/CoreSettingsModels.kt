package kz.mybrain.superkassa.presentation.settings.core

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель настроек кассы на этой машине. */
@Composable
fun coreSettingsViewModel(app: AppContainer): CoreSettingsViewModel = viewModel { coreSettingsModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun coreSettingsModel(app: AppContainer): CoreSettingsViewModel =
    CoreSettingsViewModel(CoreSettingsCases(app.areas.settings.coreSettings), app.talk)
