package kz.mybrain.superkassa.domain.map.model

/**
 * Чем кончилось определение своего места.
 *
 * Своё место машина знает с точностью до дома, адрес подключения — до
 * города поставщика связи; экран обязан различать их. Спросить о запасном
 * пути нужно владельца: адрес подключения уходит чужой службе.
 */
sealed interface SelfPlace {

    /** Место известно; [precise] — от самой машины, до дома. */
    data class Found(val place: MapPlace, val precise: Boolean) : SelfPlace

    /** Машина места не знает, а запасной путь владелец не разрешал: спросить его. */
    data object AskOwner : SelfPlace

    /** Места нет: служба не ответила или владелец запасной путь запретил. */
    data object Unknown : SelfPlace
}
