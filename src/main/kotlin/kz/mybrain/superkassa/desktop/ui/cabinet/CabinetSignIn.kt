package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
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
 *
 * Способ входа один — по ЭЦП. Вход по набранным ИИН и БИН стоял тут же
 * и предлагал ввести любые двенадцать цифр: на экране входа это выглядит
 * как вторая, неохраняемая дверь. Он остался средством отладки и виден
 * только при включённом режиме отладки — там же, где журнал обмена.
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
                ) {
                    Text(texts.title, style = MaterialTheme.typography.headlineSmall)
                    InfoTip(texts.signInHint)
                }
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
                if (AppLog.debugMode) DeveloperSignIn(session, cabinet, texts)
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
 * Вход без ЭЦП — средство отладки: кабинет пускает по ИИН и БИН
 * из заголовков.
 *
 * Показывается только при включённом режиме отладки. Кассиру и владельцу
 * этот вход не нужен вовсе, а на экране входа он выглядит как вторая
 * дверь, в которую пускают по любым двенадцати цифрам.
 *
 * Поля помнятся между запусками: при отладке кабинет открывается не один
 * раз, и набирать двенадцать цифр перед каждым разом незачем.
 */
@Composable
private fun DeveloperSignIn(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    var iin by remember { mutableStateOf(session.preferences.cabinetDeveloperIin) }
    var bin by remember { mutableStateOf(session.preferences.cabinetDeveloperBin) }
    val complete = iin.trim().length == IDENTIFIER_LENGTH && bin.trim().length == IDENTIFIER_LENGTH
    HorizontalDivider()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        Text(text = texts.developerSignIn, style = MaterialTheme.typography.titleSmall)
        InfoTip(texts.developerSignInHint)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        OutlinedTextField(
            value = iin,
            onValueChange = { iin = it.filter(Char::isDigit) },
            label = { Text(texts.iin) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = bin,
            onValueChange = { bin = it.filter(Char::isDigit) },
            label = { Text(texts.bin) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
    FieldButton(text = texts.developerSignIn, enabled = complete && !cabinet.busy, modifier = Modifier.fillMaxWidth()) {
        session.preferences.cabinetDeveloperIin = iin
        session.preferences.cabinetDeveloperBin = bin
        scope.launch {
            if (cabinet.signInAsDeveloper(iin, bin)) {
                cabinet.refreshRegisters()
            }
        }
    }
}

/** ИИН и БИН — двенадцать цифр. */
private const val IDENTIFIER_LENGTH = 12
