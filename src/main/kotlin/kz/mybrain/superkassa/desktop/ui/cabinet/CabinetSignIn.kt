package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Вход владельца в кабинет по ЭЦП.
 *
 * Одна кнопка и одна строка объяснения: сертификат владелец выбирает
 * в окне NCALayer, пароль вводит там же. Приложение ключа не видит
 * и никуда его не сохраняет — сюда возвращается только готовая подпись.
 *
 * Пока NCALayer ждёт пароль, кнопка занята и подписана ожиданием:
 * подпись занимает столько, сколько владелец ищет свой ключ, и молчащая
 * кнопка выглядела бы зависшей.
 */
@Composable
fun CabinetSignIn(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.roomy),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.normal, Alignment.CenterVertically)
    ) {
        ElevatedCard(modifier = Modifier.widthIn(max = Sizes.loginColumn)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug)
            ) {
                Icon(
                    imageVector = AppIcons.cabinet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Sizes.headerIcon)
                )
                Text(texts.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = texts.signInHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BusyButton(
                    text = if (cabinet.busy) texts.signing else texts.signIn,
                    busy = cabinet.busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            if (cabinet.signIn()) {
                                cabinet.refreshRegisters()
                            }
                        }
                    }
                )
                CabinetIssue(cabinet, texts)
                Text(
                    text = "${texts.address}: ${session.preferences.cabinetUrl}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Почему последнее действие в кабинете не удалось.
 *
 * Каждая помеха названа тем, что владелец должен сделать: запустить
 * NCALayer, повторить подпись, войти заново. Голый код отказа
 * оставлялся бы разбираться поддержке, а не владельцу.
 */
@Composable
fun CabinetIssue(cabinet: CabinetSession, texts: CabinetTexts) {
    val problem = cabinet.problem ?: return
    Text(
        text = when (problem) {
            is CabinetProblem.Refused -> problem.text
            is CabinetProblem.Unreachable -> "${texts.unreachable} · ${problem.reason}"
            CabinetProblem.NoNcaLayer -> texts.noNcaLayer
            is CabinetProblem.SignDeclined ->
                listOf(texts.signDeclined, problem.detail).filter { it.isNotBlank() }.joinToString(" · ")
            CabinetProblem.SessionExpired -> texts.sessionExpired
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}
