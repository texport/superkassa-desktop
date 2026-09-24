package kz.mybrain.superkassa.domain.analytics.usecase

import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddresses
import kz.mybrain.superkassa.domain.analytics.port.Analytics

/**
 * Адреса, с которых выходили на связь кассы компании.
 *
 * Список спрашивается целиком по компании: отбор по кассе и поиск работают
 * уже по нему. Ходить в кабинет на каждое нажатие в отборе значило бы ждать
 * сеть там, где ответ уже под рукой.
 */
class ReadExchange(private val analytics: Analytics) {

    suspend operator fun invoke(): AnalyticsAnswer<ExchangeAddresses> = analytics.exchange()
}
