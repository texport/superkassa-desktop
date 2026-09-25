package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.material3.Text
import kz.mybrain.superkassa.RenderProbe
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Список касс над карточкой — и там, где полосы прокрутки нет.
 *
 * На Android и iOS полосы прокрутки у колонки нет, и раскладка получала
 * две части вместо трёх: аналитика падала на планшете сразу после входа
 * в кабинет. Проверяется на узком и широком окне, без полосы и с ней.
 */
class ListOverCardTest {

    @Test
    fun `без полосы прокрутки список и карточка стоят на узком и широком окне`() {
        listOf(NARROW, WIDE).forEach { width ->
            RenderProbe(width = width, height = HEIGHT) {
                ListOverCard(list = { Text(LIST) }, card = { Text(CARD) }, scrollbar = {})
            }.use { probe ->
                probe.frame()
                val texts = probe.nodes().map { it.text }
                assertTrue(LIST in texts && CARD in texts, "на ширине $width: $texts")
            }
        }
    }

    @Test
    fun `с полосой прокрутки раскладка та же`() {
        RenderProbe(width = WIDE, height = HEIGHT) { ListOverCard(list = { Text(LIST) }, card = { Text(CARD) }) }
            .use { probe ->
                probe.frame()
                assertTrue(probe.nodes().any { it.text == CARD })
            }
    }

    private companion object {
        const val NARROW = 400
        const val WIDE = 1280
        const val HEIGHT = 800
        const val LIST = "Кассы сети"
        const val CARD = "Касса №1"
    }
}
