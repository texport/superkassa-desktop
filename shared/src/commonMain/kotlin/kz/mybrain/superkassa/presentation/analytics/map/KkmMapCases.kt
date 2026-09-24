package kz.mybrain.superkassa.presentation.analytics.map

import kz.mybrain.superkassa.domain.analytics.port.Analytics
import kz.mybrain.superkassa.domain.analytics.usecase.ReadKkmMap
import kz.mybrain.superkassa.presentation.common.mapview.MapCases

/**
 * Сценарии карты касс: кассы у кабинета, дома их точек у служб карты.
 *
 * @property map сценарии самой карты: плитки, поиск домов, своё место.
 */
class KkmMapCases(analytics: Analytics, val map: MapCases) {
    val read = ReadKkmMap(analytics)
}
