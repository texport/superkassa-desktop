package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.KkmBarActions
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.SectionContent
import kz.mybrain.superkassa.desktop.ui.SectionRail
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocuments
import kz.mybrain.superkassa.desktop.ui.components.AppTopBar
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.ContentWidths
import kz.mybrain.superkassa.desktop.ui.theme.Look
import kz.mybrain.superkassa.desktop.ui.theme.TextScale
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Настройки в окне кассы на предельных данных: кадры и замеры.
 *
 * Окна от наименьшего 960×640 до широкого 2560×1080 и планшеты, русский
 * и казахский, обычная и крупная ступень. Название кассы и организации
 * на сотню знаков, девять своих строк чека по сто знаков, длинные адреса
 * служб. Кадры — `/tmp/adaptive-settings-<вкладка>-<окно>-<язык>-<ступень>.png`,
 * замеры — строкой в выводе проверки: ширины карточек и полей, правый край
 * самой правой кнопки против ширины окна.
 */
class AdaptiveSettingsShots {

    /** Одно окно настроек: размер, язык, ступень и оформление. */
    data class Case(
        val width: Int,
        val height: Int,
        val language: Language = Language.Ru,
        val scale: TextScale = TextScale.Normal,
        val dark: Boolean = false
    ) {
        val name get() = "${width}x$height-${language.name.lowercase()}-${scale.name.lowercase()}" +
            if (dark) "-dark" else ""
    }

    @Composable
    private fun Window(session: Session) {
        val kkm = session.selected
        Surface(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppTopBar(
                    title = kkm?.let { session.displayName(it) }.orEmpty(),
                    subtitle = kkm?.orgTitle,
                    subtitleKept = session.whoami?.name
                ) { KkmBarActions(session, onSignOut = {}, onRefresh = {}) }
                Row(modifier = Modifier.fillMaxSize()) {
                    SectionRail(Section.entries, Section.Settings, false, {}, { Text(VERSION) }) {}
                    SectionContent(session, CabinetSession(), CabinetDocuments(), Section.Settings)
                }
            }
        }
    }

    private fun session(case: Case): Session {
        val session = KassaScene.session("adaptive-settings-${case.name}", kkm = SettingsMeasure.extremeKkm())
        session.switchLanguage(case.language)
        session.dictionaries[Dictionary.TaxRegimes] = listOf(DictionaryEntry("GENERAL", mapOf("ru" to "Общеустановленный")))
        session.dictionaries[Dictionary.VatGroups] = listOf(DictionaryEntry("VAT_12", mapOf("ru" to "НДС 12%")))
        session.preferences.nodeUrl = SettingsMeasure.LONG_URL
        session.preferences.cabinetUrl = SettingsMeasure.LONG_URL
        session.preferences.maps.tiles = SettingsMeasure.LONG_URL
        session.preferences.maps.search = SettingsMeasure.LONG_URL
        return session
    }

    /**
     * Кадр вкладки и замер; с [screens] больше одного — ещё и прокрутка
     * до конца столбца, кадр за кадром.
     */
    private fun shoot(case: Case, workplace: Boolean, screens: Int = 1) {
        val tab = if (workplace) "workplace" else "kkm"
        val appearance = if (case.dark) Appearance.Dark else Appearance.Light
        val probe = RenderProbe(case.width, case.height, appearance, Look(textScale = case.scale), case.language) {
            Window(session(case))
        }
        probe.use {
            repeat(SETTLE) { probe.frame() }
            if (workplace) {
                val title = stringsOf(case.language).settings.householdWorkplace
                SettingsMeasure.byText(probe.semantics(), title)?.let { probe.click(it.center) }
            }
            report("$tab-${case.name}", case, probe)
            repeat(screens) { at ->
                val suffix = if (screens == 1) "" else "-$at"
                File("/tmp/adaptive-settings-$tab-${case.name}$suffix.png").writeBytes(probe.frame())
                probe.wheel(at = Offset(case.width * WHEEL_AT, case.height / 2f), ticks = TICKS)
            }
        }
    }

    private fun report(name: String, case: Case, probe: RenderProbe) {
        val nodes = probe.semantics()
        // Раздел начинается там, где стоит его заголовок: левее — рельс.
        val title = SettingsMeasure.lastByText(nodes, stringsOf(case.language).settings.title) ?: return
        val rail = title.left
        val column = SettingsMeasure.extent(nodes, title)
        val fields = SettingsMeasure.fields(nodes)
        val controls = SettingsMeasure.controls(nodes).filter { it.left >= rail }
        val rightmost = (controls + fields).maxOfOrNull { it.right } ?: 0
        val outside = (controls + fields).count { it.right > case.width }
        // Карточка — столбец для чтения; всё, что правее его края, вылезло из карточки.
        val edge = rail + ContentWidths.reading.value.toInt()
        val beyond = controls.filter { it.right > edge && it.top > title.bottom + TAB_ROW }
            .joinToString { "${SettingsMeasure.label(nodes, it)}@${it.right}" }
        println(
            "настройки $name: столбец $column; поля ${SettingsMeasure.widths(fields)}; " +
                "правый край $rightmost из ${case.width}, за краем $outside; вне столбца [$beyond]"
        )
        assertTrue(column <= ContentWidths.reading.value, "$name: столбец настроек шире читаемого — $column")
        assertEquals(0, outside, "$name: кнопка или поле за краем окна")
        assertEquals("", beyond, "$name: кнопка вылезла из карточки")
    }

    @Test
    fun `настройки на всех окнах, языках и ступенях`() {
        SIZES.forEach { (width, height) ->
            LANGUAGES.forEach { language ->
                SCALES.forEach { scale ->
                    val case = Case(width, height, language, scale)
                    shoot(case, workplace = false)
                    shoot(case, workplace = true)
                }
            }
        }
    }

    @Test
    fun `настройки кассы сверху донизу`() {
        shoot(Case(960, 640, Language.Kk, TextScale.Larger), workplace = false, screens = ROLL)
        shoot(Case(2560, 1080), workplace = false, screens = ROLL_WIDE)
        shoot(Case(1180, 820, dark = true), workplace = false, screens = ROLL)
        shoot(Case(1180, 820, dark = true), workplace = true, screens = ROLL_WIDE)
    }

    private companion object {
        val SIZES = listOf(960 to 640, 1180 to 820, 1920 to 1080, 2560 to 1080, 800 to 1280, 1280 to 800)
        val LANGUAGES = listOf(Language.Ru, Language.Kk)
        val SCALES = listOf(TextScale.Normal, TextScale.Larger)
        const val VERSION = "1.0.6"
        const val SETTLE = 20
        const val ROLL = 10
        const val ROLL_WIDE = 4
        const val TICKS = 30f

        /** Высота строки вкладок под заголовком: вкладки меряются отдельно. */
        const val TAB_ROW = 64

        /** Где крутить колесо: над столбцом настроек, правее рельса. */
        const val WHEEL_AT = 0.4f
    }
}
