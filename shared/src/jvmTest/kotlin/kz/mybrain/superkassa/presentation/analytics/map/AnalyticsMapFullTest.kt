package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.data.map.WorkplaceMapMemory
import kz.mybrain.superkassa.domain.analytics.model.Placement
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.map.QuietMaps
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kz.mybrain.superkassa.presentation.analytics.map.component.AnalyticsPinCard
import kz.mybrain.superkassa.presentation.common.keyboard.EscapeCloses
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts
import kz.mybrain.superkassa.shot
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

    /** Карточка под картой так, как её заводит раздел, — на памяти рабочего места. */
    private fun cardOf(preferences: Preferences) = MapPorts(QuietMaps(), WorkplaceMapMemory(preferences)).cases().card()

    @Test
    fun `свёрнутая карточка помнится рабочим местом`() {
        val preferences = preferences()
        val card = cardOf(preferences)
        assertTrue(card.expanded, "у нового рабочего места карточка развёрнута")
        card.toggle()
        assertFalse(card.expanded)
        val again = cardOf(preferences)
        assertFalse(again.expanded, "новое открытие раздела не развернуло карточку")
        card.toggle()
        assertTrue(cardOf(preferences).expanded)
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
            texts = AnalyticsLook.texts,
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
        val kkms = (1..3).map { AnalyticsLook.kkm(it, address = HERE) }
        val here = AnalyticsLook.LATITUDE to AnalyticsLook.LONGITUDE
        val laid: Placement = laidOut(AnalyticsLook.view(kkms), mapOf(HERE to here))
        var model by mutableStateOf(AnalyticsLook.model())
        AnalyticsLook.centre(model, laid.placed)
        val groups = groupsOf(laid, model.map.zoom)
        val tools = AnalyticsLook.tools()
        var closed = 0
        RenderProbe(WIDTH, HEIGHT) {
            val laidOut = MapLaid(laid, laid.placed.size, groups)
            val parts = MapParts(model, object : AnalyticsMapActions {}, tools, laidOut, AnalyticsLook.words)
            AnalyticsMapFullscreen(parts) { closed += 1 }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-map-fullscreen", probe.frame())
            // Касса выбрана в списке, а карточка свёрнута: в заголовке видно, какая.
            model = model.copy(chosen = laid.placed[1].kkm.cashRegisterId, spot = groups.first().id)
            tools.panel.toggle()
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-map-fullscreen-chosen", probe.frame())
            // Escape слушает само окно, а не наложение: в сцене без окна
            // нажатие приходит тем же путём — через учёт открытых наложений.
            assertTrue(EscapeCloses.press(), "карта не записалась как открытое наложение")
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
