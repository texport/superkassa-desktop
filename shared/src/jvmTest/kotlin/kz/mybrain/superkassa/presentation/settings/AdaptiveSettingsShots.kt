package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.SettingsMeasure
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.designsystem.theme.size.ContentWidths
import kz.mybrain.superkassa.idleCabinet
import kz.mybrain.superkassa.presentation.shell.ProvideWindowModels
import kz.mybrain.superkassa.presentation.shell.WindowModels
import kz.mybrain.superkassa.presentation.shell.bar.KkmTopBar
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shell.rail.SectionRail
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.SectionContent
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
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
 * самой правой кнопки против ширины окна. Карточки занимают всю ширину
 * раздела и на широком окне встают рядом.
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
    private fun Window(desk: KassaDesk) {
        val shell by desk.parts.shell.state.collectAsState()
        Surface(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                KkmTopBar(shell, desk.look, onSignOut = {}, onRefresh = {})
                Row(modifier = Modifier.fillMaxSize()) {
                    SectionRail(Section.entries, Section.Settings, false, {}, { Text(VERSION) }) {}
                    // Адреса кабинета и карты — на сотню знаков: поле обязано
                    // показать их, не выталкивая кнопку за край карточки.
                    val app = remember { SettingsScene.app(desk) }
                    val parts = remember { WindowParts(desk.parts.shell, desk.look, idleCabinet(app, desk.look)) }
                    ProvideWindowModels(remember { WindowModels() }) {
                        SectionContent(app, parts, Section.Settings)
                    }
                }
            }
        }
    }

    /** Окно с кассой в режиме программирования, за которой вошёл администратор. */
    private fun desk(): KassaDesk = KassaScene.desk(SettingsMeasure.extremeKkm(), admin = true)

    /**
     * Кадр вкладки и замер; с [screens] больше одного — ещё и прокрутка
     * до конца столбца, кадр за кадром.
     */
    private fun shoot(case: Case, workplace: Boolean, screens: Int = 1) {
        val tab = if (workplace) "workplace" else "kkm"
        val appearance = if (case.dark) Appearance.Dark else Appearance.Light
        val probe = RenderProbe(case.width, case.height, appearance, Look(textScale = case.scale), case.language) {
            Window(desk())
        }
        probe.use {
            repeat(SETTLE) { probe.frame() }
            if (workplace) {
                val title = textsOf(case.language).common.settings.householdWorkplace
                SettingsMeasure.byText(probe.semantics(), title)?.let { probe.click(it.center) }
            }
            report("$tab-${case.name}", case, probe, workplace)
            repeat(screens) { at ->
                val suffix = if (screens == 1) "" else "-$at"
                File("/tmp/adaptive-settings-$tab-${case.name}$suffix.png").writeBytes(probe.frame())
                probe.wheel(at = Offset(case.width * WHEEL_AT, case.height / 2f), ticks = TICKS)
            }
        }
    }

    private fun report(name: String, case: Case, probe: RenderProbe, workplace: Boolean) {
        val nodes = probe.semantics()
        // Раздел начинается там, где стоит его заголовок: левее — рельс.
        val title = SettingsMeasure.lastByText(nodes, textsOf(case.language).common.settings.title) ?: return
        val rail = title.left
        val column = SettingsMeasure.extent(nodes, title)
        val fields = SettingsMeasure.fields(nodes)
        val controls = SettingsMeasure.controls(nodes).filter { it.left >= rail }
        val rightmost = (controls + fields).maxOfOrNull { it.right } ?: 0
        val outside = (controls + fields).count { it.right > case.width }
        // Раздел — всё, что оставил рельс, но не шире рабочего экрана.
        val room = minOf(case.width - rail, ContentWidths.workspace.value.toInt())
        // Две карточки одного раздела: на широком окне вторая стоит правее первой.
        val texts = textsOf(case.language).common.settings
        val pair = if (workplace) texts.appearance to texts.panelBehaviour else texts.printForm to texts.printer
        val lefts = listOf(pair.first, pair.second).map { SettingsMeasure.byText(nodes, it)?.left ?: 0 }
        val columns = if (lefts[1] > lefts[0]) 2 else 1
        println(
            "настройки $name: столбец $column из $room, столбцов карточек $columns; " +
                "поля ${SettingsMeasure.widths(fields)}; правый край $rightmost из ${case.width}, за краем $outside"
        )
        assertTrue(column >= room * FILLED, "$name: карточки заняли $column из $room — справа пустая полоса")
        assertEquals(0, outside, "$name: кнопка или поле за краем окна")
        if (case.width >= SIDE_BY_SIDE) assertTrue(columns > 1, "$name: на широком окне карточки стоят одним столбцом")
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

        /** Меньше этой доли раздела — значит, справа пустует полоса. */
        const val FILLED = 0.85f

        /** С этой ширины окна карточки стоят рядом: окно расширенное и шире за вычетом рельса. */
        const val SIDE_BY_SIDE = 1180

        /** Где крутить колесо: над столбцом настроек, правее рельса. */
        const val WHEEL_AT = 0.4f
    }
}
