package kz.mybrain.superkassa.domain.analytics.usecase

import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.port.Analytics

/**
 * Кассы компании на карте: положение — из выбранного источника.
 *
 * При источнике «адрес торговой точки» координат кабинет не даёт вовсе:
 * дома по адресам ищет служба карт, а не кабинет.
 */
class ReadKkmMap(private val analytics: Analytics) {

    suspend operator fun invoke(source: PositionSource): AnalyticsAnswer<KkmMapView> = analytics.kkms(source)
}
