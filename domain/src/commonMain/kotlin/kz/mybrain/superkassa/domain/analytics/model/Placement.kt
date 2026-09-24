package kz.mybrain.superkassa.domain.analytics.model

/**
 * Расстановка касс на карте: какие встали, какие нет и почему.
 *
 * Правило одно на карту, список рядом с ней и счётчики: касса не пропадает
 * из раздела оттого, что её негде поставить, а поиск дома — не отказ.
 */

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

/** Касса, которую поставить не удалось, и почему. */
data class UnplacedKkm(val kkm: AnalyticsKkm, val reason: PlacementTrouble)

/** Почему кассы нет на карте. */
enum class PlacementTrouble { NoPosition, Searching, NotOnMap }

/** Кассы, разложенные на поставленные и непоставленные. */
data class Placement(val placed: List<PlacedKkm>, val unplaced: List<UnplacedKkm>) {

    /**
     * Касс, чей дом карта ещё ищет.
     *
     * Считается отдельно от непоставленных, и считается здесь: счётчик
     * над картой и любая другая надпись о них должны брать одно число.
     */
    val searching: Int get() = unplaced.count { it.reason == PlacementTrouble.Searching }

    /**
     * Касс, которые поставить некуда.
     *
     * Ищущиеся сюда не идут: адрес у них есть, и через минуту они встанут
     * на карту сами. При адресе торговой точки кабинет координат не даёт
     * вовсе, и все три тысячи касс сети по очереди проходят через поиск —
     * сосчитанные без положения, они обещали бы владельцу три тысячи
     * касс без адреса там, где адрес есть у каждой.
     */
    val nowhere: Int get() = unplaced.size - searching
}

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
    val latitude = kkm.position?.latitude
    val longitude = kkm.position?.longitude
    val address = kkm.address?.takeIf { it.isNotBlank() }
    return when {
        latitude != null && longitude != null -> AddressAnswer.Found(latitude, longitude)
        address == null -> AddressAnswer.Missing
        else -> lookup.answer(address)
    }
}

/** Адреса, которые предстоит найти на карте: без повторов и пустых. */
fun addressesToFind(view: KkmMapView?): List<String> =
    view?.placed.orEmpty()
        .filter { it.position?.degrees != true }
        .mapNotNull { it.address?.trim()?.takeIf(String::isNotBlank) }
        .distinct()
