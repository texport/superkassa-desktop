package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель каркаса окна: одна на окно, живёт в хранилище моделей окна. */
@Composable
internal fun shellViewModel(app: AppContainer): ShellViewModel = viewModel { shellModel(app) }

/** Модель каркаса со сценариями из портов окна; проверки зовут её без окна. */
internal fun shellModel(app: AppContainer): ShellViewModel =
    ShellViewModel(ShellCases(app.services.kassa, app.services.signIn, app.services.memory), app.services.talk)
