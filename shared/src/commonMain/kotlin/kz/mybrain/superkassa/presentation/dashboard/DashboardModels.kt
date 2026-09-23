package kz.mybrain.superkassa.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.AppContainer

/**
 * Модель главного экрана окна.
 *
 * Одна на окно: живёт в хранилище моделей окна и переживает уход кассира
 * в другой раздел. Проверки создают модель сами, без окна.
 */
@Composable
fun dashboardViewModel(app: AppContainer): DashboardViewModel = viewModel { DashboardViewModel(app) }
