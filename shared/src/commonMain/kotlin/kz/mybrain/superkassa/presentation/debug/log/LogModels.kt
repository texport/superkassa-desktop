package kz.mybrain.superkassa.presentation.debug.log

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель журнала окна: одна на окно кассы и на соседнее окно отладки. */
@Composable
fun logViewModel(app: AppContainer): LogViewModel = viewModel { logModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun logModel(app: AppContainer): LogViewModel = LogViewModel(LogCases(app.areas.debug.logBook), app.talk)
