package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import kz.mybrain.superkassa.designsystem.state.BusyLine
import kz.mybrain.superkassa.navigation.NavKeys
import kz.mybrain.superkassa.presentation.common.message.MessageHost
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.bar.DoorBar
import kz.mybrain.superkassa.presentation.shell.section.SectionDoor
import kz.mybrain.superkassa.presentation.shell.section.closeDoor
import kz.mybrain.superkassa.presentation.shell.section.sectionFrame
import kz.mybrain.superkassa.presentation.users.signin.Door
import kz.mybrain.superkassa.presentation.users.signin.LoginUiState
import kz.mybrain.superkassa.presentation.users.signin.loginViewModel

/**
 * Окно до входа: кассир видит только вход.
 *
 * Пустые разделы без выбранной кассы отвечают отказами и ничему не учат,
 * поэтому ни рельса, ни шапки кассы здесь нет. Шапка окна при этом одна —
 * в том же слоте `Scaffold` и тех же цветов, что у рабочего окна: она
 * называет вход, а за дверью — заведение кассы, кабинет или настройки,
 * и держит возврат на вход. Двери своих шапок не строят.
 */
@Composable
internal fun DoorShell(app: AppContainer, window: WindowParts, messages: SnackbarHostState) {
    // Набранное кассиром живёт в модели входа окна: список касс и полоса
    // пина читают одно и то же. Поле окна — то же, что у разделов, и сверху:
    // шапки над входом нет.
    val login = loginViewModel(app.services)
    val door by login.state.collectAsScreenState()
    // Шаги за дверью — раздел настроек поверх их списка — идут той же
    // историей, что в рабочем окне: «назад» сперва снимает шаг, потом
    // закрывает дверь.
    val steps = rememberNavBackStack(NavKeys)
    val close: () -> Unit = { if (steps.isEmpty()) closeDoor(login) else steps.removeAt(steps.lastIndex) }
    LaunchedEffect(door.door) { if (door.door != Door.Settings) steps.clear() }
    StepsOf(steps, close) {
        Scaffold(
            topBar = { DoorTop(window, door, close) },
            snackbarHost = { MessageHost(messages) }
        ) { padding ->
            ShellMessages(app, messages)
            val frame = Modifier.fillMaxSize().padding(padding).sectionFrame()
            Row(modifier = frame.backOnEscape(steps.isNotEmpty(), close)) {
                SectionDoor(app, window, door, login, close, steps.lastOrNull())
            }
        }
    }
}

/** Шапка входа и полоска ожидания под ней: ждут и входа кассира, и ответа кабинета. */
@Composable
private fun DoorTop(window: WindowParts, door: LoginUiState, close: () -> Unit) {
    val office = window.cabinet?.cabinet?.state?.collectAsScreenState()?.value
    Column {
        DoorBar(window, door.door, close)
        BusyLine(door.entering || (door.door == Door.Cabinet && office?.busy == true))
    }
}
