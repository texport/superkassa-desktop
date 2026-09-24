package kz.mybrain.superkassa.presentation.analytics.record

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer

/** Модель вкладки учёта: одна на окно. */
@Composable
fun analyticsRecordViewModel(app: AppContainer): AnalyticsRecordViewModel = viewModel { analyticsRecordModel(app) }

/** Модель вкладки учёта без окна — для проверок и для окна. */
fun analyticsRecordModel(app: AppContainer): AnalyticsRecordViewModel =
    AnalyticsRecordViewModel(RecordCases(app.areas.analytics.cabinet))
