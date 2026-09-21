package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.server.cabinet.KkmPosition
import kz.mybrain.superkassa.desktop.ui.analytics.AddressAnswer
import kz.mybrain.superkassa.desktop.ui.analytics.AddressLookup
import kz.mybrain.superkassa.desktop.ui.analytics.PlacementTrouble
import kz.mybrain.superkassa.desktop.ui.analytics.addressesToFind
import kz.mybrain.superkassa.desktop.ui.analytics.fitting
import kz.mybrain.superkassa.desktop.ui.analytics.placement
import kz.mybrain.superkassa.desktop.ui.map.MapProjection
import java.math.BigDecimal
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
                kkm("c4", address = "Есть свои", position = KkmPosition(latitude = degrees(43.2), longitude = degrees(76.8)))
            )
        )
        assertEquals(
            listOf("г. Алматы, пр. Абая, 10", "г. Актобе, ул. Абилкайыр хана, 40"),
            addressesToFind(view)
        )
    }

    @Test
    fun `пустой ответ кабинета не даёт ни одной точки и ни одной строки`() {
        val nothing = placement(null, AddressLookup { AddressAnswer.Missing })
        assertTrue(nothing.placed.isEmpty() && nothing.unplaced.isEmpty())
    }

    private fun placed(id: String, latitude: Double, longitude: Double) =
        kz.mybrain.superkassa.desktop.ui.analytics.PlacedKkm(kkm(id), latitude, longitude)

    private fun kkm(id: String, address: String? = null, position: KkmPosition? = null) =
        AnalyticsKkm(cashRegisterId = id, kkmId = 2000302, address = address, position = position)

    private fun degrees(value: Double): BigDecimal = BigDecimal.valueOf(value)

    private companion object {
        const val TOLERANCE = 0.001
        const val DEGREE_TOLERANCE = 0.000001
        const val STEP = 0.05
    }
}
