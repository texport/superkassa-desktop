package kz.mybrain.superkassa.presentation.analytics.exchange

import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddress
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddresses
import kz.mybrain.superkassa.domain.analytics.model.exchangeRows
import kz.mybrain.superkassa.presentation.analytics.common.Reading

/**
 * Состояние вкладки обмена.
 *
 * @param query строка поиска.
 * @param register касса отбора; `null` — все кассы.
 */
data class AnalyticsExchangeUiState(
    val reading: Reading<ExchangeAddresses> = Reading(),
    val query: String = "",
    val register: String? = null
) {
    /** Все адреса ответа, до поиска и отбора. */
    val all: List<ExchangeAddress> get() = reading.value?.addresses.orEmpty()

    /** Строки таблицы: поиск и отбор вместе — правилом [exchangeRows]. */
    val rows: List<ExchangeAddress> get() = exchangeRows(all, query, register)
}
