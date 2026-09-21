package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.components.OfdChoice
import kz.mybrain.superkassa.desktop.ui.components.OfdTarget
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимок выбора контура БФД — чтобы посмотреть глазами.
 *
 * Поднят один контур, остальные погашены, и увидеть это можно только
 * в раскрытом списке: погашенная строка отличается от обычной цветом,
 * а цвет проверкой не поймать.
 */
class ContourLookShots {

    private fun contour(code: String, ru: String) =
        DictionaryEntry(code = code, name = mapOf("ru" to ru))

    @Test
    fun `список контуров с погашенными`() {
        val contours = listOf(
            contour("DEV", "Стенд разработки"),
            contour("TEST", "Тестовый стенд"),
            contour("PROD", "Продуктивный сервер")
        )
        RenderProbe(WIDTH, HEIGHT) {
            Box(Modifier.padding(Spacing.roomy)) {
                OfdChoice(OfdTarget(environment = "DEV"), contours, "ru") {}
            }
        }.use { probe ->
            probe.frame()
            probe.click(Offset(FIELD_X, FIELD_Y))
            val frame = probe.frame()
            File("/tmp/contours.png").writeBytes(frame)
            assertTrue(frame.isNotEmpty())
        }
    }

    private companion object {
        const val WIDTH = 520
        const val HEIGHT = 380

        /** Середина поля выбора: по нему список и раскрывается. */
        const val FIELD_X = 140f
        const val FIELD_Y = 50f
    }
}
