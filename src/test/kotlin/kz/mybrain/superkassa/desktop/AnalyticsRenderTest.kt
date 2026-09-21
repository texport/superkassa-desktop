package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.ExchangeAddress
import kz.mybrain.superkassa.desktop.server.cabinet.KkmPosition
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsExchangeList
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsKkmList
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsPinCard
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsSpotCard
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsTrouble
import kz.mybrain.superkassa.desktop.ui.analytics.PlacedKkm
import kz.mybrain.superkassa.desktop.ui.analytics.PlacementTrouble
import kz.mybrain.superkassa.desktop.ui.analytics.UnplacedKkm
import kz.mybrain.superkassa.desktop.ui.analytics.analyticsTroubleState
import kz.mybrain.superkassa.desktop.ui.analytics.kkmGroups
import kz.mybrain.superkassa.desktop.ui.map.MapMark
import kz.mybrain.superkassa.desktop.ui.map.MapMarks
import kz.mybrain.superkassa.desktop.ui.map.MapState
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Части раздела аналитики рисуются на сцене без окна.
 *
 * Кабинет здесь не спрашивается вовсе: до выкладки его ручек нет,
 * а проверять нужно другое — что разметка собирается, а не падает,
 * и что сотня строк не собирается столбцом целиком.
 */
class AnalyticsRenderTest {

    private val texts = analyticsTexts(Language.Ru)
    private val cabinet = cabinetTexts(Language.Ru)

    private fun kkm(at: Int) = AnalyticsKkm(
        cashRegisterId = "c$at",
        kkmId = 2000000 + at,
        registrationNumber = "%012d".format(at),
        internalName = "Касса $at",
        retailPlaceName = "Магазин $at",
        address = "г. Алматы, пр. Абая, $at",
        status = "REGISTERED",
        shiftStatus = "OPEN",
        shiftNumber = at.toLong(),
        lastContactAt = "2026-09-19T08:14:00Z",
        position = KkmPosition(geoSource = "GNSS")
    )

    private fun address(at: Int) = ExchangeAddress(
        cashRegisterId = "c$at",
        kkmId = 2000000 + at,
        registrationNumber = "%012d".format(at),
        internalName = "Касса $at",
        retailPlaceName = "Магазин $at",
        address = "212.154.10.$at",
        firstSeen = "2026-09-01T06:00:00Z",
        lastSeen = "2026-09-19T08:14:00Z"
    )

    /**
     * Список рядом с картой показывает все кассы: поставленные строками
     * с переходом, непоставленные — с причиной, по которой перехода нет.
     */
    @Test
    fun `список всех касс рисуется и с переходами, и со всеми причинами`() {
        val unplaced = PlacementTrouble.entries.mapIndexed { at, reason -> UnplacedKkm(kkm(at), reason) }
        val placed = (10..11).map { at -> PlacedKkm(kkm(at), 43.2 + at, 76.9 + at) }
        RenderProbe {
            AnalyticsKkmList(
                placed = placed,
                unplaced = unplaced,
                chosen = placed.first().kkm.cashRegisterId,
                source = PositionSource.RetailPlaceAddress,
                texts = texts,
                onChoose = {},
                modifier = Modifier.fillMaxSize()
            )
        }.use { assertTrue(it.frame().isNotEmpty()) }
    }

    @Test
    fun `карточка кассы рисуется и с выбранной кассой, и без неё`() {
        RenderProbe { AnalyticsPinCard(null, PositionSource.KkmCoordinates, texts, cabinet) }
            .use { assertTrue(it.frame().isNotEmpty()) }
        RenderProbe { AnalyticsPinCard(kkm(1), PositionSource.KkmCoordinates, texts, cabinet) }
            .use { assertTrue(it.frame().isNotEmpty()) }
    }

    /**
     * Ярлычок с числом касс и нажатие по нему.
     *
     * Это и есть весь смысл сведения касс в место: до него три кассы
     * торговой точки садились булавками одна на другую, и добраться
     * можно было только до верхней.
     */
    @Test
    fun `нажатие по ярлычку выбирает его место`() {
        val state = MapState(LATITUDE, LONGITUDE, CITY_ZOOM)
        val canvas = IntSize(WIDTH, HEIGHT)
        val marks = listOf(
            MapMark("place", LATITUDE, LONGITUDE, "3", Color.Red, chosen = false),
            MapMark("away", LATITUDE + AWAY, LONGITUDE + AWAY, null, Color.Red, chosen = false)
        )
        var picked: String? = null
        RenderProbe(WIDTH, HEIGHT) {
            Box(Modifier.fillMaxSize()) { MapMarks(state, canvas, marks) { picked = it.id } }
        }.use {
            it.frame()
            it.click(Offset(WIDTH / 2f, HEIGHT / 2f))
        }
        assertEquals("place", picked)
    }

    /** Список касс места: из него заходят в кассу, и строки нажимаются. */
    @Test
    fun `список касс места рисуется`() {
        val group = kkmGroups((1..3).map { at -> PlacedKkm(kkm(at), LATITUDE, LONGITUDE) }, CITY_ZOOM).single()
        RenderProbe { AnalyticsSpotCard(group, texts, cabinet, onChoose = {}) }
            .use { assertTrue(it.frame().isNotEmpty()) }
    }

    /** У кассы места есть и возврат к соседям, и переход в её аналитику. */
    @Test
    fun `карточка кассы места показывает соседей и переход в аналитику`() {
        RenderProbe {
            AnalyticsPinCard(
                kkm = kkm(1),
                source = PositionSource.RetailPlaceAddress,
                texts = texts,
                cabinet = cabinet,
                neighbours = 3,
                onNeighbours = {},
                onSales = {}
            )
        }.use { assertTrue(it.frame().isNotEmpty()) }
    }

    @Test
    fun `сотня адресов обмена не собирается столбцом целиком`() {
        val rows = (1..ROWS).map(::address)
        RenderProbe { AnalyticsExchangeList(rows, texts, Modifier.fillMaxSize()) }
            .use { assertTrue(it.frame().isNotEmpty()) }
    }

    @Test
    fun `до выкладки кабинета раздел показывает объяснение, а не пустой экран`() {
        RenderProbe {
            val state = analyticsTroubleState(AnalyticsTrouble.NotDeployed, texts, onRetry = {})
            ScreenSlot(state, Modifier.fillMaxSize()) {}
        }.use { assertTrue(it.frame().isNotEmpty()) }
    }

    private companion object {
        const val ROWS = 100

        /** Середина Алматы: с неё начинается карта приложения. */
        const val LATITUDE = 43.238949
        const val LONGITUDE = 76.889709

        /** Увеличение, на котором виден город. */
        const val CITY_ZOOM = 12

        /** Настолько в стороне, что в один ярлычок с первым не сойдётся. */
        const val AWAY = 0.02

        const val WIDTH = 400
        const val HEIGHT = 300
    }
}
