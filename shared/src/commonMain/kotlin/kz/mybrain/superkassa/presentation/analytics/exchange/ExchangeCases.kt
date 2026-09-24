package kz.mybrain.superkassa.presentation.analytics.exchange

import kz.mybrain.superkassa.domain.analytics.port.Analytics
import kz.mybrain.superkassa.domain.analytics.usecase.ReadExchange

/** Сценарии вкладки адресов обмена. */
class ExchangeCases(analytics: Analytics) {
    val read = ReadExchange(analytics)
}
