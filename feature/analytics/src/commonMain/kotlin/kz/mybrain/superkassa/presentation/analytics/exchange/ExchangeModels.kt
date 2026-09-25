package kz.mybrain.superkassa.presentation.analytics.exchange

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts

/** Модель вкладки адресов обмена: одна на окно. */
@Composable
internal fun analyticsExchangeViewModel(ports: AnalyticsPorts): AnalyticsExchangeViewModel =
    viewModel { analyticsExchangeModel(ports) }

/** Модель вкладки адресов обмена без окна — для проверок и для окна. */
internal fun analyticsExchangeModel(ports: AnalyticsPorts): AnalyticsExchangeViewModel =
    AnalyticsExchangeViewModel(ExchangeCases(ports.cabinet))
