package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts

/** Что известно об адресе торговой точки: ещё ищем, нашли или не нашли. */
sealed interface AddressAnswer {
    data object Searching : AddressAnswer
    data class Found(val latitude: Double, val longitude: Double) : AddressAnswer
    data object Missing : AddressAnswer
}

/** Кто отвечает на вопрос о координатах адреса. */
fun interface AddressLookup {
    fun answer(address: String): AddressAnswer
}

/** Касса, поставленная на карту. */
data class PlacedKkm(val kkm: AnalyticsKkm, val latitude: Double, val longitude: Double)

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

/** Касса, которую поставить не удалось, и почему. */
data class UnplacedKkm(val kkm: AnalyticsKkm, val reason: PlacementTrouble)

/** Почему кассы нет на карте. */
enum class PlacementTrouble { NoPosition, Searching, NotOnMap }

/** Кассы, разложенные на поставленные и непоставленные. */
data class Placement(val placed: List<PlacedKkm>, val unplaced: List<UnplacedKkm>)

/**
 * Раскладывает ответ кабинета по карте.
 *
 * Два источника из трёх приходят с готовыми координатами, и работы здесь
 * нет. Третий — адрес торговой точки: координат у адресного регистра нет
 * вовсе, и точку по адресу ищет карта. Пока она ищет, касса не пропадает
 * с экрана: она стоит в списке рядом с картой со словами о том, что
 * сейчас с ней происходит.
 *
 * Кассы, которые кабинет и сам поставить не смог, доходят сюда своим
 * списком и остаются в нём: почему именно — зависит от источника.
 */
fun placement(view: KkmMapView?, lookup: AddressLookup): Placement {
    if (view == null) return Placement(emptyList(), emptyList())
    val placed = mutableListOf<PlacedKkm>()
    val unplaced = view.withoutPosition.map { UnplacedKkm(it, PlacementTrouble.NoPosition) }.toMutableList()
    view.placed.forEach { kkm ->
        when (val found = pointOf(kkm, lookup)) {
            is AddressAnswer.Found -> placed += PlacedKkm(kkm, found.latitude, found.longitude)
            AddressAnswer.Searching -> unplaced += UnplacedKkm(kkm, PlacementTrouble.Searching)
            AddressAnswer.Missing -> unplaced += UnplacedKkm(kkm, PlacementTrouble.NotOnMap)
        }
    }
    return Placement(placed, unplaced)
}

/** Координаты кассы: свои, если кабинет их дал, иначе найденные по адресу. */
private fun pointOf(kkm: AnalyticsKkm, lookup: AddressLookup): AddressAnswer {
    val position = kkm.position
    val latitude = position?.latitude
    val longitude = position?.longitude
    if (latitude != null && longitude != null) {
        return AddressAnswer.Found(latitude.toDouble(), longitude.toDouble())
    }
    val address = kkm.address?.takeIf { it.isNotBlank() } ?: return AddressAnswer.Missing
    return lookup.answer(address)
}

/** Адреса, которые предстоит найти на карте: без повторов и пустых. */
fun addressesToFind(view: KkmMapView?): List<String> =
    view?.placed.orEmpty()
        .filter { it.position?.degrees != true }
        .mapNotNull { it.address?.trim()?.takeIf(String::isNotBlank) }
        .distinct()

/**
 * Почему кассы нет на карте — словами владельца.
 *
 * У «положения нет вовсе» причина зависит от источника: при адресе
 * не выбран адрес, при координатах кабинета их не задал владелец,
 * при координатах кассы машина ни разу о себе не сообщила.
 */
/**
 * Роль цвета причины: поиск — ожидание, а не отказ.
 *
 * Пока адрес ищется на карте, строка красной быть не должна: красным
 * владелец читает «починить», а чинить здесь нечего — через миг касса
 * встанет на карту сама.
 */
fun troubleTone(reason: PlacementTrouble): StatusTone = when (reason) {
    PlacementTrouble.Searching -> StatusTone.Waiting
    PlacementTrouble.NotOnMap, PlacementTrouble.NoPosition -> StatusTone.Bad
}

fun troubleWords(reason: PlacementTrouble, source: PositionSource, texts: AnalyticsTexts): String = when (reason) {
    PlacementTrouble.Searching -> texts.reasonSearching
    PlacementTrouble.NotOnMap -> texts.reasonNotOnMap
    PlacementTrouble.NoPosition -> when (source) {
        PositionSource.RetailPlaceAddress -> texts.reasonNoAddress
        PositionSource.CabinetCoordinates -> texts.reasonNoCabinetPoint
        PositionSource.KkmCoordinates -> texts.reasonNoKkmPoint
    }
}
