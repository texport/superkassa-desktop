package kz.mybrain.superkassa.presentation.shift.dashboard

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/**
 * Модель главного экрана окна.
 *
 * Одна на окно: живёт в хранилище моделей окна и переживает уход кассира
 * в другой раздел. Проверки создают модель сами, без окна.
 */
@Composable
fun dashboardViewModel(app: AppContainer): DashboardViewModel = viewModel { dashboardModel(app) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun dashboardModel(app: AppContainer): DashboardViewModel =
    DashboardViewModel(DashboardCases(app.kassa, app.signIn), app.talk)
