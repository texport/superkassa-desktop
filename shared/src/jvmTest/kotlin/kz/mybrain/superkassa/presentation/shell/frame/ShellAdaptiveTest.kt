package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.section.AppTopBar
import kz.mybrain.superkassa.designsystem.theme.size.ContentWidths
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.desk
import kz.mybrain.superkassa.domain.kkm.model.orgTitle
import kz.mybrain.superkassa.kassa.DashboardScene
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.common.model.ProvideWindowModels
import kz.mybrain.superkassa.presentation.common.model.WindowModels
import kz.mybrain.superkassa.presentation.shell.bar.KkmBarActions
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.SectionContent
import kz.mybrain.superkassa.strings.api.Language
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Оболочка окна на мониторах и планшетах.
 *
 * Шапка отдаёт названию кассы не меньше [ContentWidths.topBarTitle]:
 * действия, которым не хватает места, уходят в меню «Ещё». Меряется
 * ширина действий, а место названию — всё, что осталось от окна за
 * вычетом полей шапки. Рельс разделов стоит при любой ширине и в низком
 * окне прокручивается.
 *
 * Кадры остаются в `/tmp/shell-<ширина>x<высота>-<язык>.png` — смотреть
 * глазами, как оболочка стоит на каждом размере.
 */
class ShellAdaptiveTest {

    private class Measured(var actions: Int = 0, var rail: Int = 0)

    /** Шапка кассы с теми же действиями, что в окне, и рабочий раздел под ней. */
    @Composable
    private fun Window(desk: KassaDesk, measured: Measured) {
        val shell by desk.parts.shell.state.collectAsState()
        val bar: @Composable ((() -> Unit)?) -> Unit = {
            AppTopBar(title = shell.kkmName.orEmpty(), subtitle = shell.kkm?.orgTitle, subtitleKept = shell.cashier) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
                    modifier = Modifier.onGloballyPositioned { measured.actions = it.size.width }
                ) { KkmBarActions(shell, desk.look, onSignOut = {}, onRefresh = {}) }
            }
        }
        ShellFrame(Section.entries, Section.Dashboard, {}, topBar = bar) { padding ->
            // Навигация окна — слева от раздела: её ширина — отступ раздела от края.
            val place = Modifier.onGloballyPositioned { measured.rail = it.positionInRoot().x.toInt() }
            Box(modifier = Modifier.fillMaxSize().padding(padding).then(place)) {
                ProvideWindowModels(remember { WindowModels() }) {
                    SectionContent(desk.app, desk.parts, Section.Dashboard)
                }
            }
        }
    }

    private fun check(width: Int, height: Int, language: Language) {
        val desk = KassaScene.desk(KassaScene.kkm(shiftOpen = true), core = DashboardScene.core())
        val measured = Measured()
        inlineMain {
            RenderProbe(width = width, height = height, language = language) { Window(desk, measured) }
                .use { probe ->
                    repeat(SETTLE) { probe.frame() }
                    File("/tmp/shell-${width}x$height-${language.name.lowercase()}.png").writeBytes(probe.frame())
                    if (height <= LOW) {
                        val before = probe.frame()
                        probe.wheel(at = Offset(measured.rail / 2f, height / 2f), ticks = WHEEL)
                        assertTrue(probe.changedFrom(before), "рельс в окне $width×$height не прокручивается")
                    }
                }
        }
        // Шапка стоит правее навигации окна, как велит `NavigationSuiteScaffold`.
        val title = width - measured.rail - measured.actions - BAR_INSETS
        println(
            "окно $width×$height ($language): действия ${measured.actions}, названию $title, рельс ${measured.rail}"
        )
        assertTrue(title >= ContentWidths.topBarTitle.value, "в окне $width×$height названию кассы осталось $title")
        assertTrue(measured.rail >= Sizes.rail.value, "в окне $width×$height рельса нет")
    }

    @Test
    fun `оболочка на мониторах держит название кассы и рельс`() {
        SIZES.forEach { (width, height) -> check(width, height, Language.Ru) }
    }

    /** По-казахски подписи длиннее на треть: место названию меряется и на нём. */
    @Test
    fun `оболочка по-казахски держит название кассы`() {
        SIZES.forEach { (width, height) -> check(width, height, Language.Kk) }
    }

    private companion object {
        val SIZES = listOf(960 to 640, 1180 to 820, 1920 to 1080, 2560 to 1080, 800 to 1280, 1280 to 800)
        const val LOW = 640
        const val SETTLE = 20
        const val WHEEL = 6f

        /**
         * Поля шапки вокруг названия и действий: отступ названия и действий
         * от краёв окна и внутренние поля строки заголовка Material 3.
         */
        val BAR_INSETS = (Spacing.sectionGap * 2).value.toInt() + 24
    }
}
