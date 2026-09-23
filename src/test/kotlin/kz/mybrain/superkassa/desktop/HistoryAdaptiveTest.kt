package kz.mybrain.superkassa.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import kz.mybrain.superkassa.desktop.HistoryStage.Mode
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.SectionContent
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocuments
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.ContentWidths
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.TextScale
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Журнал документов в окнах от наименьшего до широкого монитора.
 *
 * В окне 960×640 журнал не показывал ни одной строки: ряд плашек отбора
 * не переносился, плашка ставила слово столбиком, и отбор съедал всю
 * высоту. Меряется число видимых строк — по затенённым полосам,
 * которыми строки идут через одну.
 *
 * Кадры — `/tmp/adaptive-history-journal-<размер>-<язык>-<ступень>-<оформление>.png`.
 */
class HistoryAdaptiveTest {

    private fun journal(width: Int, height: Int, mode: Mode, documents: List<Document> = MANY): Int {
        val session = KassaScene.session("history-$width-$height", shift = KassaScene.openShift(), journal = documents)
        session.switchLanguage(mode.language)
        val place = HistoryStage.Place()
        var tint = 0
        val frame = HistoryStage.shot("journal-${width}x$height-${mode.tag}", width, height, mode) {
            tint = striped()
            HistoryStage.Window(session, Section.History, place) {
                SectionContent(session, CabinetSession(), CabinetDocuments(), Section.History)
            }
        }
        val workspace = minOf(place.width.toFloat(), ContentWidths.workspace.value)
        val left = place.left + (place.width - workspace) / 2 + Spacing.screen.value + EDGE
        val rows = HistoryStage.stripes(frame, left.toInt(), tint) * 2
        println("журнал $width×$height ${mode.tag}: видно строк ≈ $rows")
        return rows
    }

    @Composable
    private fun striped(): Int = MaterialTheme.colorScheme.surfaceContainerLow.toArgb()

    @Test
    fun `журнал в наименьшем окне показывает строки`() {
        listOf(Mode(), Mode(Language.Kk, TextScale.Larger)).forEach { mode ->
            val rows = journal(SMALLEST.first, SMALLEST.second, mode, HistoryStage.documents(DOCUMENTS))
            assertTrue(BEFORE || rows >= LEAST_ROWS, "в окне 960×640 (${mode.tag}) видно строк $rows")
        }
    }

    @Test
    fun `журнал собирается во всех окнах, на двух языках и двух ступенях`() {
        HistoryStage.SIZES.forEach { (width, height) ->
            listOf(Mode(), Mode(Language.Kk, TextScale.Larger)).forEach { mode ->
                val rows = journal(width, height, mode)
                assertTrue(BEFORE || rows > 0, "журнал $width×$height (${mode.tag}) без строк")
            }
        }
        listOf(SMALLEST, DEFAULT).forEach { (width, height) ->
            journal(width, height, Mode(appearance = Appearance.Dark))
        }
    }

    private companion object {
        const val DOCUMENTS = 10_000

        /**
         * Набор для обхода окон: строк больше, чем войдёт в любое окно.
         * Десять тысяч — только в наименьшем окне: на каждый из шестнадцати
         * кадров их памяти проверок не хватает.
         */
        val MANY = HistoryStage.documents(DOCUMENTS / 20)
        val SMALLEST = 960 to 640
        val DEFAULT = 1180 to 820
        const val LEAST_ROWS = 4
        const val EDGE = 3
        val BEFORE = System.getenv("BEFORE") != null
    }
}
