package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.fitToScreen
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
}
