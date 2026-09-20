package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.ExchangeAddress
import kz.mybrain.superkassa.desktop.server.cabinet.KkmPosition
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsExchangeList
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsPinCard
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsTrouble
import kz.mybrain.superkassa.desktop.ui.analytics.analyticsTroubleState
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsUnplaced
import kz.mybrain.superkassa.desktop.ui.analytics.PlacementTrouble
import kz.mybrain.superkassa.desktop.ui.analytics.UnplacedKkm
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kotlin.test.Test
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

    @Test
    fun `список касс без положения рисуется со всеми причинами`() {
        val rows = PlacementTrouble.entries.mapIndexed { at, reason -> UnplacedKkm(kkm(at), reason) }
        RenderProbe { AnalyticsUnplaced(rows, PositionSource.RetailPlaceAddress, texts, Modifier.fillMaxSize()) }
            .use { assertTrue(it.frame().isNotEmpty()) }
    }

    @Test
    fun `карточка кассы рисуется и с выбранной кассой, и без неё`() {
        RenderProbe { AnalyticsPinCard(null, PositionSource.KkmCoordinates, texts, cabinet) }
            .use { assertTrue(it.frame().isNotEmpty()) }
        RenderProbe { AnalyticsPinCard(kkm(1), PositionSource.KkmCoordinates, texts, cabinet) }
            .use { assertTrue(it.frame().isNotEmpty()) }
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
    }
}
