package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.state.BusyLine
import kz.mybrain.superkassa.presentation.common.message.MessageHost
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.bar.DoorBar
import kz.mybrain.superkassa.presentation.shell.section.SectionDoor
import kz.mybrain.superkassa.presentation.shell.section.closeDoor
import kz.mybrain.superkassa.presentation.shell.section.sectionFrame
import kz.mybrain.superkassa.presentation.users.signin.Door
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
    val office = window.cabinet?.cabinet?.state?.collectAsScreenState()?.value
    val close = { closeDoor(login) }
    Scaffold(
        topBar = {
            Column {
                DoorBar(window, door.door, close)
                BusyLine(door.entering || (door.door == Door.Cabinet && office?.busy == true))
            }
        },
        snackbarHost = { MessageHost(messages) }
    ) { padding ->
        ShellMessages(app, messages)
        Row(modifier = Modifier.fillMaxSize().padding(padding).sectionFrame()) {
            SectionDoor(app, window, door, login, close)
        }
    }
}
