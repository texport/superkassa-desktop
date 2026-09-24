package kz.mybrain.superkassa.presentation.analytics.sales

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kz.mybrain.superkassa.domain.analytics.port.Analytics
import kz.mybrain.superkassa.domain.analytics.usecase.ReadKkmSales
import kz.mybrain.superkassa.domain.analytics.usecase.ReadSales
import kotlin.time.Clock

/**
 * Сценарии торговой сводки: по сети и по одной кассе.
 *
 * @property today какой сегодня день: по нему строится срок сводки
 *   и срок по умолчанию на полосе выбора; проверки задают его сами.
 */
class SalesCases(analytics: Analytics, val today: () -> LocalDate = ::localToday) {
    val readSales = ReadSales(analytics, today)
    val readKkmSales = ReadKkmSales(analytics, today)
}

/** Сегодня на часах рабочего места. */
private fun localToday(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
