package kz.mybrain.superkassa.domain.analytics.usecase

import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.port.Analytics

/**
 * Учёт касс компании: как КГД их учёл.
 *
 * Спрашивается та же ручка, что и у карты: в ответе о кассах на карте уже
 * лежит всё, из чего складывается учёт, — состояние КГД, блокировка, смена,
 * торговая точка и её адрес. Источник положения один — адрес торговой
 * точки: из него складывается регион, а точка на карте учёту не нужна.
 */
class ReadRecord(private val analytics: Analytics) {

    suspend operator fun invoke(): AnalyticsAnswer<KkmMapView> = analytics.kkms(PositionSource.RetailPlaceAddress)
}
