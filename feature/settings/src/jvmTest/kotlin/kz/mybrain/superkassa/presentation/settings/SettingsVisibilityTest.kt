package kz.mybrain.superkassa.presentation.settings

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
        assertEquals(
            listOf(
                Setting.Appearance,
                Setting.Language,
                Setting.PanelBehaviour,
                Setting.Facts,
                Setting.CabinetAddress,
                Setting.MapServices,
                Setting.Updates,
                Setting.Debug
            ),
            visibleSettings(hasRegister = false, admin = false)
        )
    }

    /** До входа — только разделы машины и программы: полки кассы нет вовсе. */
    @Test
    fun `до входа разделы кассы не показываются`() {
        assertEquals(
            listOf(
                SettingsSection.Look,
                SettingsSection.SalePanels,
                SettingsSection.Machine,
                SettingsSection.Addresses,
                SettingsSection.Updates,
                SettingsSection.Debug
            ),
            visibleSections(hasRegister = false, admin = false)
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

    /**
     * Сведения о кассе — версии, каталог данных — нужны поддержке до входа:
     * когда касса не открылась или не пускает.
     */
    @Test
    fun `сведения о кассе видны до входа и кассиру`() {
        assertTrue(Setting.Facts in visibleSettings(hasRegister = false, admin = false))
        assertTrue(Setting.Facts in visibleSettings(hasRegister = true, admin = false))
        assertTrue(Setting.Core !in visibleSettings(hasRegister = true, admin = false), "сроки БФД показаны кассиру")
    }

    /** На Android приложение обновляет магазин: раздела выпусков там нет. */
    @Test
    fun `без своих выпусков раздела обновлений нет`() {
        assertTrue(SettingsSection.Updates !in visibleSections(hasRegister = true, admin = true, hasReleases = false))
        assertTrue(Setting.Facts in visibleSettings(hasRegister = true, admin = true, hasReleases = false))
    }

    /** Без кабинета — на Android — адреса кабинета и служб карты настраивать незачем. */
    @Test
    fun `без кабинета раздела адресов служб нет`() {
        val shown = visibleSections(hasRegister = true, admin = true, hasCabinet = false)

        assertTrue(SettingsSection.Addresses !in shown, "адреса служб показаны без кабинета")
        assertTrue(SettingsSection.Machine in shown, "без кабинета пропали настройки кассы на машине")
    }

    @Test
    fun `настройки кассы без выбранной кассы не показываются`() {
        val shown = visibleSettings(hasRegister = false, admin = true)

        kkmSettings.forEach { assertTrue(it !in shown, "$it показана без кассы") }
    }

    /**
     * То, на что узел отвечает только администратору, кассиру не видно.
     *
     * Список перечислен целиком, а не выборкой: выборка молчала о том,
     * чего в ней нет, и режим программирования с печатной формой стояли
     * у кассира живой кнопкой и мёртвой карточкой.
     */
    @Test
    fun `служебное кассиру не показывается`() {
        assertEquals(
            listOf(
                Setting.CurrentKkm,
                Setting.PrintTarget,
                Setting.Diagnostics,
                Setting.Appearance,
                Setting.Language,
                Setting.PanelBehaviour,
                Setting.Facts,
                Setting.CabinetAddress,
                Setting.MapServices,
                Setting.Updates,
                Setting.Debug
            ),
            visibleSettings(hasRegister = true, admin = false)
        )
    }

    @Test
    fun `администратору в кассе видно всё`() {
        assertEquals(Setting.entries, visibleSettings(hasRegister = true, admin = true))
        assertEquals(SettingsSection.entries, visibleSections(hasRegister = true, admin = true))
    }

    /**
     * Каталог идёт в порядке разделов: иначе настройки одного раздела
     * встали бы вразбивку, а раздел в списке — не на своём месте.
     */
    @Test
    fun `настройки идут по разделам списка`() {
        val order = settingsCards.map { it.setting.section.ordinal }

        assertEquals(order.sorted(), order, "настройки перемешаны между разделами")
        assertEquals(Setting.entries, settingsCards.map { it.setting }, "каталог разошёлся с порядком настроек")
    }

    /**
     * Снятие с учёта — последним в разделе кассы, режим программирования —
     * сразу под самой кассой: вход и выход в одном месте, а необратимое
     * не нажимают по дороге к остальному.
     */
    @Test
    fun `в разделе кассы режим рядом с кассой, снятие последним`() {
        val kkm = visibleSettings(hasRegister = true, admin = true).filter { it.section == SettingsSection.Kkm }

        assertEquals(listOf(Setting.CurrentKkm, Setting.Programming, Setting.Decommission), kkm)
    }

    /** Полки идут от кассы к программе, и раздел не оторван от своей полки. */
    @Test
    fun `разделы стоят по полкам`() {
        val shelves = SettingsSection.entries.map { it.shelf.ordinal }

        assertEquals(shelves.sorted(), shelves, "раздел стоит не на своей полке")
    }

    /** Настройки, которые принимает узел этой кассы. */
    private val kkmSettings = listOf(
        Setting.CurrentKkm, Setting.Programming, Setting.PrintForm, Setting.PrintTarget,
        Setting.Tax, Setting.Domain, Setting.OfdSync, Setting.OfdToken,
        Setting.Diagnostics, Setting.Decommission
    )
}
