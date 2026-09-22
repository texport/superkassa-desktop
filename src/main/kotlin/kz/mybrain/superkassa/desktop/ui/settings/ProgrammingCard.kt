package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.server.enterProgramming
import kz.mybrain.superkassa.desktop.server.exitProgramming
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Режим программирования: в нём ли касса и чем это изменить.
 *
 * Узел принимает настройки кассы и снятие её с учёта только в этом
 * режиме. Прежде войти в него предлагала карточка, поля которой без него
 * заперты, а выйти — кнопка в диагностике, двумя разделами ниже: кассир
 * входил и не находил, чем выйти. Состояние и оба действия стоят здесь,
 * под самой кассой, и кнопка одна — названа по тому, что произойдёт.
 *
 * Плашка появляется только во включённом режиме: обычное состояние
 * называть незачем, а отличающееся кассир обязан видеть с одного взгляда.
 */
@Composable
internal fun ProgrammingCard(session: Session) {
    val texts = LocalStrings.current.settings
    val scope = rememberCoroutineScope()
    val inside = session.selected?.isProgramming ?: return
    SectionCard(
        title = texts.programmingMode,
        info = texts.programmingRequired,
        trailing = { if (inside) Chip(texts.programmingOn, StatusColors.pending) }
    ) {
        FilledTonalButton(
            enabled = !session.busy && session.nodeAvailable,
            onClick = { scope.launch { switchProgramming(session, enter = !inside) } }
        ) { Text(if (inside) texts.exitProgramming else texts.enterProgramming) }
    }
}

/** Переводит кассу в режим программирования или выводит из него. */
private suspend fun switchProgramming(session: Session, enter: Boolean) {
    val texts = stringsOf(session.language)
    val kkm = session.selected ?: return
    session.guard(texts.settings.programmingMode) {
        if (enter) {
            session.client.enterProgramming(kkm.kkmId, session.pin)
        } else {
            session.client.exitProgramming(kkm.kkmId, session.pin)
        }
    } ?: return
    // Сообщение объявляется последним: перечитывание списка касс снимает
    // предыдущее, и объяви мы итог раньше — кассир остался бы без ответа.
    session.refreshKkms()
    session.report(
        if (enter) texts.settings.enteredProgramming else texts.settings.exitedProgramming
    )
}

/** Состояние кассы, в котором узел разрешает менять её настройки. */
internal const val PROGRAMMING = "PROGRAMMING"
