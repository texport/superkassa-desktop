package kz.mybrain.superkassa.presentation.analytics.record

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.analytics.AnalyticsPorts

/** Модель вкладки учёта: одна на окно. */
@Composable
fun analyticsRecordViewModel(ports: AnalyticsPorts): AnalyticsRecordViewModel =
    viewModel { analyticsRecordModel(ports) }

/** Модель вкладки учёта без окна — для проверок и для окна. */
fun analyticsRecordModel(ports: AnalyticsPorts): AnalyticsRecordViewModel =
    AnalyticsRecordViewModel(RecordCases(ports.cabinet))
