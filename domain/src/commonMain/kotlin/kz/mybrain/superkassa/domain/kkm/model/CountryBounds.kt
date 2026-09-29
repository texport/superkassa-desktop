package kz.mybrain.superkassa.domain.kkm.model

/**
 * Прямоугольник, в который вписан Казахстан, — широты и долготы с запасом
 * в десятую долю градуса.
 *
 * Касса стоит в Казахстане, и точка вне него — ошибка адреса или
 * координат в кабинете. По таким точкам карту не охватывают: одна касса
 * с координатами в Европе растягивала охват на пол-Евразии, и сеть
 * из Алматы и Астаны превращалась в две точки посреди чужих стран.
 */
object CountryBounds {
    private const val SOUTH = 40.5
    private const val NORTH = 55.5
    private const val WEST = 46.4
    private const val EAST = 87.4

    /** Лежит ли точка в пределах страны. */
    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in SOUTH..NORTH && longitude in WEST..EAST
}
