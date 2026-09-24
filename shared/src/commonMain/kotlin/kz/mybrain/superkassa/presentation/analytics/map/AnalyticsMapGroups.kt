package kz.mybrain.superkassa.presentation.analytics.map

import kz.mybrain.superkassa.domain.analytics.model.PlacedKkm
import kz.mybrain.superkassa.domain.cabinet.model.KkmRecord
import kz.mybrain.superkassa.presentation.common.mapview.MapProjection
import kz.mybrain.superkassa.presentation.common.status.StatusTone
import kz.mybrain.superkassa.presentation.theme.size.AnalyticsLayout

/**
 * Сводит поставленные кассы в ярлычки.
 *
 * Порядок касс внутри ярлычка устойчивый, а не такой, каким их отдал
 * кабинет: по списку места владелец возвращается к той же кассе,
 * и строки не должны меняться местами между перерисовками.
 *
 * @param cell сторона клетки в точках полотна — [groupCell] при плотности экрана.
 */
fun kkmGroups(placed: List<PlacedKkm>, zoom: Int, cell: Double = groupCell(1f)): List<KkmGroup> =
    placed
        .groupBy { MapProjection.cell(it.latitude, it.longitude, zoom, cell) }
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
 * Клетка задана в точках интерфейса ([AnalyticsLayout.mapCell]), как
 * и кружки, а полотно карты меряется точками экрана: на экране двойной
 * плотности клетка в прежние 64 точки полотна была вдвое уже самого
 * крупного кружка, и кружки соседних клеток ложились один на другой.
 */
fun groupCell(density: Float): Double = AnalyticsLayout.mapCell.value.toDouble() * density

/**
 * Кассы одного места — одним ярлычком.
 *
 * Карта показывала каждую кассу своей булавкой, и в торговой точке
 * с тремя кассами булавки садились одна на другую: владелец видел одну
 * и не мог добраться до остальных. Так же устроены карты объявлений:
 * в ярлычке стоит число, по нажатию открывается список того, что в этом
 * доме, и только оттуда заходят в одно.
 *
 * Место считается клеткой полотна на текущем увеличении, а не адресом
 * и не торговой точкой: адреса у соседних касс бывают записаны по-разному,
 * а сойтись на карте они всё равно сойдутся. С приближением клетка
 * мельчает, и ярлычок распадается на свои кассы сам.
 */
data class KkmGroup(val latitude: Double, val longitude: Double, val kkms: List<PlacedKkm>) {

    /** Имя ярлычка: по нему показ помнит, какой из них раскрыт. */
    val id: String get() = kkms.first().kkm.cashRegisterId

    val size: Int get() = kkms.size

    /** Торговая точка места, если все кассы ярлычка стоят в одной. */
    val place: String? get() = kkms.mapNotNull { it.kkm.retailPlaceName }.distinct().singleOrNull()

    /** Адрес места: у касс одной клетки он один и тот же. */
    val address: String? get() = kkms.firstNotNullOfOrNull { it.kkm.address?.takeIf(String::isNotBlank) }

    /** Держит ли ярлычок эту кассу. */
    fun holds(cashRegisterId: String?): Boolean = kkms.any { it.kkm.cashRegisterId == cashRegisterId }
}
