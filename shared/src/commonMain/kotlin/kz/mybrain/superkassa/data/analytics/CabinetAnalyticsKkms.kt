package kz.mybrain.superkassa.data.analytics

import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddress
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddresses
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.KkmPosition
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.AnalyticsKkm as WireKkm
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.ExchangeAddress as WireExchange
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.ExchangeAddresses as WireExchanges
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.KkmMapView as WireMap
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.KkmPosition as WirePosition
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.PositionSource as WireSource

/**
 * Кассы на карте и адреса обмена — из ответов кабинета в предметную область.
 *
 * Поля совпадают по смыслу один к одному: перевод только отвязывает
 * предметную область от формата кабинета.
 */

/** Источник положения так, как его называет кабинет. */
internal fun PositionSource.wire(): WireSource = WireSource.byCode(code)

internal fun WireMap.domain(): KkmMapView = KkmMapView(
    positionSource = positionSource,
    placedCount = placedCount,
    withoutPositionCount = withoutPositionCount,
    placed = placed.map { it.domain() },
    withoutPosition = withoutPosition.map { it.domain() }
)

private fun WireKkm.domain(): AnalyticsKkm = AnalyticsKkm(
    cashRegisterId = cashRegisterId,
    kkmId = kkmId,
    registrationNumber = registrationNumber,
    internalName = internalName,
    retailPlaceId = retailPlaceId,
    retailPlaceName = retailPlaceName,
    address = address,
    status = status,
    blocked = blocked,
    shiftStatus = shiftStatus,
    shiftNumber = shiftNumber,
    lastContactAt = lastContactAt,
    position = position?.domain()
)

private fun WirePosition.domain(): KkmPosition = KkmPosition(source, latitude, longitude, geoSource, rka, cato)

internal fun WireExchanges.domain(): ExchangeAddresses =
    ExchangeAddresses(cashRegisterCount, addressCount, addresses.map { it.domain() })

private fun WireExchange.domain(): ExchangeAddress = ExchangeAddress(
    cashRegisterId = cashRegisterId,
    kkmId = kkmId,
    registrationNumber = registrationNumber,
    internalName = internalName,
    retailPlaceName = retailPlaceName,
    address = address,
    firstSeen = firstSeen,
    lastSeen = lastSeen
)
