package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.BusyLine

/**
 * Кабинет ОФД, открытый с экрана входа.
 *
 * Владелец приходит в кабинет до всякой кассы: пока она не заведена,
 * пина кассира не существует, а завести её без кабинета нельзя. Поэтому
 * дверь в кабинет стоит рядом с входом кассира, а не за ней.
 *
 * Экран тот же, что и в разделе после входа, и шапка та же. Возврата
 * своего у двери нет: стрелку рисует шапка и она же решает, куда вести —
 * к карточке кассы, пока открыты её документы, или обратно на вход.
 *
 * Полоска ожидания под шапкой такая же, как у кассы: владелец ждёт ответа
 * кабинета так же, как кассир — ответа узла, и прежде у двери её не было
 * вовсе. Окно просмотра печатной формы стоит над обоими входами и живёт
 * в каркасе окна — второго здесь не заводится.
 */
@Composable
fun CabinetDoor(session: Session, cabinet: CabinetSession, onBack: () -> Unit) {
    val documents = remember { CabinetDocuments() }
    Column(modifier = Modifier.fillMaxSize()) {
        CabinetBar(session, cabinet, documents, onExit = onBack)
        BusyLine(session.busy || cabinet.busy)
        CabinetScreen(session, cabinet, documents)
    }
}
