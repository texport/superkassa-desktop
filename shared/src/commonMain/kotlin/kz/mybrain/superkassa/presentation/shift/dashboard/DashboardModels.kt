package kz.mybrain.superkassa.presentation.shift.dashboard

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель главного экрана окна.
 *
 * Одна на окно: живёт в хранилище моделей окна и переживает уход кассира
 * в другой раздел. Проверки создают модель сами, без окна.
 */
@Composable
fun dashboardViewModel(services: WindowServices): DashboardViewModel = viewModel { dashboardModel(services) }

/** Модель со сценариями, собранными из портов окна; проверки зовут её без окна. */
fun dashboardModel(services: WindowServices): DashboardViewModel =
    DashboardViewModel(DashboardCases(services.kassa, services.signIn), services.talk)
