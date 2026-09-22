package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
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
 * Цвет ярлычка — по тому, чем занято место.
 *
 * Беда — это заблокированная касса и отказ КГД, и только они. Прежде
 * беспокойной считалась любая касса вне учёта, а черновик вне учёта:
 * владелец завёл кассу и ещё не подал заявление. В кабинете показа
 * из 3294 касс 3288 — черновики, и карта страны краснела целиком.
 *
 * Доля отвечает на тот вопрос, который владелец задаёт карте страны:
 * десятая часть касс места не работает — туда надо ехать, одна
 * из полусотни — обычный день сети. У места из трёх касс одна
 * заблокированная — треть, и оно по-прежнему красное.
 *
 * Место, где на учёте нет ни одной кассы, не зелёное и не красное:
 * оно заведено, но ещё не работает, и зелёным обещало бы работающую
 * торговую точку там, где её нет.
 */
fun groupTone(group: KkmGroup): StatusTone {
    val troubled = group.kkms.count { it.kkm.blocked || it.kkm.record == KkmRecord.Refused }
    return when {
        troubled * TROUBLE_SHARE >= group.size -> StatusTone.Bad
        troubled > 0 -> StatusTone.Waiting
        group.kkms.none { it.kkm.record == KkmRecord.OnRecord } -> StatusTone.Idle
        else -> StatusTone.Good
    }
}

/** Сколько касс места стоит на учёте: этим числом подписан итог над картой. */
fun onRecordCount(group: KkmGroup): Int = group.kkms.count { it.kkm.record == KkmRecord.OnRecord }

/** Доля неблагополучных касс, с которой место краснеет: десятая часть. */
private const val TROUBLE_SHARE = 10

/**
 * Сторона клетки места в точках полотна.
 *
 * Чуть больше самого крупного ярлычка: клетка меньше ярлычка сводила бы
 * кассы, чьи ярлычки всё равно наезжают друг на друга, а заметно
 * большая — склеивала бы соседние дома. Между кружками соседних клеток
 * остаётся воздух, и на карте страны они читаются порознь.
 */
private const val GROUP_CELL = 64.0
