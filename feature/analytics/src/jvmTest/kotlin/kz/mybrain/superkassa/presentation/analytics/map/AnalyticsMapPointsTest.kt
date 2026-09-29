package kz.mybrain.superkassa.presentation.analytics.map

import kz.mybrain.superkassa.domain.analytics.model.AddressAnswer
import kz.mybrain.superkassa.domain.analytics.model.AddressLookup
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.KkmPosition
import kz.mybrain.superkassa.domain.analytics.model.PlacedKkm
import kz.mybrain.superkassa.domain.analytics.model.PlacementTrouble
import kz.mybrain.superkassa.domain.analytics.model.addressesToFind
import kz.mybrain.superkassa.domain.analytics.model.placement
import kz.mybrain.superkassa.presentation.common.mapview.MapProjection
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Десятичные градусы — в точку окна карты и обратно.
 *
 * Это тот пересчёт, ошибка в котором не видна в коде и видна на экране:
 * кассы Алматы становятся в Караганду. Поэтому проверяются и сам
 * перевод, и то, как раскладываются кассы без координат.
 */
class AnalyticsMapPointsTest {

    private val almatyLatitude = 43.238949
    private val almatyLongitude = 76.889709
    private val zoom = 12
    private val width = 800
    private val height = 600

    private fun corner() =
        MapProjection.corner(almatyLatitude, almatyLongitude, zoom, width, height)

    @Test
    fun `центр карты приходится на середину окна`() {
        val at = MapProjection.screen(almatyLatitude, almatyLongitude, zoom, corner())
        assertTrue(abs(at.x - width / 2.0) < TOLERANCE, "x=${at.x}")
        assertTrue(abs(at.y - height / 2.0) < TOLERANCE, "y=${at.y}")
    }

    @Test
    fun `севернее и восточнее — выше и правее`() {
        val north = MapProjection.screen(almatyLatitude + STEP, almatyLongitude, zoom, corner())
        val east = MapProjection.screen(almatyLatitude, almatyLongitude + STEP, zoom, corner())
        assertTrue(north.y < height / 2.0, "север оказался ниже центра: ${north.y}")
        assertTrue(east.x > width / 2.0, "восток оказался левее центра: ${east.x}")
    }

    @Test
    fun `перевод в точку и обратно в градусы сходится`() {
        val at = MapProjection.screen(almatyLatitude, almatyLongitude, zoom, corner())
        val back = corner()
        val latitude = MapProjection.latitudeOf(back.y + at.y, zoom)
        val longitude = MapProjection.longitudeOf(back.x + at.x, zoom)
        assertTrue(abs(latitude - almatyLatitude) < DEGREE_TOLERANCE, "широта $latitude")
        assertTrue(abs(longitude - almatyLongitude) < DEGREE_TOLERANCE, "долгота $longitude")
    }

    @Test
    fun `увеличение подбирается так, чтобы весь набор помещался в окно`() {
        val one = fitting(listOf(placed("c1", almatyLatitude, almatyLongitude)), width, height)
        val two = fitting(
            listOf(
                placed("c1", almatyLatitude, almatyLongitude),
                placed("c2", 51.089753, 71.406166)
            ),
            width,
            height
        )
        assertTrue(one != null && two != null)
        assertTrue(two.zoom < one.zoom, "сеть по стране взяла увеличение ${two.zoom} против ${one.zoom}")
        assertNull(fitting(emptyList(), width, height))
    }

    /** Одна касса с координатами в Европе растягивала охват сети на пол-Евразии. */
    @Test
    fun `касса с координатами вне страны охват не растягивает`() {
        val home = listOf(placed("c1", almatyLatitude, almatyLongitude), placed("c2", 51.089753, 71.406166))
        val stray = placed("c3", 52.520008, 13.404954)

        assertEquals(fitting(home, width, height), fitting(home + stray, width, height))
        assertEquals(stray.latitude, fitting(listOf(stray), width, height)?.latitude, "касса вне страны — одна")
    }

    @Test
    fun `кассы с координатами кабинета встают на карту сразу`() {
        val view = KkmMapView(
            placed = listOf(kkm("c1", position = KkmPosition(latitude = degrees(43.2), longitude = degrees(76.8)))),
            withoutPosition = listOf(kkm("c2"))
        )
        val spread = placement(view) { AddressAnswer.Missing }
        assertEquals(listOf("c1"), spread.placed.map { it.kkm.cashRegisterId })
        assertEquals(PlacementTrouble.NoPosition, spread.unplaced.single().reason)
    }

    @Test
    fun `при адресе торговой точки касса ждёт карту, а не пропадает`() {
        val view = KkmMapView(placed = listOf(kkm("c1", address = "г. Алматы, пр. Абая, 10")))
        val waiting = placement(view) { AddressAnswer.Searching }
        assertEquals(PlacementTrouble.Searching, waiting.unplaced.single().reason)

        val found = placement(view) { AddressAnswer.Found(43.2, 76.8) }
        assertEquals(1, found.placed.size)
        assertTrue(found.unplaced.isEmpty())

        val missing = placement(view) { AddressAnswer.Missing }
        assertEquals(PlacementTrouble.NotOnMap, missing.unplaced.single().reason)
    }

    @Test
    fun `на карте спрашиваются только адреса без координат и по одному разу`() {
        val view = KkmMapView(
            placed = listOf(
                kkm("c1", address = "г. Алматы, пр. Абая, 10"),
                kkm("c2", address = " г. Алматы, пр. Абая, 10 "),
                kkm("c3", address = "г. Актобе, ул. Абилкайыр хана, 40"),
                kkm(
                    "c4",
                    address = "Есть свои",
                    position = KkmPosition(latitude = degrees(43.2), longitude = degrees(76.8))
                )
            )
        )
        assertEquals(
            listOf("г. Алматы, пр. Абая, 10", "г. Актобе, ул. Абилкайыр хана, 40"),
            addressesToFind(view)
        )
    }

    /**
     * Счётчик над картой различает «ищем» и «поставить некуда».
     *
     * При адресе торговой точки через поиск проходит вся сеть, и одним
     * числом она писала бы «без положения» о кассах, у которых адрес есть.
     */
    @Test
    fun `ищущиеся кассы не числятся без положения`() {
        val view = KkmMapView(
            placed = listOf(kkm("c1", address = "г. Алматы, пр. Абая, 10")),
            withoutPosition = listOf(kkm("c2"))
        )
        val waiting = placement(view) { AddressAnswer.Searching }
        assertEquals(1, waiting.searching, "касса с адресом не сосчитана ищущейся")
        assertEquals(1, waiting.nowhere, "без положения оказалась не одна касса")

        val missing = placement(view) { AddressAnswer.Missing }
        assertEquals(0, missing.searching)
        assertEquals(2, missing.nowhere, "ненайденный адрес — это уже не поиск")
    }

    @Test
    fun `пустой ответ кабинета не даёт ни одной точки и ни одной строки`() {
        val nothing = placement(null, AddressLookup { AddressAnswer.Missing })
        assertTrue(nothing.placed.isEmpty() && nothing.unplaced.isEmpty())
    }

    private fun placed(id: String, latitude: Double, longitude: Double) = PlacedKkm(kkm(id), latitude, longitude)

    private fun kkm(id: String, address: String? = null, position: KkmPosition? = null) =
        AnalyticsKkm(cashRegisterId = id, kkmId = 2000302, address = address, position = position)

    private fun degrees(value: Double): Double = value

    private companion object {
        const val TOLERANCE = 0.001
        const val DEGREE_TOLERANCE = 0.000001
        const val STEP = 0.05
    }
}
