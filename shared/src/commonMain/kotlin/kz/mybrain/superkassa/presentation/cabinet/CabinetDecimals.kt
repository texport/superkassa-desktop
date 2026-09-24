package kz.mybrain.superkassa.presentation.cabinet

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.presentation.map.MapPoint

/**
 * Точка карты по координатам кабинета; без любой из них точки нет.
 *
 * Кабинет держит широту и долготу порознь, а карта — местом целиком:
 * точка, у которой известна одна широта, на карте не стоит нигде.
 */
internal fun mapPointOf(latitude: Decimal?, longitude: Decimal?): MapPoint? =
    if (latitude == null || longitude == null) null else MapPoint(latitude, longitude)
