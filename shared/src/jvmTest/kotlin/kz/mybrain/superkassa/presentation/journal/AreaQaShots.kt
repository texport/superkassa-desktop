package kz.mybrain.superkassa.presentation.journal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.ProbeNode
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.adaptive.ContentKind
import kz.mybrain.superkassa.designsystem.adaptive.contentWidth
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.idleCabinet
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.cabinet.steps
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSession
import kz.mybrain.superkassa.presentation.common.print.PrintActions
import kz.mybrain.superkassa.presentation.journal.HistoryStage.Mode
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
import kz.mybrain.superkassa.presentation.shell.section.sectionFrame
import kz.mybrain.superkassa.presentation.users.CashierForm
import kz.mybrain.superkassa.presentation.users.UsersActions
import kz.mybrain.superkassa.presentation.users.UsersContent
import kz.mybrain.superkassa.presentation.users.UsersUiState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Кассиры, мастер подключения, очередь и прошлые смены — во всех окнах,
 * на трёх языках, в светлом и тёмном оформлении, на обычной и крупной
 * ступени шрифта, на предельных данных.
 *
 * Кадры — `/tmp/qa-<область>-<экран>-<окно>-<язык>-<ступень>-<оформление>.png`;
 * смотрит их человек. Число проверяет одно: главное действие экрана
 * стоит в окне целиком.
 */
class AreaQaShots {

    private fun shot(
        name: String,
        width: Int,
        height: Int,
        mode: Mode,
        content: @Composable () -> Unit
    ): List<ProbeNode> {
        val desk = KassaScene.desk()
        return RenderProbe(width, height, mode.appearance, Look(textScale = mode.scale), mode.language) {
            HistoryStage.Window(desk, Section.History, HistoryStage.Place()) { content() }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            File("/tmp/qa-$name-${width}x$height-${mode.tag}.png").writeBytes(probe.frame())
            probe.nodes()
        }
    }

    /** Экран [name] в каждом окне и условии; [main] — надпись главного действия на языке кадра. */
    private fun everywhere(name: String, main: ((Language) -> String)?, content: @Composable () -> Unit) {
        HistoryStage.SIZES.forEach { (width, height) ->
            MODES.forEach { mode ->
                val nodes = shot(name, width, height, mode, content)
                val action = main?.invoke(mode.language) ?: return@forEach
                val where = "$name ${mode.tag} $width×$height"
                val button = assertNotNull(nodes.firstOrNull { it.text == action }, "$where: нет «$action»")
                assertTrue(button.whole, "$where: «$action» обрезана — $button")
            }
        }
    }

    /** Место раздела в окне — то же, что ставит каркас. */
    @Composable
    private fun Place(content: @Composable () -> Unit) {
        Box(modifier = Modifier.sectionFrame().fillMaxHeight()) { content() }
    }

    @Test
    fun `кассиры с набранной формой`() {
        val users = HistoryStage.cashiers(CASHIERS)
        val state = UsersUiState(
            users = users,
            answered = true,
            kkmChosen = true,
            me = users.first(),
            form = CashierForm(name = users.last().name, role = UserRole.CASHIER, pin = "1234567890")
        )
        everywhere("users-list", { textsOf(it).common.users.create }) {
            Place { UsersContent(state, object : UsersActions {}) }
        }
    }

    @Test
    fun `мастер подключения — первый шаг`() {
        val contours = listOf("DEV", "TEST", "PROD")
            .map { OfdEnvironmentResponse(it, TrilingualMessageResponse(it, it, it)) }
        everywhere("setup-factory", { textsOf(it).setup.getFactory }) {
            val desk = KassaScene.desk()
            Place {
                SetupContent(
                    SetupParts(
                        state = SetupUiState(contours = contours),
                        actions = object : SetupActions {},
                        registration = RegistrationUiState(),
                        registrationActions = object : RegistrationActions {},
                        cabinet = idleCabinet(desk.app, desk.look).steps(),
                        session = CabinetSession(),
                        onBack = null
                    )
                )
            }
        }
    }

    @Test
    fun `очередь с неудачными в режиме программирования`() {
        val kkm = CoreScene.kkm(state = "PROGRAMMING")
        val queue = QueueUiState(kkm = kkm, tasks = HistoryStage.tasks(TASKS), read = true)
        everywhere("queue-failed", { textsOf(it).common.queue.retryFailed }) {
            Place { QueueContent(queue, object : QueueActions {}) }
        }
    }

    @Test
    fun `прошлые смены и документы смены`() {
        val shifts = HistoryStage.shifts(SHIFTS)
        val opened = ShiftsUiState(
            shifts = shifts,
            page = PageOutcome.page(more = true),
            loading = false,
            opened = shifts[1],
            documents = HistoryStage.documents(DOCUMENTS),
            documentsPage = PageOutcome.page(more = true)
        )
        everywhere("journal-shift-documents", null) {
            Column(modifier = Modifier.contentWidth(ContentKind.Workspace).fillMaxSize().padding(Spacing.fieldGap)) {
                ShiftsScreen(opened, object : ShiftsActions {}, object : PrintActions {})
            }
        }
    }

    private companion object {
        const val SETTLE = 20
        const val CASHIERS = 30
        const val TASKS = 60
        const val SHIFTS = 300
        const val DOCUMENTS = 500
        val MODES = listOf(
            Mode(),
            Mode(Language.Kk, TextScale.Larger, Appearance.Dark),
            Mode(Language.En, TextScale.Larger)
        )
    }
}
