package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.enterProgramming
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Вход в режим программирования — одной строкой под настройками.
 *
 * Раньше каждая настраиваемая карточка писала под собой предложение
 * «Настройки кассы меняются в режиме программирования» и ставила рядом
 * кнопку: два экрана подряд объясняли одно и то же. Осталось действие,
 * а объяснение ушло под значок — там, где ему и место.
 */
@Composable
internal fun ProgrammingGate(session: Session) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    val kkm = session.selected ?: return
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(onClick = {
            scope.launch {
                session.guard(texts.settings.enterProgramming) {
                    session.client.enterProgramming(kkm.kkmId, session.pin)
                } ?: return@launch
                session.refreshKkms()
                session.report(texts.settings.enteredProgramming)
            }
        }) { Text(texts.settings.enterProgramming) }
        InfoTip(texts.settings.programmingRequired)
    }
}
