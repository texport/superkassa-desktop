package kz.mybrain.superkassa.presentation.journal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.designsystem.adaptive.ContentKind
import kz.mybrain.superkassa.designsystem.adaptive.contentWidth
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.desk
import kz.mybrain.superkassa.idleCabinet
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.cabinet.steps
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSession
import kz.mybrain.superkassa.presentation.common.print.PrintActions
import kz.mybrain.superkassa.presentation.journal.HistoryStage.Mode
import kz.mybrain.superkassa.presentation.journal.documents.JournalUiState
import kz.mybrain.superkassa.presentation.journal.queue.QueueActions
import kz.mybrain.superkassa.presentation.journal.queue.QueueContent
import kz.mybrain.superkassa.presentation.journal.queue.QueueUiState
import kz.mybrain.superkassa.presentation.journal.shifts.ShiftsActions
import kz.mybrain.superkassa.presentation.journal.shifts.ShiftsScreen
import kz.mybrain.superkassa.presentation.journal.shifts.ShiftsUiState
import kz.mybrain.superkassa.presentation.setup.SetupActions
import kz.mybrain.superkassa.presentation.setup.SetupContent
import kz.mybrain.superkassa.presentation.setup.SetupParts
import kz.mybrain.superkassa.presentation.setup.SetupUiState
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationActions
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationUiState
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.users.UsersActions
import kz.mybrain.superkassa.presentation.users.UsersContent
import kz.mybrain.superkassa.presentation.users.UsersUiState
import kz.mybrain.superkassa.strings.api.Language
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Прошлые смены, документы смены, очередь, кассиры и мастер подключения
 * в тех же окнах, что и журнал.
 *
 * Экран рисуется с состоянием, собранным руками, — тем же, что отдаёт
 * модель, — в том же месте окна, что и у кассира.
 *
 * Кадры — `/tmp/adaptive-history-<экран>-<размер>-<язык>-<ступень>-<оформление>.png`;
 * смотрит их человек: растянутая на весь монитор строка числом не ловится.
 */
class HistoryScreensShots {

    private val modes = listOf(Mode(), Mode(Language.Kk, TextScale.Larger))

    private fun each(screen: String, body: @Composable (KassaDesk) -> Unit) {
        SIZES.forEach { (width, height) ->
            modes.forEach { mode ->
                val scene = KassaScene.desk()
                val frame = HistoryStage.shot("$screen-${width}x$height-${mode.tag}", width, height, mode) {
                    HistoryStage.Window(scene, Section.History, HistoryStage.Place()) { body(scene) }
                }
                assertTrue(frame.isNotEmpty(), "пустой кадр: $screen $width×$height")
            }
        }
    }

    /** Место раздела в окне: тот же предел ширины, что ставит каркас. */
    @Composable
    private fun SectionPlace(content: @Composable () -> Unit) {
        Box(modifier = Modifier.contentWidth(ContentKind.Workspace).fillMaxHeight()) { content() }
    }

    /** Место раздела журнала: тот же предел и те же поля, что у экрана истории. */
    @Composable
    private fun HistoryPlace(content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = Modifier.contentWidth(ContentKind.Workspace).fillMaxSize().padding(Spacing.fieldGap),
            content = content
        )
    }

    @Test
    fun `прошлые смены`() {
        val shifts = shifts(HistoryStage.shifts(SHIFTS))
        each("shifts") { HistoryPlace { ShiftsScreen(shifts, NO_SHIFTS, NO_PRINT) } }
    }

    @Test
    fun `документы смены`() {
        val opened = shifts(HistoryStage.shifts(2)).let {
            val documents = HistoryStage.documents(SHIFT_DOCUMENTS)
            it.copy(opened = it.shifts.last(), documents = documents, documentsPage = READ)
        }
        each("shift-documents") { HistoryPlace { ShiftsScreen(opened, NO_SHIFTS, NO_PRINT) } }
    }

    @Test
    fun `очередь`() {
        val queue = QueueUiState(kkm = CoreScene.kkm(), tasks = HistoryStage.tasks(TASKS), read = true)
        each("queue") { SectionPlace { QueueContent(queue, NO_QUEUE) } }
    }

    @Test
    fun `кассиры`() {
        val users = users(HistoryStage.cashiers(CASHIERS))
        each("users") { SectionPlace { UsersContent(users, NO_USERS) } }
    }

    @Test
    fun `мастер подключения`() {
        each("setup") { desk -> SectionPlace { SetupContent(setup(desk)) } }
    }

    @Test
    fun `пусто и один элемент`() {
        val one = Mode()
        listOf(0, 1).forEach { count ->
            val desk = KassaScene.desk(KassaScene.kkm(shiftOpen = true))
            val journal = JournalUiState(documents = HistoryStage.documents(count), page = READ, loading = false)
            val screens = mapOf<String, @Composable () -> Unit>(
                "history" to { SectionPlace { HistoryContent(HistoryParts(journal)) } },
                "queue" to {
                    val queue = QueueUiState(CoreScene.kkm(), HistoryStage.tasks(count), read = true)
                    SectionPlace { QueueContent(queue, NO_QUEUE) }
                },
                "users" to { SectionPlace { UsersContent(users(HistoryStage.cashiers(count)), NO_USERS) } },
                "shifts" to {
                    HistoryPlace { ShiftsScreen(shifts(HistoryStage.shifts(count)), NO_SHIFTS, NO_PRINT) }
                }
            )
            screens.forEach { (name, screen) ->
                HistoryStage.shot("few-$count-$name", WIDTH, HEIGHT, one) {
                    HistoryStage.Window(desk, Section.History, HistoryStage.Place()) { screen() }
                }
            }
        }
    }

    private fun shifts(list: List<ShiftResponse>) =
        ShiftsUiState(shifts = list, page = READ, loading = false)

    private fun users(list: List<UserResponse>) =
        UsersUiState(users = list, answered = true, kkmChosen = true, me = list.firstOrNull())

    private fun setup(desk: KassaDesk) = SetupParts(
        state = SetupUiState(contours = CONTOURS),
        actions = object : SetupActions {},
        registration = RegistrationUiState(),
        registrationActions = object : RegistrationActions {},
        cabinet = idleCabinet(desk.app, desk.look).steps(),
        session = CabinetSession(),
        onBack = null
    )

    private companion object {
        /** Наименьшее окно, широкий монитор и планшет стоймя: крайние случаи раскладки. */
        val SIZES = listOf(960 to 640, 1920 to 1080, 800 to 1280)
        const val SHIFTS = 500
        const val SHIFT_DOCUMENTS = 200
        const val TASKS = 100
        const val CASHIERS = 20
        const val WIDTH = 1180
        const val HEIGHT = 820
        val READ = PageOutcome.page(more = false)
        val NO_SHIFTS = object : ShiftsActions {}
        val NO_PRINT = object : PrintActions {}
        val NO_QUEUE = object : QueueActions {}
        val NO_USERS = object : UsersActions {}
        val CONTOURS = listOf("DEV", "TEST", "PROD")
            .map { OfdEnvironmentResponse(it, TrilingualMessageResponse(it, it, it)) }
    }
}
