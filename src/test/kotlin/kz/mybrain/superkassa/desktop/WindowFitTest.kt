package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.fitToScreen
import kz.mybrain.superkassa.desktop.app.windowMinimum
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Окно кассы открывается на том экране, который есть сейчас.
 *
 * Размер помнится с прошлого запуска, а машина не всегда та же: рабочее
 * место разворачивают на внешнем мониторе, а утром касса открывается
 * на ноутбуке. Окно шире экрана уезжало краем за него вместе с шапкой,
 * и тянуть его обратно приходилось мышью.
 */
class WindowFitTest {

    @Test
    fun `окно с внешнего монитора ужимается до ноутбучного экрана`() {
        assertEquals(1366 to 768, fitToScreen(2560 to 1440, 1366 to 768))
    }

    @Test
    fun `окно меньше экрана остаётся таким, каким его оставили`() {
        assertEquals(1000 to 700, fitToScreen(1000 to 700, 1920 to 1080))
    }

    @Test
    fun `ужимается только та сторона, которая не помещается`() {
        assertEquals(1366 to 700, fitToScreen(2560 to 700, 1366 to 768))
    }

    /** Экрана может не оказаться вовсе: касса собирается и проверяется без него. */
    @Test
    fun `без экрана размер остаётся прежним`() {
        assertEquals(2560 to 1440, fitToScreen(2560 to 1440, null))
    }

    /**
     * Нижняя граница размера меряется точками, а не пикселями экрана.
     *
     * Пересчёт в пиксели удваивал её на экране с удвоенной плотностью:
     * минимум в 960×640 превращался в 1920×1280, окно раздувалось до него
     * и уводило нижнюю полосу с кнопкой «Войти» под док.
     */
    @Test
    fun `нижняя граница размера окна задана в точках`() {
        val minimum = windowMinimum(Sizes.windowMinWidth, Sizes.windowMinHeight)
        assertEquals(Sizes.windowMinWidth.value.toInt(), minimum.width)
        assertEquals(Sizes.windowMinHeight.value.toInt(), minimum.height)
    }

    /** Минимум обязан помещаться туда же, куда помещается само окно. */
    @Test
    fun `нижняя граница размера помещается на ноутбучном экране`() {
        val minimum = windowMinimum(Sizes.windowMinWidth, Sizes.windowMinHeight)
        val laptop = 1366 to 768
        assertEquals(minimum.width to minimum.height, fitToScreen(minimum.width to minimum.height, laptop))
    }
}
