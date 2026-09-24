package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель настроек машины окна. */
@Composable
fun workplaceSettingsViewModel(app: AppContainer): WorkplaceSettingsViewModel =
    viewModel { workplaceSettingsModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun workplaceSettingsModel(app: AppContainer): WorkplaceSettingsViewModel =
    WorkplaceSettingsViewModel(WorkplaceCases(app.signIn, app.areas.settings.workplace, app.memory))
