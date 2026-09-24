package kz.mybrain.superkassa.presentation.journal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.presentation.common.adaptive.ContentKind
import kz.mybrain.superkassa.presentation.common.adaptive.contentWidth
import kz.mybrain.superkassa.presentation.journal.HistoryStage.Mode
import kz.mybrain.superkassa.presentation.journal.documents.JournalUiState
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.theme.TextScale
import kz.mybrain.superkassa.presentation.theme.color.Appearance
import kz.mybrain.superkassa.presentation.theme.size.ContentWidths
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.Language
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

    private fun journal(width: Int, height: Int, mode: Mode, documents: List<FiscalDocumentResponse> = MANY): Int {
        val desk = KassaScene.desk(KassaScene.kkm(shiftOpen = true))
        // Журнал прочитан: строки те, что отдала касса, срок прочитан целиком.
        val state = JournalUiState(documents = documents, page = PageOutcome.page(more = false), loading = false)
        val place = HistoryStage.Place()
        var tint = 0
        val frame = HistoryStage.shot("journal-${width}x$height-${mode.tag}", width, height, mode) {
            tint = striped()
            HistoryStage.Window(desk, Section.History, place) {
                // Место раздела в окне то же, что ставит каркас: предел ширины рабочего экрана.
                Box(modifier = Modifier.contentWidth(ContentKind.Workspace).fillMaxHeight()) {
                    HistoryContent(HistoryParts(state))
                }
            }
        }
        val workspace = minOf(place.width.toFloat(), ContentWidths.workspace.value)
        val left = place.left + (place.width - workspace) / 2 + Spacing.fieldGap.value + EDGE
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
