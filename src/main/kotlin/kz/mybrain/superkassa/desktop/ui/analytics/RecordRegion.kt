package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm

/**
 * Учёт касс по регионам страны.
 *
 * Вопрос, который задают парку в три тысячи касс первым: где он стоит
 * и где учёт не доведён до конца. Тремя тысячами строк на это не ответить,
 * а двумя десятками областей — можно.
 *
 * Свой счёт региону не заводится: в каждом регионе считается тот же
 * [RecordCount], что и по всей сети. Тогда строка области и строка итога
 * читаются одинаково, и сложить их по-разному нельзя.
 */
data class RecordRegion(val title: String, val count: RecordCount)

/**
 * Свод парка по регионам.
 *
 * Порядок — по числу касс, с самого крупного: с него читать и начинают.
 * При равенстве — по названию, иначе две области одинакового размера
 * менялись бы местами от ответа к ответу и читались как изменение.
 */
fun recordRegions(kkms: List<AnalyticsKkm>, unknown: String): List<RecordRegion> = kkms
    .groupBy { regionOf(it.address, unknown) }
    .map { (title, rows) -> RecordRegion(title, recordCount(rows)) }
    .sortedWith(compareByDescending<RecordRegion> { it.count.total }.thenBy { it.title })
