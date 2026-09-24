package kz.mybrain.superkassa.presentation.common.mapview

/** Место, к которому карта едет: широта и долгота цели перехода. */
data class MapGoal(val latitude: Double, val longitude: Double)

/** Увеличение, на котором виден город: с него начинается найденное по адресу подключения. */
const val CITY_ZOOM = 12

/** Увеличение, на котором различимы дома: на нём открывается найденный адрес. */
const val HOUSE_ZOOM = 17

/** Самое мелкое увеличение карты: дальше страна теряется в пол-экрана. */
internal const val MIN_ZOOM = 3

/** Самое крупное увеличение: крупнее плиток у службы нет. */
internal const val MAX_ZOOM = 18

/** Середина Алматы: с чего-то карта начинаться должна. */
internal const val START_LATITUDE = 43.238949

/** Долгота середины Алматы. */
internal const val START_LONGITUDE = 76.889709
