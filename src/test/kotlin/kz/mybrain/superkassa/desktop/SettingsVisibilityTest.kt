package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.settings.Setting
import kz.mybrain.superkassa.desktop.ui.settings.visibleSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Что видно в настройках и откуда.
 *
 * Экран настроек один на всё приложение, но открывается из двух мест:
 * с экрана входа, где кассы ещё нет, и из кассы, куда кассир вошёл.
 * Прежде это были два экрана со своими списками карточек, и они
 * разошлись: отладка стояла только за входом — там, где она уже
 * не нужна, потому что войти получилось.
 */
class SettingsVisibilityTest {

    @Test
    fun `до входа видно то, что задают раньше входа`() {
        val shown = visibleSettings(hasRegister = false, admin = false)

        assertEquals(
            listOf(
                Setting.Appearance,
                Setting.PanelBehaviour,
                Setting.NodeAddress,
                Setting.CabinetAddress,
                Setting.MapServices,
                Setting.Debug,
                Setting.NodeFacts
            ),
            shown
        )
    }

    /**
     * Отладка нужна ровно тогда, когда войти нельзя: узел не отвечает,
     * список касс пуст. Это единственная настройка, у которой условий
     * показа нет вовсе.
     */
    @Test
    fun `отладка видна всегда`() {
        assertTrue(Setting.Debug in visibleSettings(hasRegister = false, admin = false))
        assertTrue(Setting.Debug in visibleSettings(hasRegister = true, admin = false))
        assertTrue(Setting.Debug in visibleSettings(hasRegister = true, admin = true))
    }

    @Test
    fun `настройки кассы без выбранной кассы не показываются`() {
        val shown = visibleSettings(hasRegister = false, admin = true)

        listOf(
            Setting.CurrentKkm, Setting.PrintForm, Setting.PrintTarget,
            Setting.Tax, Setting.OfdSync, Setting.OfdToken,
            Setting.Diagnostics, Setting.Decommission
        ).forEach { assertTrue(it !in shown, "$it показана без кассы") }
    }

    /**
     * Снятие с учёта и выдача токена — то, на что узел отвечает только
     * администратору. Кассиру их не показываем: нажатие вернуло бы отказ
     * по правам, и это худший способ узнать, что тебе нельзя.
     */
    @Test
    fun `служебное кассиру не показывается`() {
        val shown = visibleSettings(hasRegister = true, admin = false)

        listOf(Setting.Decommission, Setting.OfdToken, Setting.Tax, Setting.OfdSync)
            .forEach { assertTrue(it !in shown, "$it показана кассиру") }
        assertTrue(Setting.Diagnostics in shown)
        assertTrue(Setting.PrintTarget in shown)
    }

    @Test
    fun `администратору в кассе видно всё`() {
        assertEquals(Setting.entries, visibleSettings(hasRegister = true, admin = true))
    }
}
