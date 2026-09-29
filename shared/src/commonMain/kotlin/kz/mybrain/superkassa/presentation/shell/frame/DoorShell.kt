package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import kz.mybrain.superkassa.designsystem.state.BusyLine
import kz.mybrain.superkassa.navigation.NavKeys
import kz.mybrain.superkassa.navigation.section.KkmsKey
import kz.mybrain.superkassa.navigation.step.StepKey
import kz.mybrain.superkassa.presentation.common.message.MessageHost
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.bar.DoorBar
import kz.mybrain.superkassa.presentation.shell.section.DoorSection
import kz.mybrain.superkassa.presentation.shell.section.currentDoor
import kz.mybrain.superkassa.presentation.shell.section.doorEntries
import kz.mybrain.superkassa.presentation.shell.section.doorSectionsFor
import kz.mybrain.superkassa.presentation.shell.section.openDoor
import kz.mybrain.superkassa.presentation.shell.section.stepBack
import kz.mybrain.superkassa.presentation.users.signin.LoginViewModel
import kz.mybrain.superkassa.presentation.users.signin.loginViewModel

/**
 * Окно до входа: кассы, новая касса, кабинет БФД и настройки.
 *
 * С него начинается всё, и устроено оно как рабочее окно: та же рамка
 * [ShellFrame] на `NavigationSuiteScaffold` — нижняя полоса на телефоне,
 * рельс шире, — одна шапка и одна история Navigation 3. Первый раздел —
 * кассы и вход по пину; «назад» из любого раздела ведёт к ним, а шаг
 * внутри раздела — раздел настроек или карточка точки поверх списка —
 * снимается стрелкой в шапке, жестом и Escape. Прежде это был экран входа
 * с кнопками-дверями под списком касс, и за каждой открывалась своя
 * страница со своей стрелкой.
 *
 * Разделов кассы здесь нет: без выбранной кассы они отвечали бы отказами.
 */
@Composable
internal fun DoorShell(app: AppContainer, window: WindowParts, messages: SnackbarHostState) {
    // Набранное кассиром живёт в модели входа окна: список касс и полоса
    // пина читают одно и то же.
    val login = loginViewModel(app.services)
    val history = rememberNavBackStack(NavKeys, KkmsKey)
    val door = history.currentDoor()
    val back: () -> Unit = { history.stepBack() }
    val toKkms = { login.reload().also { history.openDoor(DoorSection.Kkms) } }
    StepsOf(history, back) {
        ShellFrame(
            sections = doorSectionsFor(app.areas),
            current = door,
            onPick = history::openDoor,
            topBar = { onMenu -> DoorTop(window, login, door, onMenu, back.takeIf { history.last() is StepKey }) },
            snackbarHost = { MessageHost(messages) }
        ) { padding ->
            ShellMessages(app, messages)
            WindowDisplay(history, back, Modifier.padding(padding)) { step ->
                doorEntries(app, window, login, toKkms, step)
            }
        }
    }
}

/** Шапка окна и полоска ожидания под ней: ждут и входа кассира, и ответа кабинета. */
@Composable
private fun DoorTop(
    window: WindowParts,
    login: LoginViewModel,
    door: DoorSection,
    onMenu: (() -> Unit)?,
    onBack: (() -> Unit)?
) {
    val entering = login.state.collectAsScreenState().value.entering
    val office = window.cabinet?.cabinet?.state?.collectAsScreenState()?.value
    Column {
        DoorBar(window, door, onMenu, onBack, login::reload.takeIf { door == DoorSection.Kkms })
        BusyLine(entering || (door == DoorSection.Cabinet && office?.busy == true))
    }
}
