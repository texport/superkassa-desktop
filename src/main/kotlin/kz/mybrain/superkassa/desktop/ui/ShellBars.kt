package kz.mybrain.superkassa.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetBar
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocuments
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetMessage
import kz.mybrain.superkassa.desktop.ui.components.AppTopBar
import kz.mybrain.superkassa.desktop.ui.components.KkmStatusChips
import kz.mybrain.superkassa.desktop.ui.components.LanguagePicker
import kz.mybrain.superkassa.desktop.ui.components.ThemeSwitch
import kz.mybrain.superkassa.desktop.ui.components.waitedLongEnough
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/**
 * Шапка окна: одна на всё приложение.
 *
 * Она называет то, чем владелец сейчас распоряжается, и она же держит
 * возврат: в кабинете это компания и он сам, в остальных разделах —
 * касса и кассир. Экраны под шапкой своих стрелок не рисуют.
 */
@Composable
internal fun ShellBar(
    session: Session,
    cabinet: CabinetSession,
    documents: CabinetDocuments,
    section: Section
) {
    val scope = rememberCoroutineScope()
    Column {
        if (section == Section.Cabinet && cabinet.open) {
            CabinetBar(session, cabinet, documents)
        } else {
            KkmTopBar(
                session = session,
                onSignOut = { session.signOut() },
                onRefresh = {
                    scope.launch {
                        session.refreshKkms()
                        session.refreshSelected()
                    }
                }
            )
        }
        // Кабинет живёт своим сеансом, а полоска у окна одна: владелец
        // ждёт ответа кабинета так же, как кассир — ответа узла.
        BusyLine(session.busy || cabinet.busy)
    }
}

/**
 * Шапка приложения: какая касса и в каком она состоянии.
 *
 * Заголовок — номер кассы, подзаголовок — организация и смена. Плашки
 * состояния стоят до действий: кассир читает слева направо и должен
 * узнать о блокировке раньше, чем дотянется до кнопки.
 */
@Composable
internal fun KkmTopBar(session: Session, onSignOut: () -> Unit, onRefresh: () -> Unit) {
    val texts = LocalStrings.current
    val kkm = session.selected
    AppTopBar(
        title = kkm?.let { session.displayName(it) } ?: texts.shell.noKkm,
        subtitle = kkm?.let { listOfNotNull(it.orgTitle, session.whoami?.name).joinToString(Glyphs.SEPARATOR) }
    ) {
        KkmStatusChips(session)
        IconButton(onClick = onRefresh) {
            Icon(AppIcons.refresh, contentDescription = texts.common.refresh)
        }
        ThemeSwitch(session)
        LanguagePicker(session)
        TextButton(onClick = onSignOut) { Text(texts.shell.changeCashier) }
    }
}

/**
 * Передаёт помеху кабинета общему показу сообщений.
 *
 * Кабинет живёт своим сеансом, а окно у приложения одно: отказ ИСНА
 * и отказ узла владелец читает в одном и том же месте, а не ищет
 * красную строку по разделам.
 */
@Composable
internal fun CabinetMessageEffect(session: Session, cabinet: CabinetSession) {
    val texts = cabinetTexts(session.language)
    LaunchedEffect(cabinet.problem) {
        val problem = cabinet.problem ?: return@LaunchedEffect
        session.lastMessage = cabinetMessage(problem, texts)
        cabinet.clearProblem()
    }
}

/**
 * Полоска ожидания под шапкой.
 *
 * Один индикатор на всё окно, а не свой у каждой кнопки: обращение к узлу
 * идёт из любого раздела, и кассир должен видеть, что касса занята,
 * не гадая, какая кнопка сейчас работает. Место постоянное — полоска
 * не сдвигает содержимое, когда появляется.
 */
@Composable
internal fun BusyLine(busy: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().height(Sizes.busyLine)) {
        // Пауза перед показом — та же, что у ожидания на месте содержимого:
        // узел на этой же машине отвечает за десятки миллисекунд, и полоска
        // мигала на каждом нажатии.
        if (waitedLongEnough(busy)) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
