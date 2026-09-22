package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord

/**
 * Парк касс в числах: сколько их, как они учтены и работают ли сейчас.
 *
 * Один и тот же счёт годится и на всю сеть, и на один регион — потому
 * он и объявлен типом, а не девятью числами, разложенными по вёрстке:
 * перепутанные местами «заведены» и «на учёте» не заметил бы ни
 * разработчик, ни проверка.
 *
 * Смыслы учёта берутся у [KkmRecord] и здесь не пересказываются: кабинет
 * различает одиннадцать состояний, и второй разбор тех же кодов разошёлся
 * бы с первым на первом же новом коде.
 *
 * @param total касс всего — столько их заведено в кабинете.
 * @param entered из них заведённых, по которым заявление не подавалось.
 * @param applied тех, по которым заявление подано и ждёт ответа КГД.
 * @param places торговых точек, на которых эти кассы стоят.
 * @param trading касс с открытой сменой — и только из стоящих на учёте,
 *   см. [recordCount].
 * @param blocked касс, которым фискальные операции закрыты.
 */
data class RecordCount(
    val total: Int,
    val entered: Int,
    val applied: Int,
    val onRecord: Int,
    val refused: Int,
    val deregistered: Int,
    val places: Int,
    val trading: Int,
    val blocked: Int
) {
    /** Есть ли что показывать: пустой парк экран объясняет словами. */
    val empty: Boolean get() = total == 0
}

/** Все кассы компании: и поставленные на карту, и те, которых поставить некуда. */
fun recordKkms(view: KkmMapView?): List<AnalyticsKkm> =
    view?.placed.orEmpty() + view?.withoutPosition.orEmpty()

/**
 * Парк касс, сосчитанный по смыслам учёта и по работе прямо сейчас.
 *
 * Торгующими считаются только кассы на учёте: смена бывает открыта
 * и у кассы, которую КГД не учёл, но торговлей по закону это не является,
 * и считать её работой сети значит обещать министру то, чего нет.
 * Сколько их из скольких — вкладка говорит рядом с числом.
 *
 * Блокировки считаются по всему парку: блокирует не владелец, и касса
 * вне учёта заблокированной тоже бывает.
 */
fun recordCount(kkms: List<AnalyticsKkm>): RecordCount {
    val byRecord = kkms.groupingBy { it.record }.eachCount()
    return RecordCount(
        total = kkms.size,
        entered = byRecord[KkmRecord.Entered] ?: 0,
        applied = byRecord[KkmRecord.Applied] ?: 0,
        onRecord = byRecord[KkmRecord.OnRecord] ?: 0,
        refused = byRecord[KkmRecord.Refused] ?: 0,
        deregistered = byRecord[KkmRecord.Deregistered] ?: 0,
        places = kkms.mapNotNull(::placeKey).distinct().size,
        trading = kkms.count { it.record == KkmRecord.OnRecord && KkmMark.ShiftOpen.holds(it) },
        blocked = kkms.count(KkmMark.Blocked.holds)
    )
}

/**
 * Кассы, которым КГД отказал.
 *
 * Отдельным списком, а не отбором по месту показа: по отказу владелец
 * действует — разбирает причину и подаёт заявление заново, — и найти
 * такую кассу он должен, не листая весь парк.
 *
 * Порядок — по торговой точке и названию: отказы разбирают точками,
 * а кабинетный порядок ответа о них ничего не знает.
 */
fun refusedKkms(kkms: List<AnalyticsKkm>): List<AnalyticsKkm> = kkms
    .filter { it.record == KkmRecord.Refused }
    .sortedWith(compareBy({ it.retailPlaceName.orEmpty() }, ::kkmTitle))

/**
 * Чем торговая точка отличается от соседней.
 *
 * Ключ, а нет его — название: ключ кабинет присылает не у всякой кассы,
 * а считать две кассы одного магазина двумя точками нельзя. Кассы без
 * точки вовсе в счёт точек не идут.
 */
private fun placeKey(kkm: AnalyticsKkm): String? =
    kkm.retailPlaceId?.takeIf(String::isNotBlank) ?: kkm.retailPlaceName?.takeIf(String::isNotBlank)
