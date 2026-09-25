package kz.mybrain.superkassa.presentation.analytics.sales

import kz.mybrain.superkassa.domain.analytics.model.SalesView
import kz.mybrain.superkassa.presentation.analytics.common.Reading
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod

/**
 * Состояние сводки: выбранный срок и ответ кабинета по нему.
 *
 * @param period срок полосы выбора — тот же, что у журнала кассы.
 */
internal data class AnalyticsSalesUiState(
    val period: JournalPeriod,
    val reading: Reading<SalesView> = Reading()
)
