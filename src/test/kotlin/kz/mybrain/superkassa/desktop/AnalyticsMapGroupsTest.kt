package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.ui.analytics.KkmMark
import kz.mybrain.superkassa.desktop.ui.analytics.MapSieve
import kz.mybrain.superkassa.desktop.ui.analytics.Placement
import kz.mybrain.superkassa.desktop.ui.analytics.PlacedKkm
import kz.mybrain.superkassa.desktop.ui.analytics.kkmGroups
import kz.mybrain.superkassa.desktop.ui.analytics.sievePlaces
import kz.mybrain.superkassa.desktop.ui.analytics.sieved
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Кассы одного места — одним ярлычком, и отбор над ними.
 *
 * Проверяется то, что на экране видно только глазами: три кассы торговой
 * точки должны сойтись в один ярлычок с числом, на увеличении квартала
 * разойтись по своим домам, а отбор — сужать карту и список одинаково.
 */
class AnalyticsMapGroupsTest {

    private val latitude = 43.238949
    private val longitude = 76.889709

    @Test
    fun `кассы одного дома сводятся в один ярлычок`() {
        val groups = kkmGroups(
            listOf(
                placed("c1", latitude, longitude),
                placed("c2", latitude, longitude),
                placed("c3", latitude + STEP, longitude + STEP)
            ),
            CITY_ZOOM
        )

        assertEquals(1, groups.size, "на увеличении города набор должен быть одним ярлычком")
        assertEquals(3, groups.single().size)
    }

    /** На квартале дома расходятся, и ярлычок распадается на свои кассы. */
    @Test
    fun `с приближением ярлычок распадается`() {
        val apart = listOf(placed("c1", latitude, longitude), placed("c2", latitude + STEP, longitude + STEP))

        assertEquals(1, kkmGroups(apart, CITY_ZOOM).size)
        assertEquals(2, kkmGroups(apart, HOUSE_ZOOM).size)
    }

    /** Ярлычок стоит серединой места, а не на первой из своих касс. */
    @Test
    fun `ярлычок встаёт в середину места`() {
        val group = kkmGroups(
            listOf(placed("c1", latitude, longitude), placed("c2", latitude + SMALL, longitude)),
            CITY_ZOOM
        ).single()

        assertTrue(group.latitude > latitude, "ярлычок остался на первой кассе: ${group.latitude}")
        assertTrue(group.latitude < latitude + SMALL)
    }

    /** Ярлычок помнит свои кассы: по этому выбранная в списке подсвечивает своё место. */
    @Test
    fun `ярлычок узнаёт свою кассу`() {
        val group = kkmGroups(listOf(placed("c1", latitude, longitude)), CITY_ZOOM).single()

        assertTrue(group.holds("c1"))
        assertTrue(!group.holds("c2"))
    }

    @Test
    fun `отбор по признаку сужает и карту, и список рядом`() {
        val placement = Placement(
            placed = listOf(placed("c1", latitude, longitude, blocked = true), placed("c2", latitude, longitude)),
            unplaced = emptyList()
        )

        val left = sieved(placement, MapSieve(marks = setOf(KkmMark.Blocked)))

        assertEquals(listOf("c1"), left.placed.map { it.kkm.cashRegisterId })
    }

    /** Поиск идёт по тому, чем владелец помнит кассу, а не только по номеру КГД. */
    @Test
    fun `поиск находит кассу по своему имени и по адресу`() {
        val placement = Placement(
            placed = listOf(
                placed("c1", latitude, longitude, name = "Второй зал"),
                placed("c2", latitude, longitude, address = "пр. Абая, 10")
            ),
            unplaced = emptyList()
        )

        assertEquals(listOf("c1"), sieved(placement, MapSieve(needle = "второй")).placed.map { it.kkm.cashRegisterId })
        assertEquals(listOf("c2"), sieved(placement, MapSieve(needle = "Абая")).placed.map { it.kkm.cashRegisterId })
    }

    /** Точки для плашки отбора берутся из обеих половин ответа и без повторов. */
    @Test
    fun `торговые точки отбора собираются без повторов`() {
        val view = KkmMapView(
            placed = listOf(kkm("c1", place = "p-1"), kkm("c2", place = "p-1")),
            withoutPosition = listOf(kkm("c3", place = "p-2"))
        )

        assertEquals(listOf("p-1", "p-2"), sievePlaces(view).map { it.id })
    }

    private fun placed(
        id: String,
        latitude: Double,
        longitude: Double,
        blocked: Boolean = false,
        name: String? = null,
        address: String? = null
    ) = PlacedKkm(
        kkm = kkm(id, blocked = blocked, name = name, address = address),
        latitude = latitude,
        longitude = longitude
    )

    private fun kkm(
        id: String,
        blocked: Boolean = false,
        name: String? = null,
        address: String? = null,
        place: String? = null
    ) = AnalyticsKkm(
        cashRegisterId = id,
        kkmId = 2000302,
        internalName = name,
        address = address,
        retailPlaceId = place,
        retailPlaceName = place,
        blocked = blocked
    )

    private companion object {
        /** Увеличение, на котором виден город целиком. */
        const val CITY_ZOOM = 12

        /** Увеличение, на котором виден дом. */
        const val HOUSE_ZOOM = 18

        /** Соседние дома на карте города. */
        const val STEP = 0.001

        /** Две кассы в одном зале. */
        const val SMALL = 0.00002
    }
}
