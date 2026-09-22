package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmPosition
import kz.mybrain.superkassa.desktop.ui.analytics.KkmGroup
import kz.mybrain.superkassa.desktop.ui.analytics.PlacedKkm
import kz.mybrain.superkassa.desktop.ui.map.MapState
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.desktop.ui.analytics.kkmGroups
import kz.mybrain.superkassa.desktop.ui.analytics.onScreen
import java.math.BigDecimal
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Сеть в две тысячи касс по всему Казахстану.
 *
 * Сейчас у кабинета единицы касс, а к показу их будет столько же, сколько
 * торговых точек в стране. Проверяется здесь то, что на этом числе
 * ломается первым: сведение касс в кружки должно оставаться разборкой
 * по клеткам, а не сравнением каждой кассы с каждой, кружки на карте
 * страны — собираться в десятки, а не в тысячи, и с приближением
 * распадаться.
 */
class AnalyticsMapCrowdTest {

    /**
     * Карта страны сводит сеть в десятки кружков, а не в кашу из меток.
     *
     * Верхняя граница — сколько клеток по 56 точек помещается в полотно
     * страны: больше кружков, чем клеток, быть не может вовсе, и если
     * их вышло под тысячу, значит сведение не сработало.
     */
    @Test
    fun `две тысячи касс на карте страны сводятся в десятки кружков`() {
        val groups = kkmGroups(crowd(), COUNTRY_ZOOM)

        assertTrue(groups.size < GROUPS_AT_COUNTRY, "на карте страны вышло ${groups.size} кружков")
        assertTrue(groups.sumOf { it.size } == CROWD, "часть касс потерялась при сведении")
        assertTrue(groups.maxOf { it.size } > TEN, "ни один кружок не собрал даже десятка касс")
    }

    /** С приближением кружки распадаются: на квартале каждая касса сама по себе. */
    @Test
    fun `с приближением кружки распадаются`() {
        val placed = crowd()

        val country = kkmGroups(placed, COUNTRY_ZOOM).size
        val city = kkmGroups(placed, CITY_ZOOM).size
        val house = kkmGroups(placed, HOUSE_ZOOM).size

        assertTrue(country < city, "на городе кружков не стало больше: $country и $city")
        assertTrue(city < house, "на квартале кружков не стало больше: $city и $house")
        assertTrue(house > CROWD / 2, "на квартале касса так и не отделилась от соседей: $house")
    }

    /**
     * Сведение линейно по числу касс, а не квадратично.
     *
     * Меряется отношением, а не абсолютным временем: на чужой машине
     * и под чужой загрузкой миллисекунды будут другими, а отношение
     * восьмикратного набора к однократному — то же. Квадратичный разбор
     * дал бы шестьдесят четыре, линейный — около восьми; порог взят
     * с большим запасом на разогрев виртуальной машины.
     */
    @Test
    fun `сведение не квадратично по числу касс`() {
        val small = crowd(CROWD / EIGHT)
        val whole = crowd(CROWD)
        repeat(WARMUP) { kkmGroups(whole, COUNTRY_ZOOM) }

        val one = measureTimeMillis { repeat(ROUNDS) { kkmGroups(small, COUNTRY_ZOOM) } }.coerceAtLeast(1)
        val eight = measureTimeMillis { repeat(ROUNDS) { kkmGroups(whole, COUNTRY_ZOOM) } }

        assertTrue(eight < one * EIGHT * SLACK, "восьмикратный набор занял $eight мс против $one мс")
    }

