package kz.mybrain.superkassa.presentation.analytics.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.analytics.model.AddressAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.PlacedKkm
import kz.mybrain.superkassa.domain.analytics.port.FakeAnalytics
import kz.mybrain.superkassa.domain.map.QuietMaps
import kz.mybrain.superkassa.domain.map.model.MapPlace
import kz.mybrain.superkassa.domain.map.port.Maps
import kz.mybrain.superkassa.presentation.analytics.mapModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Карта касс без окна: как модель ведёт карту, пока служба карт находит дома.
 *
 * При адресе торговой точки дома находятся по одному, и набор растёт
 * от одного двора до сети по всей стране. Карта ведётся к набору, пока
 * владелец не распорядился ею сам.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsMapViewModelTest {
    private val analytics = FakeAnalytics().apply {
        kkms = { AnalyticsAnswer.Done(KkmMapView(placed = listOf(kkm("c1", ALMATY), kkm("c2", URALSK)))) }
    }

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `карта отъезжает, пока кассы прибывают`() {
        var model: AnalyticsMapViewModel? = null
        var house = 0
        val maps = PacedMaps { address -> if (address == URALSK) house = model!!.state.value.map.zoom }
        model = mapModel(analytics, maps)

        model.follow(OWNER)

        assertTrue(house > 0, "первый найденный дом не навёл карту")
        assertTrue(model.state.value.map.zoom < house, "карта осталась на увеличении ${model.state.value.map.zoom}")
    }

    /** Наведение прекращает рука владельца: карта из-под неё не уезжает. */
    @Test
    fun `сдвинутую владельцем карту прибывшие кассы не уводят`() {
        var model: AnalyticsMapViewModel? = null
        var own = 0 to 0.0
        val maps = PacedMaps { address ->
            if (address == URALSK) {
                val map = model!!.state.value.map
                map.zoomBy(-3)
                own = map.zoom to map.centerLongitude
            }
        }
        model = mapModel(analytics, maps)

        model.follow(OWNER)

        val map = model.state.value.map
        assertEquals(own.first, map.zoom, "карта уехала из-под руки владельца")
        assertEquals(own.second, map.centerLongitude)
    }

    /** То же и после выбора кассы: к ней владелец и шёл. */
    @Test
    fun `выбранную кассу прибывшие соседи не отменяют`() {
        var model: AnalyticsMapViewModel? = null
        val maps = PacedMaps { address ->
            val chosen = PlacedKkm(kkm("c1", ALMATY), ALMATY_LATITUDE, ALMATY_LONGITUDE)
            if (address == URALSK) model!!.show(chosen, emptyList())
        }
        model = mapModel(analytics, maps)

        model.follow(OWNER)

        assertEquals("c1", model.state.value.chosen)
        assertEquals(ALMATY_LONGITUDE, model.state.value.map.goal?.longitude)
    }

    /**
     * Молчание службы карт — не «не нашли» навсегда.
     *
     * Касса без ответа службы встаёт в «не нашли», но при обновлении её дом
     * спрашивается снова; найденный — нет.
     */
    @Test
    fun `не ответившая служба не хоронит адрес`() {
        val almaty = MapPlace(ALMATY_LATITUDE, ALMATY_LONGITUDE, "")
        val maps = QuietMaps(found = mapOf(ALMATY to almaty), silent = setOf(URALSK))
        val model = mapModel(analytics, maps)

        model.follow(OWNER)
        assertEquals(AddressAnswer.Missing, model.state.value.found[URALSK])
        model.refresh()

        assertEquals(listOf(ALMATY, URALSK, URALSK), maps.searched, "ненайденный адрес не спрошен снова")
    }

    @Test
    fun `новый ответ кабинета снимает выбор и отказ говорится на экране`() {
        val model = mapModel(analytics, QuietMaps())
        model.follow(OWNER)
        model.pick("c1")

        analytics.kkms = { AnalyticsAnswer.Troubled(REFUSED) }
        model.refresh()

        val state = model.state.value
        assertNull(state.chosen)
        assertEquals(REFUSED, state.reading.trouble)
        assertNull(state.reading.value)
    }

    @Test
    fun `без входа в кабинет карта ничего не спрашивает`() {
        val model = mapModel(analytics, QuietMaps())

        model.follow(null)
        model.refresh()

        assertEquals(emptyList(), analytics.asked)
    }

    private fun kkm(id: String, address: String) = AnalyticsKkm(cashRegisterId = id, address = address)

    /** Служба карт, находящая оба дома; перед ответом о каждом зовёт проверку. */
    private class PacedMaps(private val before: (String) -> Unit) : Maps by QuietMaps() {
        override suspend fun find(address: String): List<MapPlace> {
            before(address)
            val almaty = address == ALMATY
            return listOf(
                MapPlace(
                    if (almaty) ALMATY_LATITUDE else URALSK_LATITUDE,
                    if (almaty) ALMATY_LONGITUDE else URALSK_LONGITUDE,
                    ""
                )
            )
        }
    }

    private companion object {
        const val OWNER = "owner-access"
        const val ALMATY = "Алматы, Медеуский, Достык, 10"
        const val URALSK = "Западно-Казахстанская, Уральск, Сарайшык, 5"
        const val ALMATY_LATITUDE = 43.238949
        const val ALMATY_LONGITUDE = 76.889709
        const val URALSK_LATITUDE = 51.227811
        const val URALSK_LONGITUDE = 51.386998
        val REFUSED = AnalyticsTrouble.Refused("Карта выдана не этой компании")
    }
}
