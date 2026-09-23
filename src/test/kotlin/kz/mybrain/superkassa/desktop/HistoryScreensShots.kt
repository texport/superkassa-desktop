package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.HistoryStage.Mode
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.SectionContent
import kz.mybrain.superkassa.desktop.ui.adaptive.ContentKind
import kz.mybrain.superkassa.desktop.ui.adaptive.contentWidth
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocuments
import kz.mybrain.superkassa.desktop.ui.history.PageOutcome
import kz.mybrain.superkassa.desktop.ui.history.PastShiftsView
import kz.mybrain.superkassa.desktop.ui.history.ShiftDocuments
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.TextScale
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Прошлые смены, документы смены, очередь, кассиры и мастер подключения
 * в тех же окнах, что и журнал.
 *
 * Кадры — `/tmp/adaptive-history-<экран>-<размер>-<язык>-<ступень>-<оформление>.png`;
 * смотрит их человек: растянутая на весь монитор строка числом не ловится.
 */
class HistoryScreensShots {

    private val modes = listOf(Mode(), Mode(Language.Kk, TextScale.Larger))

    private fun each(screen: String, session: (String) -> Session, body: @Composable (Session) -> Unit) {
        SIZES.forEach { (width, height) ->
            modes.forEach { mode ->
                val scene = session("$screen-$width-$height").also { it.switchLanguage(mode.language) }
                val frame = HistoryStage.shot("$screen-${width}x$height-${mode.tag}", width, height, mode) {
                    HistoryStage.Window(scene, Section.History, HistoryStage.Place()) { body(scene) }
                }
                assertTrue(frame.isNotEmpty(), "пустой кадр: $screen $width×$height")
            }
        }
    }

    @Composable
    private fun Screen(session: Session, section: Section) =
        SectionContent(session, CabinetSession(), CabinetDocuments(), section)

    /** Место раздела журнала: тот же предел и те же поля, что у экрана истории. */
    @Composable
    private fun HistoryPlace(content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = Modifier.contentWidth(ContentKind.Workspace).fillMaxSize().padding(Spacing.screen),
            content = content
        )
    }

    @Test
    fun `прошлые смены`() {
        val shifts = HistoryStage.shifts(SHIFTS)
        each("shifts", { KassaScene.session(it, pastShifts = shifts) }) { session ->
            HistoryPlace { PastShiftsView(session) }
        }
    }

    @Test
    fun `документы смены`() {
        val documents = HistoryStage.documents(SHIFT_DOCUMENTS)
        each("shift-documents", { KassaScene.session(it) }) { session ->
            val journal = journalTexts(session.language).shifts
            HistoryPlace {
                ShiftDocuments(
                    session, journal, HistoryStage.shifts(2).last(), documents, loading = false,
                    page = PageOutcome.page(more = false), onBack = {}, onRetry = {}, onPreview = {}
                )
            }
        }
    }

    @Test
    fun `очередь`() {
        val tasks = HistoryStage.tasks(TASKS)
        each("queue", { KassaScene.session(it).also { s -> s.board.adoptQueue(tasks) } }) {
            Screen(it, Section.Queue)
        }
    }

    @Test
    fun `кассиры`() {
        each("users", { KassaScene.session(it, cashiers = HistoryStage.cashiers(CASHIERS)) }) {
            Screen(it, Section.Users)
        }
    }

    @Test
    fun `мастер подключения`() {
        each("setup", { KassaScene.session(it) }) { Screen(it, Section.Register) }
    }

    @Test
    fun `пусто и один элемент`() {
        val one = Mode()
        listOf(0, 1).forEach { count ->
            val session = KassaScene.session(
                "few-$count",
                shift = KassaScene.openShift(),
                journal = HistoryStage.documents(count),
                pastShifts = HistoryStage.shifts(count),
                cashiers = HistoryStage.cashiers(count)
            )
            session.board.adoptQueue(HistoryStage.tasks(count))
            listOf(Section.History, Section.Queue, Section.Users).forEach { section ->
                HistoryStage.shot("few-$count-${section.name.lowercase()}", WIDTH, HEIGHT, one) {
                    HistoryStage.Window(session, section, HistoryStage.Place()) { Screen(session, section) }
                }
            }
            HistoryStage.shot("few-$count-shifts", WIDTH, HEIGHT, one) {
                HistoryStage.Window(session, Section.History, HistoryStage.Place()) {
                    HistoryPlace { PastShiftsView(session) }
                }
            }
        }
    }

    private companion object {
        /** Наименьшее окно, широкий монитор и планшет стоймя: крайние случаи раскладки. */
        val SIZES = listOf(960 to 640, 1920 to 1080, 800 to 1280)
        const val SHIFTS = 500
        const val SHIFT_DOCUMENTS = 200
        const val TASKS = 100
        const val CASHIERS = 20
        const val WIDTH = 1180
        const val HEIGHT = 820
    }
}
