package kz.mybrain.superkassa.presentation.analytics.map

import kz.mybrain.superkassa.domain.analytics.model.AddressAnswer
import kz.mybrain.superkassa.domain.analytics.model.AddressLookup
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.PlacedKkm
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.presentation.analytics.common.Reading
import kz.mybrain.superkassa.presentation.common.mapview.MapState

/**
 * Состояние карты касс.
 *
 * @param source откуда брать положение касс. Раздел открывается на
 *   координатах кабинета: их кабинет отдаёт готовыми на всю сеть, и карта
 *   заполняется сразу. Адрес торговой точки приходит без координат вовсе —
 *   дом по нему ищет служба карт, по адресу в секунду, и сеть из тысячи
 *   адресов собиралась бы на карте четверть часа.
 * @param chosen касса, карточку которой сейчас читают.
 * @param spot раскрытое место: ярлычок, список касс которого стоит под
 *   картой. Отдельно от выбранной кассы: из списка места заходят в кассу,
 *   и вернуться к соседям владелец должен без нового поиска по карте.
 * @param opened касса, аналитику которой открыли отдельным окном.
 * @param sieve отбор касс: он сужает и карту, и список рядом с ней.
 * @param map где стоит карта; держатель положения и увеличения, как
 *   состояние прокрутки у списка. Новый на каждый ответ кабинета.
 * @param found что служба карт ответила об адресах торговых точек.
 * @param steered распорядился ли картой владелец выбором кассы или
 *   ярлычка; рука на самой карте считается в [MapState.steered].
 */
data class AnalyticsMapUiState(
    val source: PositionSource = PositionSource.CabinetCoordinates,
    val reading: Reading<KkmMapView> = Reading(),
    val chosen: String? = null,
    val spot: String? = null,
    val opened: AnalyticsKkm? = null,
    val sieve: MapSieve = MapSieve(),
    val map: MapState = MapState(),
    val found: Map<String, AddressAnswer> = emptyMap(),
    val steered: Boolean = false
) : AddressLookup {

    /** Неизвестный адрес ещё ищется: говорить о нём «не нашли» было бы неправдой. */
    override fun answer(address: String): AddressAnswer = found[address.trim()] ?: AddressAnswer.Searching

    /** Адреса, дом которых уже найден: их служба карт второй раз не спрашивает. */
    val foundAddresses: Set<String> get() = found.filterValues { it is AddressAnswer.Found }.keys

    /** Выбор забыт: и при новом ответе кабинета, и при нажатии мимо ярлычка. */
    fun forgotten(): AnalyticsMapUiState = copy(chosen = null, spot = null)
}

/**
 * Что владелец делает на карте касс.
 *
 * Пустые по умолчанию: снимок вида рисует карту без модели и отдаёт
 * `object : AnalyticsMapActions {}`.
 */
internal interface AnalyticsMapActions {
    fun choose(source: PositionSource) {}
    fun refresh() {}
    fun sift(sieve: MapSieve) {}
    fun open(group: KkmGroup) {}
    fun show(row: PlacedKkm, groups: List<KkmGroup>) {}
    fun pick(kkm: String?) {}
    fun forget() {}
    fun openSales(kkm: AnalyticsKkm?) {}
}
