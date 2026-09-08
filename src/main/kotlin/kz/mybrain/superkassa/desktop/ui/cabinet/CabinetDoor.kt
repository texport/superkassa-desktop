package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session

/**
 * Кабинет ОФД, открытый с экрана входа.
 *
 * Владелец приходит в кабинет до всякой кассы: пока она не заведена,
 * пина кассира не существует, а завести её без кабинета нельзя. Поэтому
 * дверь в кабинет стоит рядом с входом кассира, а не за ним.
 *
 * Экран тот же, что и в разделе после входа, и шапка та же: добавлен
 * только возврат. Два одинаковых кабинета разошлись бы на первой же
 * правке — прежде у двери была своя строка с названием, и языка с выходом
 * в ней не было вовсе.
 */
@Composable
fun CabinetDoor(session: Session, cabinet: CabinetSession, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        CabinetBar(session, cabinet, onBack)
        CabinetScreen(session, cabinet)
    }
}
