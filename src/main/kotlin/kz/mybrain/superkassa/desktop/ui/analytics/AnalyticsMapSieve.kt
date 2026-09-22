package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
import kz.mybrain.superkassa.desktop.ui.cabinet.kkmRecord
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts

/**
 * Отбор касс на карте.
 *
 * Сеть в сотню касс на карте города — это сотня одинаковых ярлычков,
 * и ответа на вопрос владельца «где у меня открыта смена» в ней нет.
 * Отбор отвечает: он оставляет на карте только то, о чём спросили,
 * и список рядом с картой сужается вместе с ней — иначе владелец читал
 * бы в списке кассы, которых на карте уже нет.
 */
data class MapSieve(
    val needle: String = "",
    val place: String? = null,
    val record: KkmRecord? = null,
    val marks: Set<KkmMark> = emptySet()
) {
    /** Задан ли отбор хоть чем-нибудь: по этому плашки показывают, что они не пустые. */
    val set: Boolean get() = needle.isNotBlank() || place != null || record != null || marks.isNotEmpty()
}

/**
 * Признак кассы, по которому её отбирают.
 *
 * Перечисление одно и то же и для плашек отбора, и для самого отбора:
 * добавленный признак появляется в ряду плашек и начинает работать
 * без второй правки.
 */
enum class KkmMark(val title: (AnalyticsTexts) -> String, val holds: (AnalyticsKkm) -> Boolean) {

    /** Смена открыта: касса сейчас торгует. */
    ShiftOpen({ it.markShiftOpen }, { it.shiftStatus == SHIFT_OPEN }),

    /** Касса заблокирована: фискальных операций не выполняет. */
    Blocked({ it.markBlocked }, { it.blocked })
}

/**
 * Учёт КГД — не плашкой, а выбором из списка.
 *
 * Смыслы учёта исключают друг друга: касса не бывает разом на учёте
 * и снятой с него. Пять нажимаемых плашек в ряду обещали бы обратное —
 * нажав две, владелец получал бы пустую карту, — а ряд отбора от них
 * переносился на вторую строку и забирал высоту у самой карты.
 */
fun recordTitle(record: KkmRecord?, texts: AnalyticsTexts): String = when (record) {
    null -> texts.allRecords
    KkmRecord.OnRecord -> texts.markOnRecord
    KkmRecord.Entered -> texts.markEntered
    KkmRecord.Applied -> texts.markApplied
    KkmRecord.Refused -> texts.markRefused
    KkmRecord.Deregistered -> texts.markDeregistered
}

/**
 * Что КГД знает об этой кассе.
 *
 * Все плашки учёта и цвет ярлычка спрашивают одно и то же, и спрашивают
 * здесь: прежде набор «на учёте» лежал в трёх файлах тремя копиями.
 */
val AnalyticsKkm.record: KkmRecord get() = kkmRecord(status)

/** Торговая точка для плашки отбора: чем её звать и что отбирать. */
data class SievePlace(val id: String, val name: String)

/** Оставляет ли отбор эту кассу. */
fun MapSieve.keeps(kkm: AnalyticsKkm): Boolean =
    marks.all { it.holds(kkm) } && keepsRecord(kkm) && keepsPlace(kkm) && keepsNeedle(kkm)

private fun MapSieve.keepsRecord(kkm: AnalyticsKkm): Boolean =
    record == null || kkm.record == record

private fun MapSieve.keepsPlace(kkm: AnalyticsKkm): Boolean =
    place == null || kkm.retailPlaceId == place

/**
 * Поиск по набранному — там, где владелец помнит кассу.
 *
 * Своё имя, регистрационный номер и торговая точка: по номеру КГД кассу
 * ищут в заявлении, а глазами владелец помнит её как «второй зал».
 */
private fun MapSieve.keepsNeedle(kkm: AnalyticsKkm): Boolean {
    val text = needle.trim()
    if (text.isEmpty()) return true
    return listOfNotNull(
        kkm.internalName,
        kkm.registrationNumber,
        kkm.retailPlaceName,
        kkm.address,
        kkm.kkmId.takeIf { it != 0 }?.toString()
    ).any { it.contains(text, ignoreCase = true) }
}

/** Отбор применяется к обеим половинам: и к карте, и к списку рядом с ней. */
fun sieved(placement: Placement, sieve: MapSieve): Placement = Placement(
    placed = placement.placed.filter { sieve.keeps(it.kkm) },
    unplaced = placement.unplaced.filter { sieve.keeps(it.kkm) }
)

/** Торговые точки ответа кабинета — для плашки отбора по точке. */
fun sievePlaces(view: KkmMapView?): List<SievePlace> =
    (view?.placed.orEmpty() + view?.withoutPosition.orEmpty())
        .mapNotNull { kkm ->
            val id = kkm.retailPlaceId?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            SievePlace(id, kkm.retailPlaceName?.takeIf(String::isNotBlank) ?: id)
        }
        .distinctBy { it.id }
        .sortedBy { it.name }

/** Состояние смены, при котором касса торгует. */
private const val SHIFT_OPEN = "OPEN"
