package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsMapModel
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsPinCard
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsSieveBar
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsSpotCard
import kz.mybrain.superkassa.desktop.ui.analytics.PlacedKkm
import kz.mybrain.superkassa.desktop.ui.analytics.kkmGroups
import kz.mybrain.superkassa.desktop.ui.analytics.kkmMarks
import kz.mybrain.superkassa.desktop.ui.analytics.sievePlaces
import kz.mybrain.superkassa.desktop.ui.map.MapGeocoder
import kz.mybrain.superkassa.desktop.ui.map.MapMarks
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки карты касс — чтобы посмотреть на неё глазами.
 *
 * Разметку карты проверяют проверки разметки, а вид — только человек:
 * ярлычок может встать поверх соседа, надпись — не поместиться,
 * а ряд отбора — разъехаться по высоте. Снимки кладутся в `/tmp`
 * и на сборку не влияют.
 */
class MapLookShots {

    private val texts = analyticsTexts(Language.Ru)
    private val cabinetWords = cabinetTexts(Language.Ru)

    private fun kkm(at: Int, place: String = "Магазин на Абая") = AnalyticsKkm(
        cashRegisterId = "c$at",
        kkmId = 2000300 + at,
        registrationNumber = "%012d".format(4500000 + at),
        internalName = "Касса $at",
        retailPlaceId = "p-1",
        retailPlaceName = place,
        address = "г. Алматы, пр. Абая, 10",
        status = "REGISTERED",
        shiftStatus = if (at == 1) "OPEN" else "CLOSED",
        shiftNumber = at.toLong(),
        lastContactAt = "2026-09-20T19:47:00Z",
        blocked = at == 3
    )

    private fun shot(name: String, bytes: ByteArray) {
        val file = File("/tmp/$name.png")
        file.writeBytes(bytes)
        assertTrue(file.length() > 0, "снимок $name пуст")
    }

    @Test
    fun `ярлычки мест на карте`() {
        val groups = kkmGroups(
            listOf(
                PlacedKkm(kkm(1), LATITUDE, LONGITUDE),
                PlacedKkm(kkm(2), LATITUDE, LONGITUDE),
                PlacedKkm(kkm(3), LATITUDE, LONGITUDE),
                PlacedKkm(kkm(4), LATITUDE + APART, LONGITUDE + APART),
                PlacedKkm(kkm(5), LATITUDE - APART, LONGITUDE + APART)
            ),
            ZOOM
        )
        val model = model()
        model.spot = groups.first().id
        RenderProbe(WIDTH, HEIGHT) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                MapMarks(model.map, IntSize(WIDTH, HEIGHT), kkmMarks(groups, model)) {}
            }
        }.use { shot("map-marks", it.frame()) }
    }

    @Test
    fun `отбор над картой и карточки под ней`() {
        val model = model()
        val groups = kkmGroups((1..3).map { PlacedKkm(kkm(it), LATITUDE, LONGITUDE) }, ZOOM)
        val view = KkmMapView(placed = (1..3).map { kkm(it) })
        RenderProbe(WIDTH, HEIGHT) {
            Column(Modifier.fillMaxSize().padding(Spacing.normal)) {
                AnalyticsSieveBar(model, sievePlaces(view), texts)
                AnalyticsSpotCard(groups.single(), texts, cabinetWords, onChoose = {})
                AnalyticsPinCard(
                    kkm = kkm(1),
                    source = PositionSource.RetailPlaceAddress,
                    texts = texts,
                    cabinet = cabinetWords,
                    neighbours = 3
                )
            }
        }.use { shot("map-under", it.frame()) }
    }

    private fun model() = AnalyticsMapModel(CabinetSession(), MapGeocoder())

    private companion object {
        const val LATITUDE = 43.238949
        const val LONGITUDE = 76.889709
        const val ZOOM = 12
        const val APART = 0.01
        const val WIDTH = 900
        const val HEIGHT = 700
    }
}
