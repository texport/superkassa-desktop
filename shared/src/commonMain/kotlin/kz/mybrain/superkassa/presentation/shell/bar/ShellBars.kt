package kz.mybrain.superkassa.presentation.shell.bar

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.designsystem.section.AppTopBar
import kz.mybrain.superkassa.designsystem.state.BusyLine
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.domain.kkm.model.orgTitle
import kz.mybrain.superkassa.presentation.cabinet.CabinetBar
import kz.mybrain.superkassa.presentation.common.look.LookViewModel
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.ShellUiState
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.users.signin.loginViewModel

/**
 * Шапка окна: одна на всё приложение.
 *
 * Она называет то, чем владелец сейчас распоряжается, и она же держит
 * возврат: в кабинете это компания и он сам, в остальных разделах —
 * касса и кассир. Экраны под шапкой своих стрелок не рисуют.
 */
@Composable
internal fun ShellBar(window: WindowParts, shell: ShellUiState, section: Section, onSignOut: () -> Unit) {
    val door = window.cabinet
    val office = door?.cabinet?.state?.collectAsScreenState()?.value
    Column {
        if (door != null && section == Section.Cabinet && office?.open == true) {
            CabinetBar(door.cabinet, door.look)
        } else {
            KkmTopBar(shell, window.look, onSignOut = onSignOut, onRefresh = window.shell::refresh)
        }
        // Кабинет живёт своей моделью, а полоска у окна одна: владелец
        // ждёт ответа кабинета так же, как кассир — ответа кассы.
        BusyLine(shell.busy || office?.busy == true)
    }
}

/** Шапка рабочего окна: кассир уходит сценарием входа — вход и уход одна область, одна модель. */
@Composable
internal fun WorkBar(app: AppContainer, window: WindowParts, shell: ShellUiState, section: Section) =
    ShellBar(window, shell, section, loginViewModel(app.services)::signOut)

/**
 * Шапка приложения: какая касса и в каком она состоянии.
 *
 * Заголовок — название кассы, подзаголовок — организация и кассир. Плашки
 * состояния стоят до действий: кассир читает слева направо и должен
 * узнать о блокировке раньше, чем дотянется до кнопки. Имя кассира
 * не сокращается: длинное название организации уступает ему место.
 */
@Composable
internal fun KkmTopBar(
    shell: ShellUiState,
    look: LookViewModel,
    onSignOut: () -> Unit,
    onRefresh: () -> Unit
) {
    val texts = LocalStrings.current
    AppTopBar(
        title = shell.kkmName ?: texts.shell.noKkm,
        subtitle = shell.kkm?.orgTitle,
        subtitleKept = shell.cashier
    ) {
        KkmBarActions(shell, look, onSignOut, onRefresh)
    }
}
