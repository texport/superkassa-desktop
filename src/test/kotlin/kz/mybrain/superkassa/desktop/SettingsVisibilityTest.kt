package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.settings.Setting
import kz.mybrain.superkassa.desktop.ui.settings.SettingsHousehold
import kz.mybrain.superkassa.desktop.ui.settings.settingsCards
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
                Setting.NodeFacts,
                Setting.Updates,
                Setting.Debug
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
            Setting.CurrentKkm, Setting.Programming, Setting.PrintForm, Setting.PrintTarget,
            Setting.Tax, Setting.OfdSync, Setting.OfdToken,
            Setting.Diagnostics, Setting.Decommission
        ).forEach { assertTrue(it !in shown, "$it показана без кассы") }
    }

    /**
     * То, на что узел отвечает только администратору, кассиру не видно.
     *
     * Список перечислен целиком, а не выборкой: выборка молчала о том,
     * чего в ней нет, и режим программирования с печатной формой стояли
     * у кассира живой кнопкой и мёртвой карточкой — узел отвечает по ним
     * только администратору, а войти в режим кассир не может вовсе.
     */
    @Test
    fun `служебное кассиру не показывается`() {
        assertEquals(
            listOf(
                Setting.Appearance,
                Setting.PanelBehaviour,
                Setting.NodeAddress,
                Setting.CabinetAddress,
                Setting.MapServices,
                Setting.NodeFacts,
                Setting.Updates,
                Setting.Debug,
                Setting.CurrentKkm,
                Setting.PrintTarget,
                Setting.Diagnostics
            ),
            visibleSettings(hasRegister = true, admin = false)
        )
    }

    @Test
    fun `администратору в кассе видно всё`() {
        assertEquals(Setting.entries, visibleSettings(hasRegister = true, admin = true))
    }

    /**
     * Порядок внутри хозяйства идёт от повседневного к необратимому.
     *
     * Прежде «Сведения об узле» и «Обновления» стояли после режима отладки,
     * а отладка — посреди настроек кассы: владелец, пришедший узнать версию,
     * первым делом натыкался на уровень записи журнала.
     */
    @Test
    fun `порядок идёт от повседневного к необратимому`() {
        val shown = visibleSettings(hasRegister = true, admin = true)

        assertEquals(
            Setting.Decommission,
            shown.last { it in kkmSettings },
            "необратимое обязано стоять последним среди настроек кассы"
        )
        assertTrue(
            shown.indexOf(Setting.Debug) > shown.indexOf(Setting.Updates),
            "отладка стоит раньше обновлений"
        )
        assertTrue(
            shown.indexOf(Setting.Debug) > shown.indexOf(Setting.NodeFacts),
            "отладка стоит раньше сведений об узле"
        )
        assertTrue(
            shown.indexOf(Setting.NodeAddress) < shown.indexOf(Setting.Diagnostics),
            "адреса служб перемешаны с настройками кассы"
        )
    }

    /**
     * Вход в режим программирования и выход из него — одна настройка.
     *
     * Прежде войти предлагала карточка печатной формы, а выйти — кнопка
     * в диагностике: кассир входил и искал выход по всему экрану.
     */
    @Test
    fun `режим программирования стоит рядом с самой кассой`() {
        val shown = visibleSettings(hasRegister = true, admin = true)

        assertEquals(
            Setting.CurrentKkm,
            shown[shown.indexOf(Setting.Programming) - 1],
            "режим программирования оторван от кассы, которой принадлежит"
        )
    }

    /**
     * Вкладка открывается разделом, а не карточкой в пустом окне.
     *
     * Хозяйство кабинета БФД держало один адрес: вкладка занимала треть
     * шапки, а под ней стояла карточка и поле высотой в экран. Адрес
     * кабинета хранится на этой же машине, как адрес узла и адреса карты,
     * и стоит теперь рядом с ними.
     */
    @Test
    fun `во вкладке не бывает одной карточки на пустом экране`() {
        SettingsHousehold.entries.forEach { household ->
            val cards = settingsCards.filter { it.group.household == household }
            assertTrue(cards.size > 1, "$household открывается одной карточкой в пустом окне")
        }
    }

    /** Настройки, которые принимает узел этой кассы. */
    private val kkmSettings = listOf(
        Setting.CurrentKkm, Setting.Programming, Setting.PrintForm, Setting.PrintTarget,
        Setting.Tax, Setting.OfdSync, Setting.OfdToken,
        Setting.Diagnostics, Setting.Decommission
    )
}
