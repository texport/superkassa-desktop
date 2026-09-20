package kz.mybrain.superkassa.desktop.ui.map

import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.AddressSuggestion
import kz.mybrain.superkassa.desktop.server.cabinet.addressBuildings
import kz.mybrain.superkassa.desktop.server.cabinet.addressLocalities
import kz.mybrain.superkassa.desktop.server.cabinet.addressRegions
import kz.mybrain.superkassa.desktop.server.cabinet.addressStreets

/**
 * Шаги адресного регистра, нужные подбору по метке.
 *
 * Те же четыре шага, которыми адрес выбирается руками. Объявлены здесь
 * отдельно от кабинета, чтобы сведение метки с регистром проверялось
 * без поднятого кабинета и без сети.
 */
internal interface RegistrySteps {
    suspend fun regions(query: String): List<AddressSuggestion>
    suspend fun localities(parentId: Long, query: String): List<AddressSuggestion>
    suspend fun streets(localityId: Long, query: String): List<AddressSuggestion>
    suspend fun buildings(streetId: Long, number: String): List<AddressSuggestion>
}

/** На каком шаге подбор по метке остановился: об этом владельцу и говорят. */
enum class PointStep { Place, Region, Locality, Street, House }

/** Чем кончился подбор адреса по метке. */
sealed interface PointMatch {

    /** Дома регистра, отвечающие месту под меткой: выбирает из них владелец. */
    data class Houses(val items: List<AddressSuggestion>) : PointMatch

    /** Регистр не подтвердил место: дальше этого шага подбор не прошёл. */
    data class Missing(val step: PointStep) : PointMatch
}

/**
 * Сведение места под меткой с записями регистра.
 *
 * Проходит те же шаги, что и каскад: регион, пункты, улица, дом. Место
 * даёт названия, регистр — записи; ни одна из них адресом точки сама
 * не становится — дома уходят владельцу списком, и адрес берётся по коду
 * РКА выбранного им дома.
 */
internal suspend fun matchPointPlace(steps: RegistrySteps, place: MapPointPlace): PointMatch {
    val region = matchSuggestion(byStem(addressNeedle(place.region)) { steps.regions(it) }, place.region)
        ?: return PointMatch.Missing(PointStep.Region)
    val localities = localityChain(steps, region.id, place.localities)
    if (localities.isEmpty()) return PointMatch.Missing(PointStep.Locality)
    val street = streetIn(steps, localities, place.street) ?: return PointMatch.Missing(PointStep.Street)
    val houses = matchHouses(steps.buildings(street.id, addressNeedle(place.house)), place.house)
    return if (houses.isEmpty()) PointMatch.Missing(PointStep.House) else PointMatch.Houses(houses)
}

/**
 * Цепочка пунктов от региона вниз.
 *
 * Глубина не фиксирована, как и у каскада: у Караганды районы лежат под
 * городом, у Астаны — прямо под регионом. Поэтому следующее название
 * ищется под уже найденным, а не найденное пропускается: район, не
 * нашедшийся под городом, будет искаться там же, где искался город.
 */
private suspend fun localityChain(
    steps: RegistrySteps,
    regionId: Long,
    names: List<String>
): List<AddressSuggestion> {
    val chain = mutableListOf<AddressSuggestion>()
    var parent = regionId
    names.forEach { name ->
        val found = matchSuggestion(byStem(addressNeedle(name)) { steps.localities(parent, it) }, name)
        if (found != null) {
            chain += found
            parent = found.id
        }
    }
    return chain
}

/**
 * Улица — в самом мелком из найденных пунктов, а если там её нет,
 * то в тех, что крупнее: регистр держит улицы то под городом,
 * то под его районом.
 */
private suspend fun streetIn(
    steps: RegistrySteps,
    localities: List<AddressSuggestion>,
    street: String
): AddressSuggestion? {
    if (street.isBlank()) return null
    return localities.reversed().firstNotNullOfOrNull { locality ->
        matchSuggestion(byStem(addressNeedle(street)) { steps.streets(locality.id, it) }, street)
    }
}

/**
 * Спрашивает регистр целым названием, а не нашлось — его основой.
 *
 * Карта склоняет названия («улица Радостовца»), регистр держит их
 * в именительном («Радостовец»), и запрос целым словом не находил ничего.
 * Основа — то же слово без последних букв; короче [STEM_FLOOR] знаков
 * не укорачиваем: слишком короткий запрос вернёт пол-города.
 */
private suspend fun byStem(
    needle: String,
    fetch: suspend (String) -> List<AddressSuggestion>
): List<AddressSuggestion> {
    val whole = fetch(needle)
    if (whole.isNotEmpty() || needle.length <= STEM_FLOOR) {
        return whole
    }
    return fetch(needle.dropLast(needle.length - needle.length.coerceAtMost(STEM_FLOOR)))
}

/** До скольких знаков укорачивается название при поиске по основе. */
private const val STEM_FLOOR = 5

/**
 * Шаги регистра, взятые у кабинета.
 *
 * Те же вызовы, которыми ходит каскад: второго пути к регистру нет.
 * Отказ кабинета здесь не отличается от пустого ответа — на отказ
 * владельцу отвечает общая строка сообщений, а подбору хватает того,
 * что записи не нашлось.
 */
internal class CabinetRegistrySteps(
    private val cabinet: CabinetSession,
    private val token: String
) : RegistrySteps {

    override suspend fun regions(query: String): List<AddressSuggestion> =
        ask { cabinet.client.addressRegions(token, query).items }

    override suspend fun localities(parentId: Long, query: String): List<AddressSuggestion> =
        ask { cabinet.client.addressLocalities(token, parentId, query).items }

    override suspend fun streets(localityId: Long, query: String): List<AddressSuggestion> =
        ask { cabinet.client.addressStreets(token, localityId, query).items }

    override suspend fun buildings(streetId: Long, number: String): List<AddressSuggestion> =
        ask { cabinet.client.addressBuildings(token, streetId, number).items }

    private suspend fun ask(call: suspend () -> List<AddressSuggestion>): List<AddressSuggestion> =
        cabinet.guard { call() }.orEmpty()
}
