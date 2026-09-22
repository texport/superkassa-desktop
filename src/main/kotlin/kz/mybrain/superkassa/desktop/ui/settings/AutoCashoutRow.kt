package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.updateAutoCashout
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Автоизъятие наличных при закрытии смены.
 *
 * На этом месте стоял переключатель автозакрытия смены. Его настройку
 * не читал никто: смену закрывает кассир, а сверх суток касса перестаёт
 * оформлять операции — так требуют требования к ККМ. Зато автоизъятие
 * узел выполняет по-настоящему, и задать его было нечем.
 *
 * Отдельной кнопки у переключателя нет: он и есть действие, и уходит
 * на узел сразу.
 */
@Composable
internal fun AutoCashoutRow(session: Session, kkm: Kkm) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = kkm.autoCashout,
            enabled = !session.busy && kkm.isProgramming,
            onCheckedChange = { wanted ->
                scope.launch {
                    session.guard(texts.settings.autoCashout) {
                        session.client.updateAutoCashout(kkm.kkmId, wanted, session.pin)
                    } ?: return@launch
                    session.refreshKkms()
                    session.report(texts.settings.settingsSaved)
                }
            }
        )
        Text(texts.settings.autoCashout, style = MaterialTheme.typography.bodyMedium)
        InfoTip(texts.settings.autoCashoutHint)
    }
}
