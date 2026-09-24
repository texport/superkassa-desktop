package kz.mybrain.superkassa.presentation.map

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.cabinet.model.AddressLevel
import kz.mybrain.superkassa.domain.cabinet.model.AddressSuggestion
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.domain.map.model.MapPointPlace

/**
 * Адресный регистр глазами окна карты.
 *
 * Регистр держит кабинет, а окно карты о кабинете не знает: регистр ему
 * отдаёт форма точки, которая окно открыла. Отказ регистра здесь
 * не отличается от пустого ответа — на отказ владельцу отвечает общая
 * строка сообщений, а подбору хватает того, что записи не нашлось.
 */
interface MapRegistry {

    /** Вошёл ли владелец в кабинет: без входа регистр не отвечает. */
    val open: Boolean

    /** Записи уровня [level] под родителем [parentId], подходящие под [query]. */
    suspend fun lookUp(level: AddressLevel, parentId: Long, query: String): List<AddressSuggestion>

    /** Адрес регистра по коду РКА; `null` — регистр его не выдал. */
    suspend fun resolve(rka: String): RegisterAddress?

    /**
     * Подбор адреса по шагам регистра — та же форма, что у точки.
     *
     * @param query подпись выбранного адреса; после выбора поле заполняется им.
     * @param owner чей адрес подбирается: смена владельца сбрасывает начатый путь.
     */
    @Composable
    fun Search(query: String, onQuery: (String) -> Unit, owner: Any?, onChoose: (RegisterAddress) -> Unit)
}

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

/** Регион ищется сам по себе: родителя у него нет. */
private const val NO_PARENT = 0L

/** Шаги регистра, взятые у регистра окна: второго пути к регистру нет. */
internal class RegistryWalkSteps(private val registry: MapRegistry) : RegistrySteps {

    override suspend fun regions(query: String): List<AddressSuggestion> =
        registry.lookUp(AddressLevel.Region, NO_PARENT, query)

    override suspend fun localities(parentId: Long, query: String): List<AddressSuggestion> =
        registry.lookUp(AddressLevel.Locality, parentId, query)

    override suspend fun streets(localityId: Long, query: String): List<AddressSuggestion> =
        registry.lookUp(AddressLevel.Street, localityId, query)

    override suspend fun buildings(streetId: Long, number: String): List<AddressSuggestion> =
        registry.lookUp(AddressLevel.Building, streetId, number)
}
