package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.map.MapProjection

/**
 * Сводит поставленные кассы в ярлычки.
 *
 * Порядок касс внутри ярлычка устойчивый, а не такой, каким их отдал
 * кабинет: по списку места владелец возвращается к той же кассе,
 * и строки не должны меняться местами между перерисовками.
 */
fun kkmGroups(placed: List<PlacedKkm>, zoom: Int): List<KkmGroup> =
    placed
        .groupBy { MapProjection.cell(it.latitude, it.longitude, zoom, GROUP_CELL) }
        .map { (_, kkms) -> group(kkms.sortedBy { it.kkm.cashRegisterId }) }
        .sortedBy { it.id }

private fun group(kkms: List<PlacedKkm>): KkmGroup = KkmGroup(
    // Середина места, а не координаты первой кассы: иначе ярлычок
    // трёх касс стоял бы на одной из них и смещался при смене порядка.
    latitude = kkms.map { it.latitude }.average(),
    longitude = kkms.map { it.longitude }.average(),
    kkms = kkms
)

/**
 * Цвет ярлычка — по худшему, что в нём есть.
 *
 * Ярлычок один на несколько касс, и красить его средним нельзя:
 * заблокированная касса среди трёх работающих — повод подойти к этому
 * месту, и она обязана быть видна с карты города.
 */
fun groupTone(group: KkmGroup): StatusTone = when {
    group.kkms.any { it.kkm.blocked || KkmMark.OffRecord.holds(it.kkm) } -> StatusTone.Bad
    group.kkms.any { KkmMark.ShiftOpen.holds(it.kkm) } -> StatusTone.Waiting
    else -> StatusTone.Good
}

/**
 * Сторона клетки места в точках полотна.
 *
 * Примерно во столько же точек рисуется сам ярлычок: клетка меньше
 * ярлычка сводила бы кассы, чьи ярлычки всё равно наезжают друг
 * на друга, а заметно больше — склеивала бы соседние дома.
 */
private const val GROUP_CELL = 56.0
