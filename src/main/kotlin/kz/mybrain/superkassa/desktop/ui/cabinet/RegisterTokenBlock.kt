package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.issueToken
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Технический токен кассы.
 *
 * Выдаётся по требованию владельца и показывается один раз: это ключ,
 * которым касса подписывает запросы, и место ему в настройках кассы,
 * а не в журнале кабинета.
 *
 * Кнопка гаснет там, где кабинет токен не выдаст: по черновику и по кассе
 * с поданным заявлением. Прежде она нажималась всегда, и владелец получал
 * отказ сервера — по-английски и кодом.
 *
 * Выданное значение обёрнуто в область выделения: его переносят в другое
 * приложение, а переписывать десять цифр с экрана руками — верный способ
 * ошибиться в одной.
 */
@Composable
fun RegisterTokenBlock(cabinet: CabinetSession, texts: CabinetTexts, register: CabinetRegister) {
    val scope = rememberCoroutineScope()
    var issued by remember(register.id) { mutableStateOf<Long?>(null) }

    val allowed = tokenAllowed(register)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        Text(text = texts.token, style = MaterialTheme.typography.titleSmall)
        InfoTip(texts.tokenHint)
    }
    // Почему кнопка погасла — строкой: это состояние кассы, а не объяснение
    // раздела, и владелец должен видеть его не открывая подсказку.
    if (!allowed) {
        Text(
            text = texts.tokenOnlyRegistered,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    issued?.let { value ->
        SelectionContainer {
            DetailLine(texts.tokenIssued, value.toString())
        }
    }
    BusyButton(
        text = texts.issueToken,
        busy = cabinet.busy,
        enabled = allowed,
        onClick = {
            scope.launch {
                val token = cabinet.token ?: return@launch
                issued = cabinet.guard { cabinet.client.issueToken(token, register.id) }?.token
            }
        }
    )
}
