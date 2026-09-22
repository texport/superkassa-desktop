package kz.mybrain.superkassa.desktop

import androidx.compose.ui.input.key.Key
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsMapCard
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsMapFullscreen
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsPinCard
import kz.mybrain.superkassa.desktop.ui.analytics.MapParts
import kz.mybrain.superkassa.desktop.ui.analytics.Placement
import kz.mybrain.superkassa.desktop.ui.map.MapServices
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Карта во всё окно и сворачиваемая карточка под ней.
 *
 * Проверяется то, что владелец делает руками: раскрывает карту, выходит
 * из неё Escape, сворачивает карточку и находит её свёрнутой при
 * следующем открытии раздела.
 */
class AnalyticsMapFullTest {

    private fun preferences(): Preferences =
        Preferences(File(Files.createTempDirectory("an").toFile(), "kkm"))

    @Test
    fun `свёрнутая карточка помнится рабочим местом`() {
        val preferences = preferences()
        val card = AnalyticsMapCard(preferences)
        assertTrue(card.expanded, "у нового рабочего места карточка развёрнута")
        card.toggle()
        assertFalse(card.expanded)
        assertFalse(AnalyticsMapCard(preferences).expanded, "новое открытие раздела не развернуло карточку")
        card.toggle()
        assertTrue(AnalyticsMapCard(preferences).expanded)
    }

    @Test
    fun `свёрнутая карточка без кассы оставляет заголовок и прячет подсказку`() {
        val expanded = shot(expanded = true)
        val collapsed = shot(expanded = false)
        assertFalse(expanded.contentEquals(collapsed), "свёрнутая карточка нарисована так же, как развёрнутая")
    }

    private fun shot(expanded: Boolean): ByteArray = RenderProbe {
        AnalyticsPinCard(
            kkm = null,
            source = PositionSource.KkmCoordinates,
            texts = Look.texts,
            cabinet = Look.cabinet,
            expanded = expanded,
            onToggle = {}
        )
    }.use { probe ->
        repeat(SETTLE) { probe.frame() }
        probe.frame()
    }

    @Test
    fun `карта во всё окно рисуется со списком касс и закрывается Escape`() {
        val session = Look.session()
        // Плитки — с заведомо недоступного источника: снимку сеть не нужна.
        session.preferences.maps.tiles = "file:///superkassa-no-tiles"
        val kkms = (1..3).map { Look.kkm(it, address = HERE) }
        val laid: Placement = laidOut(Look.view(kkms), mapOf(HERE to (Look.LATITUDE to Look.LONGITUDE)))
        val model = Look.model()
        model.centre(laid.placed)
        val groups = groupsOf(laid, model.map.zoom)
        val panel = Look.panel()
        var closed = 0
        RenderProbe(WIDTH, HEIGHT) {
            val parts = MapParts(
                session, model, MapServices(session.preferences), laid, groups,
                Look.texts, Look.cabinet, panel
            )
            AnalyticsMapFullscreen(parts) { closed += 1 }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-map-fullscreen", probe.frame())
            // Касса выбрана в списке, а карточка свёрнута: в заголовке видно, какая.
            model.show(laid.placed[1], groups)
            panel.toggle()
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-map-fullscreen-chosen", probe.frame())
            // Ни одного нажатия мышью до Escape: фокус окно берёт само.
            probe.key(Key.Escape)
        }
        assertEquals(1, closed, "Escape не закрыл карту во всё окно")
    }

    private companion object {
        const val HERE = "г. Алматы, пр. Абая, 10"
        const val WIDTH = 1372
        const val HEIGHT = 887
        const val SETTLE = 12
    }
}