    /**
     * Итог над картой считается по видимому куску, а не по всей сети.
     *
     * Владелец двигает карту и спрашивает её глазами «сколько здесь»;
     * число, посчитанное по всей сети, отвечало бы не на этот вопрос.
     */
    @Test
    fun `видимым считается только то, что попало в окно`() {
        val placed = crowd()
        val groups = kkmGroups(placed, REGION_ZOOM)
        val atAlmaty = MapState(ALMATY_LATITUDE, ALMATY_LONGITUDE, REGION_ZOOM)
        val atAstana = MapState(ASTANA_LATITUDE, ASTANA_LONGITUDE, REGION_ZOOM)
        val canvas = IntSize(WINDOW_WIDE, WINDOW_HIGH)

        val almaty = onScreen(groups, atAlmaty, canvas)
        val astana = onScreen(groups, atAstana, canvas)

        assertTrue(almaty.isNotEmpty() && astana.isNotEmpty(), "в окно не попало ни одного места")
        assertTrue(almaty.size < groups.size, "в окно попала вся сеть: ${almaty.size}")
        assertTrue(almaty.map { it.id } != astana.map { it.id }, "сдвиг карты не поменял видимое")
    }

    /** Окно нулевого размера — ещё не окно: до первой отрисовки рисовать нечего. */
    @Test
    fun `до первой отрисовки видимого нет`() {
        val groups = kkmGroups(crowd(), CITY_ZOOM)

        assertTrue(onScreen(groups, MapState(), IntSize.Zero).isEmpty())
    }

    private companion object {

        const val ALMATY_LATITUDE = 43.238949
        const val ALMATY_LONGITUDE = 76.889709
        const val ASTANA_LATITUDE = 51.180100
        const val ASTANA_LONGITUDE = 71.446000
        const val WINDOW_WIDE = 840
        const val WINDOW_HIGH = 560
        /** Увеличение, на котором в окно попадает область, а не вся страна. */
        const val REGION_ZOOM = 6

        const val CROWD = 2000
        const val COUNTRY_ZOOM = 5
        const val CITY_ZOOM = 12
        const val HOUSE_ZOOM = 18
        const val TEN = 10
        const val EIGHT = 8
        const val WARMUP = 20
        const val ROUNDS = 20
        const val SLACK = 4
        const val GROUPS_AT_COUNTRY = 400
    }
}

/**
 * Кассы, раскиданные по Казахстану: широта 40,5–55,5, долгота 46,5–87,5.
 *
 * Зерно задано числом: набор должен быть один и тот же от прогона
 * к прогону, иначе проверка о числе кружков то проходит, то нет.
 * Каждая двадцатая касса заблокирована, каждая седьмая торгует —
 * так в кружках встречаются все три цвета.
 */
internal fun crowd(size: Int = 2000): List<PlacedKkm> {
    val dice = Random(20260922)
    return (1..size).map { at ->
        val latitude = 40.5 + dice.nextDouble() * 15.0
        val longitude = 46.5 + dice.nextDouble() * 41.0
        PlacedKkm(crowdKkm(at), latitude, longitude)
    }
}

/** Касса сети: своё имя, своя точка и своё состояние. */
internal fun crowdKkm(at: Int): AnalyticsKkm = AnalyticsKkm(
    cashRegisterId = "c$at",
    kkmId = 2000300 + at,
    registrationNumber = "%012d".format(4500000L + at),
    internalName = "Касса $at",
    retailPlaceId = "p-${at / 3}",
    retailPlaceName = "Магазин ${at / 3}",
    address = "г. Алматы, пр. Абая, $at",
    status = if (at % 50 == 0) "DEREGISTERED" else "REGISTERED",
    blocked = at % 20 == 0,
    shiftStatus = if (at % 7 == 0) "OPEN" else "CLOSED",
    shiftNumber = at.toLong(),
    lastContactAt = "2026-09-22T09:15:00Z"
)

/** Та же сеть ответом кабинета: координаты готовые, искать по адресу нечего. */
internal fun crowdView(placed: List<PlacedKkm>) = Look.view(
    placed.map { row ->
        row.kkm.copy(
            position = KkmPosition(
                latitude = BigDecimal.valueOf(row.latitude),
                longitude = BigDecimal.valueOf(row.longitude)
            )
        )
    }
)

/** Сколько касс сошлось в самом крупном кружке: по нему видно, что сведение работает. */
internal fun List<KkmGroup>.biggest(): Int = maxOf { it.size }
