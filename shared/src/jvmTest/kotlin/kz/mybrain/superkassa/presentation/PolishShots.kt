package kz.mybrain.superkassa.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.ProbeNode
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.section.CollapsibleCard
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.analytics.model.SalesOverview
import kz.mybrain.superkassa.presentation.analytics.sales.SalesOverviewTiles
import kz.mybrain.superkassa.presentation.common.document.JournalTable
import kz.mybrain.superkassa.presentation.journal.HistoryStage
import kz.mybrain.superkassa.presentation.journal.documents.journalEntriesOf
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Мелочи, найденные проверками экранов: кадр каждой — `/tmp/polish-<что>.png`,
 * и там, где это поведение, — замер.
 */
class PolishShots {

    private fun shoot(
        name: String,
        width: Int,
        height: Int,
        language: Language = Language.Ru,
        scale: TextScale = TextScale.Normal,
        content: @Composable () -> Unit
    ): List<ProbeNode> = RenderProbe(width, height, look = Look(textScale = scale), language = language) {
        Surface(Modifier.fillMaxSize()) { content() }
    }.use { probe ->
        repeat(SETTLE) { probe.frame() }
        File("/tmp/polish-$name.png").writeBytes(probe.frame())
        probe.nodes()
    }

    @Test
    fun `в журнале окна 1180 кнопки строки видны целиком`() {
        val journal = textsOf(Language.Ru).journal.history
        val nodes = shoot("journal-1180x820", JOURNAL_W, JOURNAL_H) {
            // Слева — место рельса окна: таблице остаётся то же, что в окне кассы.
            Column(Modifier.fillMaxSize().padding(start = RAIL_ROOM.dp)) {
                val documents = HistoryStage.documents(ROWS)
                val entries = journalEntriesOf(textsOf(Language.Ru).common, Language.Ru, emptyMap(), documents)
                JournalTable(journal, entries, onPreview = {}, onPrint = {})
            }
        }
        val preview = textsOf(Language.Ru).common.preview.title
        val buttons = nodes.filter { it.label == preview && it.visible.height > 0f }
        assertTrue(buttons.isNotEmpty(), "у строк нет кнопки показа")
        buttons.forEach { assertTrue(it.whole && it.at.x + it.width <= JOURNAL_W, "кнопка за краем: $it") }
    }

    @Test
    fun `без прошлого срока под каждым числом стоит прочерк, и ряд не ломается`() {
        val texts = textsOf(Language.Ru).analytics.sales
        val nodes = shoot("sales-overview-no-previous", OVERVIEW_W, OVERVIEW_H) {
            Column(Modifier.fillMaxWidth().padding(Spacing.fieldGap)) { SalesOverviewTiles(NO_PREVIOUS, texts) }
        }
        val lines = nodes.count { it.text.contains(texts.versusPrevious) }
        assertEquals(OVERVIEW_NUMBERS, lines, "строка сравнения пропала")
    }

    @Test
    fun `заголовок карточки кассы по-казахски на крупном шрифте переносится, а не обрывается`() {
        val texts = textsOf(Language.Kk).cabinet
        val nodes = shoot("cabinet-technical-kk-larger", CARD_W, CARD_H, Language.Kk, TextScale.Larger) {
            Column(Modifier.width(CARD_PANE.dp).padding(Spacing.fieldGap)) {
                CollapsibleCard(
                    title = texts.register.technicalState,
                    expanded = false,
                    onToggle = {},
                    info = texts.hints.technicalState,
                    trailing = { Chip(WORKING_KK, StatusColors.delivered) }
                ) {}
            }
        }
        val title = nodes.single { it.text == texts.register.technicalState }
        val chip = nodes.single { it.text == WORKING_KK }
        assertTrue(title.at.x + title.width <= chip.at.x, "заголовок наехал на плашку: $title, $chip")
        // Две строки заголовка выше плашки в полтора раза и больше; одна — вровень с ней.
        assertTrue(title.height > chip.height * TWO_LINES, "заголовок не перенёсся, а оборван в одну строку: $title")
    }

    private companion object {
        const val CARD_W = 480
        const val CARD_H = 240
        const val CARD_PANE = 440
        const val WORKING_KK = "Касса жұмыста"
        const val TWO_LINES = 1.5f

        const val SETTLE = 20
        const val VERSION = "1.0.6"
        const val RAIL_W = 1280
        const val RAIL_H = 736
        const val JOURNAL_W = 1180
        const val JOURNAL_H = 820
        const val RAIL_ROOM = 135
        const val ROWS = 20
        const val OVERVIEW_W = 1180
        const val OVERVIEW_H = 480

        /** Главных чисел в итогах срока: у каждого своя строка сравнения. */
        const val OVERVIEW_NUMBERS = 6

        val NO_PREVIOUS = SalesOverview(
            revenue = 98_797_031_200L,
            receiptCount = 12_408,
            average = 7_962_500L,
            tax = 10_585_400_000L,
            cashless = 64,
            net = 97_100_000_000L,
            revenueChange = null,
            receiptsChange = null,
            averageChange = null,
            taxChange = null,
            cashlessChange = null,
            netChange = null
        )
    }
}
