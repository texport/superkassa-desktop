package kz.mybrain.superkassa.desktop.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.adoptCabinetNames
import kz.mybrain.superkassa.desktop.app.loadDictionaries
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocuments

/**
 * Каркас окна.
 *
 * Разделы — рельсом слева, как в настольном Material 3; над содержимым —
 * шапка приложения с кассой, её состоянием и действиями. Состояние узла
 * и кассы висит всегда и на всех разделах: кассир должен увидеть разрыв
 * связи до того, как пробьёт чек, а не после.
 */
@Composable
fun Shell(session: Session) {
    // Кабинет живёт рядом с кассовым сеансом, а не внутри него: вход туда
    // свой — по ЭЦП владельца, — и переживает переходы между разделами.
    val cabinet = remember {
        CabinetSession(CabinetClient(session.preferences.cabinetUrl)).apply {
            // Названия касс владелец даёт в кабинете, а нужны они кассиру
            // на входе, когда кабинет закрыт: прочитанное уходит узлу
            // и оттуда его видит любое рабочее место.
            onRegisterNames = { registers -> session.adoptCabinetNames(registers) }
        }
    }
    // Один хост сообщений на окно: снекбар лежит поверх содержимого
    // и не сдвигает разметку под руками кассира.
    val messages = remember { SnackbarHostState() }
    // Куда владелец углубился внутри кабинета — знает окно, а не экран
    // под ним: стрелка возврата в приложении одна и живёт в шапке.
    val documents = remember { CabinetDocuments() }

    LaunchedEffect(Unit) {
        session.refreshKkms()
        session.loadDictionaries()
    }

    // Вошли — сразу подтягиваем состояние выбранной кассы.
    LaunchedEffect(session.selected?.kkmId, session.pin) {
        if (session.signedIn) {
            session.refreshSelected()
        }
    }

    if (session.signedIn) {
        WorkShell(session, cabinet, documents, messages)
    } else {
        DoorShell(session, cabinet, messages)
    }
}

/**
 * Окно до входа: кассир видит только вход.
 *
 * Пустые разделы без выбранной кассы отвечают отказами и ничему не учат,
 * поэтому ни рельса, ни шапки кассы здесь нет.
 */
@Composable
private fun DoorShell(session: Session, cabinet: CabinetSession, messages: SnackbarHostState) {
    Scaffold(
        topBar = { BusyLine(session.busy) },
        snackbarHost = { MessageHost(messages) }
    ) { padding ->
        ShellMessages(session, cabinet, messages)
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            SectionDoor(session, cabinet)
        }
    }
}

/**
 * Рабочее окно: рельс разделов, шапка и содержимое.
 *
 * Шапка идёт во всю ширину окна, а рельс разделов — под ней: иначе шапка
 * начиналась правее рельса и накрывала его край, а окно выглядело
 * собранным из двух несогласованных половин.
 */
@Composable
private fun WorkShell(
    session: Session,
    cabinet: CabinetSession,
    documents: CabinetDocuments,
    messages: SnackbarHostState
) {
    var section by remember { mutableStateOf(Section.Dashboard) }
    // Кассиру видны только его разделы: очередь, кассиров и настройки узел
    // отдаёт администратору, и пустой отказ вместо экрана ничему не учит.
    val sections = Section.entries.filter { session.isAdmin || !it.adminOnly }
    if (section !in sections) {
        section = Section.Dashboard
    }
    Scaffold(
        topBar = { ShellBar(session, cabinet, documents, section) },
        snackbarHost = { MessageHost(messages) }
    ) { padding ->
        ShellMessages(session, cabinet, messages)
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            SectionRail(
                sections = sections,
                current = section,
                collapsed = session.railCollapsed,
                onToggle = { session.toggleRail() }
            ) { picked ->
                // Отказ относится к действию, а не к окну: уходя с экрана
                // своей рукой, кассир оставлял за собой отказ настроек,
                // и тот висел поверх аналитики и журнала до нажатия.
                // Переход, сделанный самим приложением, сообщение
                // не гасит: там оно как раз об итоге действия.
                session.lastMessage = null
                cabinet.clearProblem()
                section = picked
            }
            CompositionLocalProvider(LocalSectionSwitch provides { asked -> section = asked }) {
                SectionContent(session, cabinet, documents, section)
            }
        }
    }
}

/**
 * Показ сообщений окна.
 *
 * Помехи кабинета идут тем же путём, что и отказы кассы: сообщение
 * в приложении одно, и место ему внизу окна.
 */
@Composable
private fun ShellMessages(session: Session, cabinet: CabinetSession, messages: SnackbarHostState) {
    MessageEffect(session.lastMessage, messages) { session.lastMessage = null }
    CabinetMessageEffect(session, cabinet)
}
