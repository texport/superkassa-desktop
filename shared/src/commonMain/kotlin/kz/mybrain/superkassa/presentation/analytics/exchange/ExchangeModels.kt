package kz.mybrain.superkassa.presentation.analytics.exchange

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель вкладки адресов обмена: одна на окно. */
@Composable
fun analyticsExchangeViewModel(app: AppContainer): AnalyticsExchangeViewModel =
    viewModel { analyticsExchangeModel(app) }

/** Модель вкладки адресов обмена без окна — для проверок и для окна. */
fun analyticsExchangeModel(app: AppContainer): AnalyticsExchangeViewModel =
    AnalyticsExchangeViewModel(ExchangeCases(app.areas.analytics.cabinet))
