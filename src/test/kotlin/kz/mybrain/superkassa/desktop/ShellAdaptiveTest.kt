package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.KkmBarActions
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.SectionContent
import kz.mybrain.superkassa.desktop.ui.SectionRail
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocuments
import kz.mybrain.superkassa.desktop.ui.components.AppTopBar
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.theme.ContentWidths
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
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
    private fun Window(session: Session, measured: Measured) {
        val kkm = session.selected
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = kkm?.let { session.displayName(it) }.orEmpty(),
                subtitle = kkm?.orgTitle,
                subtitleKept = session.whoami?.name
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                    modifier = Modifier.onGloballyPositioned { measured.actions = it.size.width }
                ) { KkmBarActions(session, onSignOut = {}, onRefresh = {}) }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.onGloballyPositioned { measured.rail = it.size.width }) {
                    SectionRail(Section.entries, Section.Dashboard, false, {}, { Text(VERSION) }) {}
                }
                SectionContent(session, CabinetSession(), CabinetDocuments(), Section.Dashboard)
            }
        }
    }

    private fun check(width: Int, height: Int, language: Language) {
        val session = KassaScene.session("shell-$width-$height", shift = KassaScene.openShift())
        session.switchLanguage(language)
        val measured = Measured()
        RenderProbe(width = width, height = height, language = language) { Window(session, measured) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                File("/tmp/shell-${width}x$height-${language.name.lowercase()}.png").writeBytes(probe.frame())
                if (height <= LOW) {
                    val before = probe.frame()
                    probe.wheel(at = Offset(measured.rail / 2f, height / 2f), ticks = WHEEL)
                    assertTrue(probe.changedFrom(before), "рельс в окне $width×$height не прокручивается")
                }
            }
        val title = width - measured.actions - BAR_INSETS
        println("окно $width×$height ($language): действия ${measured.actions}, названию $title, рельс ${measured.rail}")
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
        const val VERSION = "1.0.0"

        /**
         * Поля шапки вокруг названия и действий: отступ названия и действий
         * от краёв окна и внутренние поля строки заголовка Material 3.
         */
        val BAR_INSETS = (Spacing.roomy * 2).value.toInt() + 24
    }
}
